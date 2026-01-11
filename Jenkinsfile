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

        stage('📥 Clean & Checkout') {
            steps {
                // Workspace temizleme
                deleteDir()
                // Git checkout
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],  // branch ismi doğru olduğundan emin ol
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'CloneOption', noTags: false, shallow: false, depth: 0]],
                    userRemoteConfigs: [[url: 'https://github.com/EmreS0000/LibraryManagement.git']]
                ])
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
                timeout(time: 5, unit: 'MINUTES') {
                    sh './mvnw test -Dtest=*IntegrationTest -DargLine="-Xmx512m" -q'
                }
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
                    // Eski container varsa kapat
                    try { sh 'docker compose down -v' } catch(Exception e) { echo 'Devam ediliyor...' }
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
        }
        success {
            echo '✅ Build başarılı!'
        }
        failure {
            echo '❌ Build başarısız!'
        }
        cleanup {
            sh 'docker compose down -v || true'
        }
    }
}
