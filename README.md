# CloudTrader

A deliberately simple stock portfolio simulator made of two Spring Boot services and one MySQL database.

- **market-service** (port 8081) makes a simulated price for 5 stocks every second and stores it in MySQL.
  Every 10 seconds it deletes old rows so the table stays at about 50,000 rows.
- **portfolio-service** (port 8082) places buy and sell trades at the live price (it asks market-service over HTTP)
  and calculates holdings and profit/loss.

See [benchmark/RESULTS.md](benchmark/RESULTS.md) for measured numbers.

## What you need

- Java 21 and Maven
- MySQL 8
- Docker Desktop (for the images and for Kubernetes)
- Python 3 (only for the benchmark)

## 1. Run locally

### Start MySQL

The services expect a database `cloudtrader` and a user `trader` with password `trader123`.
These are local development values only.

**Option A: MySQL in Docker** (this is what the benchmark runs used)

```bash
docker run -d --name cloudtrader-mysql -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=cloudtrader \
  -e MYSQL_USER=trader \
  -e MYSQL_PASSWORD=trader123 \
  mysql:8.4

# The container listens on port 3307, so tell the services where to find it
export DB_URL=jdbc:mysql://localhost:3307/cloudtrader
```

Port 3307 is used so it does not clash with a MySQL already installed on port 3306.

**Option B: a MySQL already installed on your laptop** (port 3306, no `DB_URL` needed)

```sql
CREATE DATABASE cloudtrader;
CREATE USER 'trader'@'localhost' IDENTIFIED BY 'trader123';
GRANT ALL PRIVILEGES ON cloudtrader.* TO 'trader'@'localhost';
```

### Build and test

```bash
cd market-service    && mvn -B package && cd ..
cd portfolio-service && mvn -B package && cd ..
```

Each build runs the unit tests (3 in each service). They are plain Java and need no database.

### Start the services

Use two terminals. If you chose Option A, run the `export DB_URL=...` line in both.

```bash
java -jar market-service/target/market-service.jar       # terminal 1
java -jar portfolio-service/target/portfolio-service.jar # terminal 2
```

The tables are created automatically on startup.

### Try it

```bash
curl localhost:8081/prices
curl localhost:8081/prices/AAPL
curl "localhost:8081/prices/AAPL/history?limit=5"
curl localhost:8081/prices/stats

curl -X POST localhost:8082/trades -H "Content-Type: application/json" \
     -d '{"symbol":"AAPL","side":"BUY","quantity":10}'
curl -X POST localhost:8082/trades -H "Content-Type: application/json" \
     -d '{"symbol":"AAPL","side":"SELL","quantity":4}'
# Selling more than you own is refused with HTTP 400
curl -X POST localhost:8082/trades -H "Content-Type: application/json" \
     -d '{"symbol":"AAPL","side":"SELL","quantity":100}'

curl localhost:8082/trades
curl localhost:8082/portfolio
curl "localhost:8082/portfolio?method=java"
```

## 2. Run the benchmark

The commands below use the Docker MySQL from Option A. With Option B, replace
`docker exec -i cloudtrader-mysql mysql` with plain `mysql`.

Both seed scripts only add rows. Empty the tables first if you want clean numbers:

```bash
docker exec -i cloudtrader-mysql mysql -utrader -ptrader123 cloudtrader \
  -e "TRUNCATE price_history; TRUNCATE trades;"
```

### Experiment A: does retention keep `/prices/stats` fast?

```bash
# 1. Stop market-service, then put 1,000,000 rows in the price table
docker exec -i cloudtrader-mysql mysql -utrader -ptrader123 cloudtrader < benchmark/seed_prices.sql

# 2. Start market-service with retention switched off, and measure
RETENTION_ENABLED=false java -jar market-service/target/market-service.jar
python3 benchmark/benchmark.py http://localhost:8081/prices/stats

# 3. Stop it, start it again normally (retention on)
java -jar market-service/target/market-service.jar

# 4. Wait until this shows about 50,000, then measure again
docker exec -i cloudtrader-mysql mysql -utrader -ptrader123 cloudtrader \
  -e "SELECT COUNT(*) FROM price_history"
python3 benchmark/benchmark.py http://localhost:8081/prices/stats
```

### Experiment B: Java loop vs SQL `GROUP BY`

```bash
docker exec -i cloudtrader-mysql mysql -utrader -ptrader123 cloudtrader < benchmark/seed_trades.sql

python3 benchmark/benchmark.py "http://localhost:8082/portfolio?method=java"
python3 benchmark/benchmark.py "http://localhost:8082/portfolio?method=sql"
```

`benchmark.py` makes 20 warm-up calls, then 200 measured calls, and prints the median and p95 in milliseconds.
For `/portfolio` it also prints the server's own `calculationMs`.

Write your numbers into [benchmark/RESULTS.md](benchmark/RESULTS.md). Only quote numbers that are in that file.

## 3. Deploy to Docker Desktop Kubernetes

1. In Docker Desktop, open **Settings > Kubernetes**, tick **Enable Kubernetes**, and wait until it says it is running.
2. Check that kubectl can see it:

   ```bash
   kubectl config use-context docker-desktop
   kubectl get nodes
   ```

3. Build the jars (section 1), then the images. Docker Desktop's Kubernetes uses the images built on your machine.

   ```bash
   docker build -t market-service:1.0 market-service
   docker build -t portfolio-service:1.0 portfolio-service
   ```

4. Deploy the database first, then the services:

   ```bash
   kubectl apply -f k8s/mysql.yaml
   kubectl rollout status statefulset/mysql

   kubectl apply -f k8s/market-service.yaml -f k8s/portfolio-service.yaml
   kubectl rollout status deployment/market-service
   kubectl rollout status deployment/portfolio-service
   ```

5. portfolio-service is exposed on NodePort 30082:

   ```bash
   curl localhost:30082/actuator/health
   curl localhost:30082/portfolio
   ```

What is in `k8s/`:

| File | Contains |
|---|---|
| `mysql.yaml` | Secret (passwords), headless Service `mysql`, StatefulSet with a 1 GiB PersistentVolumeClaim template |
| `market-service.yaml` | Deployment and a Service (`http://market-service:8081` inside the cluster) |
| `portfolio-service.yaml` | Deployment and a NodePort Service on 30082 |

To remove everything:

```bash
kubectl delete -f k8s/
kubectl delete pvc mysql-data-mysql-0   # the database disk is kept until you delete it
```

## 4. Run Jenkins locally on macOS

```bash
brew install jenkins-lts
brew services start jenkins-lts
```

1. Open http://localhost:8080. The first-time password is in `~/.jenkins/secrets/initialAdminPassword`.
2. Install the suggested plugins and create your admin user.
3. Choose **New Item > Pipeline**. Under **Pipeline**, pick **Pipeline script from SCM**, choose **Git**,
   enter this repository's URL, and leave the script path as `Jenkinsfile`.
4. Click **Build Now**.

The [Jenkinsfile](Jenkinsfile) has 7 stages: Checkout, Build, Unit Tests, Docker Build, Deploy Database,
Deploy Services, Smoke Test. It expects Docker Desktop to be running with Kubernetes enabled (section 3),
because Jenkins runs `docker` and `kubectl` on the same Mac.

Jenkins uses port 8080 and the services use 8081 and 8082, so they do not clash.

To stop Jenkins: `brew services stop jenkins-lts`.
