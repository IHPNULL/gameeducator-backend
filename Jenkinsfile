// Pipeline do backend do GameEducator: builda a imagem, roda os testes DENTRO
// da imagem, sobe a integracao (Postgres) e a suite Cucumber de API contra o
// backend real (perfil "automation"). Qualquer falha para a pipeline.
// Jenkins local: ver jenkins/README.md.
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 40, unit: 'MINUTES')
        skipDefaultCheckout()
    }

    parameters {
        string(name: 'DOCKER_REGISTRY', defaultValue: '', description: 'Docker registry host para publicar a imagem em main')
    }

    environment {
        COMPOSE_PROJECT_NAME = "gameeducator-backend-${env.BUILD_NUMBER}"
    }

    stages {
        stage('Codigo-fonte') {
            steps {
                deleteDir()
                script {
                    // LOCAL_SOURCE_DIR e definido pelo Jenkins local (jenkins/docker-compose.yml):
                    // a pasta local chega por volume somente-leitura. Sem ele, usa "checkout scm".
                    if (env.LOCAL_SOURCE_DIR) {
                        sh """
                            tar -C "${env.LOCAL_SOURCE_DIR}" \\
                                --exclude=.git --exclude=target \\
                                -cf - . | tar -xf -
                        """
                    } else {
                        checkout scm
                    }
                }
                script {
                    env.IMAGE_TAG = sh(returnStdout: true, script: '''
                        if [ -n "$GIT_COMMIT" ]; then
                            echo "$GIT_COMMIT" | cut -c1-12
                        else
                            echo "local-${BUILD_NUMBER}"
                        fi
                    ''').trim()
                    echo "IMAGE_TAG=${env.IMAGE_TAG}  COMPOSE_PROJECT_NAME=${env.COMPOSE_PROJECT_NAME}"
                }
            }
        }

        stage('Backend: testes') {
            steps {
                sh "docker build -f docker/backend.Dockerfile --target test -t gameeducator-backend:${IMAGE_TAG}-test ."
                sh "docker run --name ${COMPOSE_PROJECT_NAME}-backend-test gameeducator-backend:${IMAGE_TAG}-test"
            }
            post {
                always {
                    sh "docker cp ${COMPOSE_PROJECT_NAME}-backend-test:/app/target target || true"
                    sh "docker rm -f ${COMPOSE_PROJECT_NAME}-backend-test || true"
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                    jacoco(
                        execPattern: 'target/jacoco.exec',
                        classPattern: 'target/classes',
                        sourcePattern: 'src/main/java'
                    )
                }
            }
        }

        stage('Backend: imagem') {
            steps {
                sh "docker build -f docker/backend.Dockerfile -t gameeducator-backend:${IMAGE_TAG} ."
                script {
                    if (env.BRANCH_NAME == 'main') {
                        sh "docker tag gameeducator-backend:${IMAGE_TAG} gameeducator-backend:latest"
                    }
                }
            }
        }

        stage('Automacao: imagem api-tests') {
            steps {
                // O build ja roda "mvnw test-compile" e falha se o codigo de teste nao compilar.
                sh "docker build -f docker/api-tests.Dockerfile -t gameeducator-api-tests:${IMAGE_TAG} api-tests"
            }
        }

        stage('Integracao (Postgres)') {
            steps {
                sh "docker compose -f docker/docker-compose.yml up -d --no-build db backend"
                sh '''
                    ok=0
                    for i in $(seq 1 30); do
                        if docker compose -f docker/docker-compose.yml exec -T backend wget -q -O- http://127.0.0.1:8080/v3/api-docs > /dev/null 2>&1; then
                            ok=1; break
                        fi
                        sleep 2
                    done
                    if [ "$ok" != "1" ]; then
                        echo "ERRO: backend (perfil postgres) nao respondeu em /v3/api-docs apos 60s" >&2
                        exit 1
                    fi
                '''
            }
            post {
                always {
                    sh 'docker compose -f docker/docker-compose.yml logs backend || true'
                    sh 'docker compose -f docker/docker-compose.yml down -v || true'
                }
            }
        }

        stage('Automacao: Cucumber API') {
            steps {
                sh "docker compose -f docker/docker-compose.yml -f docker/docker-compose.automation.yml up -d --no-build backend"
                sh '''
                    ok=0
                    for i in $(seq 1 30); do
                        if docker compose -f docker/docker-compose.yml -f docker/docker-compose.automation.yml exec -T backend wget -q -O- http://127.0.0.1:8080/v3/api-docs > /dev/null 2>&1; then
                            ok=1; break
                        fi
                        sleep 2
                    done
                    if [ "$ok" != "1" ]; then
                        echo "ERRO: backend (perfil automation) nao respondeu em /v3/api-docs apos 60s" >&2
                        exit 1
                    fi
                '''
                sh "docker run --name ${COMPOSE_PROJECT_NAME}-api-tests --network ${COMPOSE_PROJECT_NAME}_default gameeducator-api-tests:${IMAGE_TAG}"
            }
            post {
                always {
                    sh "docker cp ${COMPOSE_PROJECT_NAME}-api-tests:/app/target/cucumber-report api-tests-cucumber-report || true"
                    sh "docker cp ${COMPOSE_PROJECT_NAME}-api-tests:/app/target/surefire-reports api-tests-surefire-reports || true"
                    sh "docker rm -f ${COMPOSE_PROJECT_NAME}-api-tests || true"
                    sh 'docker compose -f docker/docker-compose.yml -f docker/docker-compose.automation.yml logs backend || true'
                    sh 'docker compose -f docker/docker-compose.yml -f docker/docker-compose.automation.yml down -v || true'
                    junit testResults: 'api-tests-surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'api-tests-cucumber-report/**', allowEmptyArchive: true
                }
            }
        }

        stage('Publish') {
            when {
                branch 'main'
            }
            steps {
                script {
                    if (!params.DOCKER_REGISTRY) {
                        echo 'DOCKER_REGISTRY nao configurado - pulando publicacao da imagem.'
                        return
                    }
                    docker.withRegistry("https://${params.DOCKER_REGISTRY}", 'gameeducator-docker-registry') {
                        sh "docker tag gameeducator-backend:latest ${params.DOCKER_REGISTRY}/gameeducator-backend:latest"
                        sh "docker push ${params.DOCKER_REGISTRY}/gameeducator-backend:latest"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (env.IMAGE_TAG) {
                    sh "docker rmi gameeducator-backend:${env.IMAGE_TAG}-test || true"
                }
            }
        }
    }
}
