pipeline {
    agent any

    tools {
        maven 'M3'
    }

    options {
        skipDefaultCheckout()
        disableConcurrentBuilds()
    }

    environment {
        APP_JAR = 'target/devops-lab8-app.jar'
        NEXUS_REPO_URL = 'http://lab7-nexus:8081/repository/maven-snapshots/'
        DOCKER_IMAGE = 'trungson05/devops-lab8-app'
        DOCKER_TAG = "${env.BUILD_NUMBER}"
        CONTAINER_NAME = 'lab8-cd-production'
        APP_PORT = '3002'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('CI: Build') {
            steps {
                dir('app') {
                    sh 'mvn -B -ntp clean compile'
                }
            }
        }

        stage('CI: Test') {
            steps {
                dir('app') {
                    sh 'mvn -B -ntp test'
                }
            }
            post {
                always {
                    junit 'app/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('CI: SonarQube Analysis') {
            steps {
                dir('app') {
                    withSonarQubeEnv(installationName: 'SonarQube', credentialsId: 'sonarqube-token') {
                        sh '''
                            set +x
                            export SONAR_TOKEN="$SONAR_AUTH_TOKEN"
                            mvn -B -ntp sonar:sonar -Dsonar.projectKey=devops-lab8
                        '''
                    }
                }
            }
        }

        stage('CI: Package') {
            steps {
                dir('app') {
                    sh 'mvn -B -ntp package -DskipTests'
                    archiveArtifacts artifacts: env.APP_JAR, fingerprint: true
                }
            }
        }

        stage('CI: Publish to Nexus') {
            steps {
                dir('app') {
                    withCredentials([usernamePassword(credentialsId: 'nexus-credentials',
                            usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
                        configFileProvider([configFile(fileId: 'maven-settings-nexus', variable: 'MAVEN_SETTINGS')]) {
                            sh '''
                                mvn -B -ntp -s "$MAVEN_SETTINGS" deploy:deploy-file \\
                                    -Dfile="$APP_JAR" -DpomFile=pom.xml \\
                                    -DrepositoryId=nexus -Durl="$NEXUS_REPO_URL"
                            '''
                        }
                    }
                }
            }
        }

        stage('CD: Docker Build') {
            steps {
                sh 'docker build --target ci -t "$DOCKER_IMAGE:$DOCKER_TAG" -t "$DOCKER_IMAGE:latest" .'
            }
        }

        stage('CD: Docker Scan') {
            steps {
                sh '''
                    docker scout quickview "$DOCKER_IMAGE:$DOCKER_TAG" || echo "Bỏ qua Docker Scout vì lệnh quét chưa chạy được."
                '''
            }
        }

        stage('CD: Push to Registry') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials_1',
                        usernameVariable: 'HUB_USER', passwordVariable: 'HUB_PASS')]) {
                    sh '''
                        export DOCKER_CONFIG="$(mktemp -d)"
                        trap 'rm -rf "$DOCKER_CONFIG"' EXIT
                        set +x
                        printf '%s' "$HUB_PASS" | docker login -u "$HUB_USER" --password-stdin
                        docker push "$DOCKER_IMAGE:$DOCKER_TAG"
                        docker push "$DOCKER_IMAGE:latest"
                    '''
                }
            }
        }

        stage('CD: Deploy Container') {
            steps {
                sh '''
                    if docker container inspect "$CONTAINER_NAME" >/dev/null 2>&1; then
                        docker rm -f "$CONTAINER_NAME"
                    fi
                    docker run -d --name "$CONTAINER_NAME" -p "$APP_PORT:3000" \\
                        --restart unless-stopped "$DOCKER_IMAGE:$DOCKER_TAG"
                '''
            }
        }

        stage('CD: Verify Deploy') {
            steps {
                sh '''
                    url="http://host.docker.internal:$APP_PORT"
                    curl -fsS --retry 10 --retry-all-errors --retry-delay 1 "$url/health"
                    curl -fsS "$url/"
                '''
            }
        }
    }

    post {
        success {
            echo "Đã triển khai ${DOCKER_IMAGE}:${DOCKER_TAG} trên cổng ${APP_PORT}."
        }
        always {
            cleanWs()
        }
    }
}
