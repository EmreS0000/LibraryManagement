pipeline {
    agent any

    options {
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        COMPOSE_PROJECT_NAME = 'library-app'
    }

    stages {
        stage('📥 Checkout') {
            steps {
                echo '📥 GitHub\'dan kodlar çekiliyor...'
                checkout scm
            }
        }

        stage('🔨 Build') {
            steps {
                echo '🔨 Proje build ediliyor...'
                sh './mvnw clean compile -DskipTests'
            }
        }

        stage('🧪 Unit Tests') {
            steps {
                echo '🧪 Birim testleri çalıştırılıyor...'
                sh './mvnw test -Dtest=!*IntegrationTest,!*SeleniumTest,!*E2ETest'
            }
        }

        stage('🔗 Integration Tests') {
            steps {
                echo '🔗 Entegrasyon testleri çalıştırılıyor...'
                sh './mvnw test -Dtest=*IntegrationTest'
            }
        }

        stage('🏗️ Frontend Build') {
            steps {
                echo '🏗️ Frontend build ediliyor...'
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }

        stage('🐳 Docker Build & Run') {
            steps {
                echo '🐳 Docker containers başlatılıyor...'
                script {
                    try {
                        sh 'docker-compose down -v'
                    } catch (Exception e) {
                        echo '⚠️ Container yok, devam ediliyor...'
                    }
                    sh 'docker-compose up -d --build'
                    echo '⏳ Uygulamanın başlatılması için 30 saniye bekleniyor...'
                    sh 'sleep 30'
                    sh 'docker-compose ps'
                }
            }
        }

        stage('🌐 Selenium E2E Tests') {
            steps {
                echo '🌐 Selenium E2E testleri çalıştırılıyor...'
                sh './mvnw failsafe:integration-test failsafe:verify -DskipUnitTests -Dincludes="**/*SeleniumTest.java,**/*E2ETest*.java"'
            }
        }

        stage('📊 Test Reports') {
            steps {
                echo '📊 Test raporları hazırlanıyor...'
                junit allowEmptyResults: true, 
                      testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'
            }
        }

        stage('📈 Code Coverage') {
            steps {
                echo '📈 Kod kapsama raporu oluşturuluyor...'
                sh './mvnw jacoco:report'
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
            echo '🧹 Cleanup işlemleri yapılıyor...'
            sh 'docker-compose logs > docker-logs.txt || true'
            archiveArtifacts artifacts: 'target/*.jar,docker-logs.txt', fingerprint: true, allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml'
        }
        success {
            echo '✅ Pipeline başarılı!'
        }
        failure {
            echo '❌ Pipeline başarısız!'
            sh 'docker-compose logs || true'
        }
        cleanup {
            echo '🧹 Docker containers kapatılıyor...'
            sh 'docker-compose down || true'
        }
    }
}