// CloudTrader pipeline: 7 stages, from source code to a running, checked deployment.
// Assumes Jenkins runs on the same Mac as Docker Desktop (with Kubernetes switched on),
// so "docker" and "kubectl" talk to the local machine.
pipeline {
    agent any

    environment {
        // Homebrew and Docker Desktop install their tools here; Jenkins does not look there by default
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
    }

    stages {
        // 1. Get the code from Git
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // 2. Compile both services and build the jars (tests run in the next stage)
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests package -f market-service/pom.xml'
                sh 'mvn -B -DskipTests package -f portfolio-service/pom.xml'
            }
        }

        // 3. Run the unit tests. They are plain Java, so no database is needed
        stage('Unit Tests') {
            steps {
                sh 'mvn -B test -f market-service/pom.xml'
                sh 'mvn -B test -f portfolio-service/pom.xml'
            }
        }

        // 4. Pack each jar into a Docker image
        stage('Docker Build') {
            steps {
                sh 'docker build -t market-service:1.0 market-service'
                sh 'docker build -t portfolio-service:1.0 portfolio-service'
            }
        }

        // 5. Start MySQL (Secret + Service + StatefulSet) and wait until it is ready
        stage('Deploy Database') {
            steps {
                sh 'kubectl apply -f k8s/mysql.yaml'
                sh 'kubectl rollout status statefulset/mysql --timeout=180s'
            }
        }

        // 6. Start both services and wait until they are ready.
        //    The image tag stays "1.0", so we restart the pods to make them pick up the new image.
        stage('Deploy Services') {
            steps {
                sh 'kubectl apply -f k8s/market-service.yaml -f k8s/portfolio-service.yaml'
                sh 'kubectl rollout restart deployment/market-service deployment/portfolio-service'
                sh 'kubectl rollout status deployment/market-service --timeout=180s'
                sh 'kubectl rollout status deployment/portfolio-service --timeout=180s'
            }
        }

        // 7. Ask the deployed portfolio-service if it is healthy. curl -f fails the stage on any error
        stage('Smoke Test') {
            steps {
                sh 'curl -f http://localhost:30082/actuator/health'
            }
        }
    }
}
