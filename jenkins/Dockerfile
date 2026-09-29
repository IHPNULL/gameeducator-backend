# Build context: repo root (see jenkins/docker-compose.yml)
#
# Jenkins usado SO localmente por este repositorio (nunca publicado). Alem da
# imagem oficial, adiciona: Docker CLI + plugins Compose/Buildx (para falar
# com o servico "docker" via TCP/TLS - Docker-in-Docker, ver
# jenkins/docker-compose.yml) e os plugins do Jenkins fixados em plugins.txt.
# O job "gameeducator" e criado automaticamente na subida por um script em
# init.groovy.d, que le o Jenkinsfile do repositorio (ver esse arquivo).
#
# Versoes de Docker CLI/Compose/Buildx: as MESMAS baixadas manualmente no
# host para validar Docker-in-Docker (ver PROJECT-BRIEF.md/D5). O download
# resolve automaticamente arm64 ou amd64 (variavel TARGETARCH, preenchida
# pelo BuildKit), entao a mesma imagem builda em qualquer arquitetura.
FROM jenkins/jenkins:2.568.3-lts-jdk21

USER root

ARG TARGETARCH
ARG DOCKER_VERSION=29.8.1
ARG COMPOSE_VERSION=5.5.1
ARG BUILDX_VERSION=0.37.1

# docker-compose publica sha256 por arquivo; docker CLI (tar estatico) e
# buildx nao publicam checksum por arquivo para download direto - baixados
# via HTTPS do dominio oficial (mesmo nivel de confianca de um
# "apt-get install docker-ce-cli").
RUN set -eux; \
    case "${TARGETARCH}" in \
        amd64) DOCKER_ARCH=x86_64; COMPOSE_ARCH=x86_64; BUILDX_ARCH=amd64 ;; \
        arm64) DOCKER_ARCH=aarch64; COMPOSE_ARCH=aarch64; BUILDX_ARCH=arm64 ;; \
        *) echo "Arquitetura nao suportada: ${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    curl -fsSL -o /tmp/docker.tgz \
        "https://download.docker.com/linux/static/stable/${DOCKER_ARCH}/docker-${DOCKER_VERSION}.tgz"; \
    tar -xzf /tmp/docker.tgz -C /tmp; \
    install -m 0755 /tmp/docker/docker /usr/local/bin/docker; \
    rm -rf /tmp/docker.tgz /tmp/docker; \
    mkdir -p /usr/local/libexec/docker/cli-plugins; \
    curl -fsSL -o /usr/local/libexec/docker/cli-plugins/docker-compose \
        "https://github.com/docker/compose/releases/download/v${COMPOSE_VERSION}/docker-compose-linux-${COMPOSE_ARCH}"; \
    curl -fsSL -o /tmp/docker-compose.sha256 \
        "https://github.com/docker/compose/releases/download/v${COMPOSE_VERSION}/docker-compose-linux-${COMPOSE_ARCH}.sha256"; \
    COMPOSE_SHA256="$(cut -d' ' -f1 /tmp/docker-compose.sha256)"; \
    printf '%s  %s\n' "${COMPOSE_SHA256}" /usr/local/libexec/docker/cli-plugins/docker-compose > /tmp/docker-compose.sha256.check; \
    sha256sum -c /tmp/docker-compose.sha256.check; \
    rm -f /tmp/docker-compose.sha256 /tmp/docker-compose.sha256.check; \
    chmod 0755 /usr/local/libexec/docker/cli-plugins/docker-compose; \
    curl -fsSL -o /usr/local/libexec/docker/cli-plugins/docker-buildx \
        "https://github.com/docker/buildx/releases/download/v${BUILDX_VERSION}/buildx-v${BUILDX_VERSION}.linux-${BUILDX_ARCH}"; \
    chmod 0755 /usr/local/libexec/docker/cli-plugins/docker-buildx; \
    docker --version; \
    docker compose version; \
    docker buildx version

COPY jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# casc.yaml fica FORA de /var/jenkins_home (que e o volume persistente
# "jenkins-data"): assim, reconstruir esta imagem sempre aplica a versao
# atual do arquivo, sem depender do mecanismo de "copia so na primeira
# subida" que a imagem oficial usa para arquivos dentro do JENKINS_HOME.
COPY jenkins/casc.yaml /var/jenkins_config/casc.yaml
ENV CASC_JENKINS_CONFIG=/var/jenkins_config/casc.yaml

# init.groovy.d ja mora dentro do JENKINS_HOME: a imagem oficial so copia
# esses arquivos para o volume na PRIMEIRA subida (ver jenkins/README.md
# sobre "docker compose down -v" ao alterar este script).
COPY jenkins/init.groovy.d/create-job.groovy /usr/share/jenkins/ref/init.groovy.d/create-job.groovy

ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

USER jenkins
