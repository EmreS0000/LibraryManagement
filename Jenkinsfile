pipeline {
    agent any

    options {
        timeout(time: 2, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_REGISTRY = 'docker.io'
        COMPOSE_PROJECT_NAME = 'library-app'
        MAVEN_OPTS = '-Xmx384m -Xms256m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1'
        NODE_OPTIONS = '--max-old-space-size=512'
    }

    stages {

        stage('📥 Clean & Checkout') {
            steps {
                deleteDir()
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'CloneOption', noTags: false, shallow: false, depth: 0]],
                    userRemoteConfigs: [[url: 'https://github.com/EmreS0000/LibraryManagement.git']]
                ])
                sh 'chmod +x ./mvnw'
            }
        }

        stage('🔨 Build') {
            steps {
                sh './mvnw clean compile -DskipTests -T 1 -q'
            }
        }

        stage('🧪 Unit Tests') {
            steps {
                sh './mvnw test -Dtest=!*IntegrationTest,!*SeleniumTest,!*E2E* -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" -q'
            }
        }

        stage('🔗 Integration Tests') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    sh './mvnw test -Dtest=*IntegrationTest -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" -q'
                }
            }
        }

        stage('🏗️ Frontend Build') {
            steps {
                dir('frontend') {
                    sh 'npm install --silent --prefer-offline --no-audit'
                    sh 'npm run build'
                }
            }
        }

        stage('🐳 Docker Build & Run') {
            steps {
                script {
                    try { 
                        sh 'docker-compose down -v || true'
                        sh 'sleep 5'
                    } catch(Exception e) { 
                        echo 'Devam ediliyor...' 
                    }
                    sh 'docker-compose up -d --build'
                    sh 'sleep 40'
                    sh 'docker-compose ps'
                }
            }
        }

        stage('🌐 Selenium E2E Tests') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    sh './mvnw failsafe:integration-test failsafe:verify -DskipUnitTests -Dincludes="**/*SeleniumTest.java,**/*E2ETest*.java" -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC" -q'
                }
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
            sh 'docker-compose logs > docker-logs.txt || true'
            archiveArtifacts artifacts: 'target/*.jar,docker-logs.txt', fingerprint: true, allowEmptyArchive: true
        }
        success {
            echo '✅ Build başarılı!'
        }
        failure {
            echo '❌ Build başarısız!'
        }
        cleanup {
            sh 'docker-compose down -v || true'
            sh 'docker system prune -f || true'
        }
    }
}
