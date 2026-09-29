# Jenkins local

Este Jenkins e so para uso local: cada pessoa do grupo sobe o seu, na sua
propria maquina, com Docker. Ele nao instala nada no seu computador fora de
containers, e nao publica nada na internet.

## O que voce precisa

- Docker (com Docker Compose) instalado. So isso - Java, Node e Maven ficam
  **dentro** das imagens, voce nao precisa instalar nenhum deles.

## 1. Subir o Jenkins

A partir da raiz do repositorio (a pasta `Devops-qa-gp6-main`):

```bash
docker compose -f jenkins/docker-compose.yml up -d --build
```

Isso builda e sobe duas coisas:

- `docker`: um Docker "de dentro" so para o Jenkins usar (Docker-in-Docker).
  E onde as imagens do projeto (backend, frontend, api-tests, ui-tests) sao
  construidas e rodadas. Nao aparece no `docker images` da sua maquina - so
  dentro deste Jenkins.
- `jenkins`: o Jenkins em si, ja configurado, com o job **gameeducator** ja
  criado a partir do `Jenkinsfile` deste repositorio.

Espere uns 15-30 segundos e abra:

**http://127.0.0.1:8090**

Usuario e senha: **admin / admin** (uso local; para trocar, copie
`jenkins/.env.example` para `jenkins/.env` e edite antes do primeiro
`up --build`).

## 2. Rodar a pipeline

No Jenkins, entre no job **gameeducator** e clique em **Build Now** (ou
**Construir agora**). Acompanhe em **Console Output**.

A pipeline faz, nesta ordem, parando no primeiro erro:

1. Pega o codigo (desta pasta local).
2. Builda a imagem de **teste** do backend e roda `./mvnw verify` dentro
   dela (os 91 testes + o gate de cobertura do JaCoCo).
3. Builda a imagem **final** do backend (`gameeducator-backend`).
4. Builda a imagem de **teste** do frontend e roda o Vitest dentro dela.
5. Builda a imagem **final** do frontend (`gameeducator-frontend`).
6. Builda as imagens `gameeducator-api-tests` e `gameeducator-ui-tests`.
7. Sobe backend + frontend + Postgres (as imagens do passo 3 e 5) e confere
   se o backend responde.
8. Sobe backend + frontend no perfil de automacao (H2 com dados fixos) e
   roda as duas suites Cucumber (API e UI) contra elas.
9. `Publish`: so roda na branch `main` e com um registry configurado -
   nesta configuracao local, sempre fica pulado (ver "Limitacoes" abaixo).

Se um teste quebrar, o estagio correspondente fica vermelho, os estagios
seguintes aparecem como "skipped", e o build final fica **FAILURE** - a
pipeline nao segue em frente com um teste quebrado.

## 3. Ver as imagens geradas

As imagens ficam dentro do Docker-in-Docker do Jenkins (servico `docker`),
nao no Docker da sua maquina. Para listar:

```bash
docker compose -f jenkins/docker-compose.yml exec docker docker images 'gameeducator-*'
```

Nomes: `gameeducator-backend`, `gameeducator-frontend`,
`gameeducator-api-tests`, `gameeducator-ui-tests`. A tag e o commit do Git
(12 caracteres) quando existir, ou `local-<numero do build>`.

Quer usar essas imagens fora do Jenkins (por exemplo, para rodar a
aplicacao)? Builde-as direto na sua maquina, sem passar pelo Jenkins:

```bash
docker compose -f docker/docker-compose.yml up --build
```

## 4. Parar

```bash
docker compose -f jenkins/docker-compose.yml down      # para, mantem o Jenkins configurado (job, historico) e as imagens ja construidas
docker compose -f jenkins/docker-compose.yml down -v   # para e apaga tudo (Jenkins do zero na proxima subida)
```

## Se voce mudar algo dentro de `jenkins/`

O Jenkins so copia `plugins.txt`/`casc.yaml`/o script que cria o job para
dentro do seu "HD" (volume `jenkins-data`) na **primeira vez** que ele sobe.
Se voce editar esses arquivos depois, rode `down -v` antes do proximo
`up --build` para garantir que a versao nova seja usada. Isso NAO vale para
o `Jenkinsfile` da raiz do repositorio: ele e lido de novo a cada vez que o
Jenkins inicia (`docker compose ... up` ou `restart`), sem precisar de
`down -v`.

## Limitacoes conhecidas

- **`Publish` nunca roda aqui.** Esse estagio usa o passo `docker.withRegistry(...)`,
  que vem do plugin "Docker Pipeline" (`docker-workflow`) - ele nao esta
  instalado neste Jenkins local, de proposito, porque este projeto nunca
  publica imagem nenhuma. O estagio so seria executado numa branch `main`
  com um registry configurado, o que nao existe neste ambiente local -
  entao ele nunca chega a precisar do plugin.
- **O codigo vem de uma pasta local, nao do Git.** Esta copia do repositorio
  nao tem historico Git. O Jenkins le o codigo direto da pasta (montada
  so-leitura em `/src`), em vez de um "checkout" de um repositorio remoto.
  Se um dia este projeto ganhar um repositorio Git de verdade, o mesmo
  `Jenkinsfile` funciona com "checkout scm" normalmente (o proprio
  Jenkinsfile ja sabe escolher entre os dois, ver o 1o estagio).
- **So testado no Docker deste Mac (Apple Silicon, via Colima).** Deve
  funcionar em qualquer Docker com suporte a `privileged: true` (a maioria
  funciona), mas isso nao foi testado em outro sistema operacional.
