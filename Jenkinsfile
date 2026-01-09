pipeline {
    agent any

    options {
        timeout(time: 2, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        COMPOSE_PROJECT_NAME = 'library-app'
    }

    stages {
        stage('📥 Checkout') {
            steps {
                checkout scm
                sh 'chmod +x ./mvnw'
            }
        }

        stage('🔨 Build') {
            steps {
                sh './mvnw clean compile -DskipTests -q'
            }
        }

        stage('🧪 Unit Tests') {
            steps {
                sh './mvnw test -Dtest=!*IntegrationTest,!*SeleniumTest,!*E2E* -DargLine="-Xmx512m" -q'
            }
        }

        stage('🔗 Integration Tests') {
            steps {
                sh './mvnw test -Dtest=*IntegrationTest -DargLine="-Xmx512m" -Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=300 -q'
            }
        }

        stage('🏗️ Frontend Build') {
            steps {
                dir('frontend') {
                    sh 'npm install --silent'
                    sh 'npm run build'
                }
            }
        }

        stage('🐳 Docker Build & Run') {
            steps {
                script {
                    try {
                        sh 'docker compose down -v'
                    } catch (Exception e) {
                        echo 'Devam ediliyor...'
                    }
                    sh 'docker compose up -d --build'
                    sh 'sleep 30'
                    sh 'docker compose ps'
                }
            }
        }

        stage('🌐 Selenium E2E Tests') {
            steps {
                sh './mvnw failsafe:integration-test failsafe:verify -DskipUnitTests -Dincludes="**/*SeleniumTest.java,**/*E2ETest*.java" -q'
            }
        }

        stage('📊 Test Reports') {
            steps {
                junit allowEmptyResults: true, 
                      testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'
            }
        }

        stage('📈 Code Coverage') {
            steps {
                sh './mvnw jacoco:report -q'
                publishHTML([
                    reportDir: 'target/site/jacoco',
                    reportFiles: 'index.html',
                    reportName: 'JaCoCo Code Coverage'
                ])
            }
        }
    }

    post {
        always {
            sh 'docker compose logs > docker-logs.txt || true'
            archiveArtifacts artifacts: 'target/*.jar,docker-logs.txt', fingerprint: true, allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'
        }
        success {
            echo '✅ Build başarılı!'
        }
        failure {
            echo '❌ Build başarısız!'
            sh 'docker compose logs || true'
        }
        cleanup {
            sh 'docker-compose down || true'
        }
    }
}