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

                script {
                    // Windows agent'ta sh/chmod patlar; Unix'te aynı kalsın
                    if (isUnix()) {
                        sh 'chmod +x ./mvnw'
                    } else {
                        // Windows'ta chmod gerekmez (mvnw.cmd zaten çalıştırılabilir)
                        echo 'Windows agent: chmod atlandı.'
                    }
                }
            }
        }

        stage('🔨 Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh './mvnw clean compile -DskipTests -T 1 -q'
                    } else {
                        bat 'mvnw.cmd clean compile -DskipTests -T 1 -q'
                    }
                }
            }
        }

        stage('🧪 Unit Tests') {
            steps {
                script {
                    if (isUnix()) {
                        sh './mvnw test -Dtest=!*IntegrationTest,!*SeleniumTest,!*E2E* -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" -q'
                    } else {
                        bat 'mvnw.cmd test -Dtest=!*IntegrationTest,!*SeleniumTest,!*E2E* -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" -q'
                    }
                }
            }
        }

        stage('🔗 Integration Tests') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {
                        if (isUnix()) {
                            sh './mvnw test -Dtest=*IntegrationTest -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" -q'
                        } else {
                            bat 'mvnw.cmd test -Dtest=*IntegrationTest -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" -q'
                        }
                    }
                }
            }
        }

stage('🏗️ Frontend Build') {
    steps {
        dir('frontend') {
            script {
                if (isUnix()) {
                    sh 'npm -v'
                    sh 'npm install --silent --prefer-offline --no-audit'
                    sh 'npm run build'
                } else {
                    // Windows: npm PATH'te görünmeyebiliyor (Jenkins service hesabı).
                    // O yüzden direkt npm.cmd tam yolundan çağırıyoruz.
                    def NPM = 'C:\\Program Files\\nodejs\\npm.cmd'

                    // Teşhis amaçlı: log'a bas (istersen sonra silebilirsin)
                    bat 'where node || ver'
                    bat 'where npm || ver'
                    bat 'node -v'

                    // Asıl çözüm:
                    bat "\"${NPM}\" -v"
                    bat "\"${NPM}\" install --silent --prefer-offline --no-audit"
                    bat "\"${NPM}\" run build"
                }
            }
        }
    }
}

        stage('🐳 Docker Build & Run') {
            steps {
                script {
                    try {
                        if (isUnix()) {
                            sh 'docker-compose down -v || true'
                            sh 'sleep 5'
                        } else {
                            // Windows bat içinde "|| true" yok; hata olsa da pipeline durmasın diye try/catch zaten var
                            bat 'docker-compose down -v'
                            bat 'powershell -Command "Start-Sleep -Seconds 5"'
                        }
                    } catch(Exception e) {
                        echo 'Devam ediliyor...'
                    }

                    if (isUnix()) {
                        sh 'docker-compose up -d --build'
                        sh 'sleep 40'
                        sh 'docker-compose ps'
                    } else {
                        bat 'docker-compose up -d --build'
                        bat 'powershell -Command "Start-Sleep -Seconds 40"'
                        bat 'docker-compose ps'
                    }
                }
            }
        }

        stage('🌐 Selenium E2E Tests') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {
                        if (isUnix()) {
                            sh './mvnw failsafe:integration-test failsafe:verify -DskipUnitTests -Dincludes="**/*SeleniumTest.java,**/*E2ETest*.java" -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC" -q'
                        } else {
                            bat 'mvnw.cmd failsafe:integration-test failsafe:verify -DskipUnitTests -Dincludes="**/*SeleniumTest.java,**/*E2ETest*.java" -DforkCount=1 -DreuseForks=true -DargLine="-Xmx192m -Xms128m -XX:MaxMetaspaceSize=96m -XX:+UseSerialGC" -q'
                        }
                    }
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
                script {
                    if (isUnix()) {
                        sh './mvnw jacoco:report -q'
                    } else {
                        bat 'mvnw.cmd jacoco:report -q'
                    }
                }

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
            script {
                if (isUnix()) {
                    sh 'docker-compose logs > docker-logs.txt || true'
                } else {
                    // Windows'ta redirect var, ama komut hata verirse pipeline'ı düşürmesin
                    try {
                        bat 'docker-compose logs > docker-logs.txt'
                    } catch(Exception e) {
                        echo 'docker-compose logs alınamadı (Windows).'
                    }
                }
            }
            archiveArtifacts artifacts: 'target/*.jar,docker-logs.txt', fingerprint: true, allowEmptyArchive: true
        }
        success {
            echo '✅ Build başarılı!'
        }
        failure {
            echo '❌ Build başarısız!'
        }
        cleanup {
            script {
                try {
                    if (isUnix()) {
                        sh 'docker-compose down -v || true'
                        sh 'docker system prune -f || true'
                    } else {
                        bat 'docker-compose down -v'
                        bat 'docker system prune -f'
                    }
                } catch(Exception e) {
                    echo 'Cleanup sırasında hata oldu ama yutuldu.'
                }
            }
        }
    }
}
