# Jenkins local

Este Jenkins e so para uso local: cada pessoa do grupo sobe o seu, na sua
propria maquina, com Docker. Ele nao instala nada no seu computador fora de
containers, e nao publica nada na internet.

## O que voce precisa

- Docker (com Docker Compose) instalado. So isso - Java, Node e Maven ficam
  **dentro** das imagens, voce nao precisa instalar nenhum deles.

## 1. Subir o Jenkins

Clone os 3 repositorios lado a lado (a pasta pai e montada no Jenkins):

```
gameeducator/
├── gameeducator-backend      # este repo (contem jenkins/)
├── gameeducator-frontend
└── gameeducator-automation
```

Se estiverem em outro lugar, defina `SOURCES_DIR=/caminho/da/pasta/pai` em
`jenkins/.env` (veja `.env.example`). Depois, a partir da raiz de
`gameeducator-backend`:

```bash
docker compose -f jenkins/docker-compose.yml up -d --build
```

Isso builda e sobe duas coisas:

- `docker`: um Docker "de dentro" so para o Jenkins usar (Docker-in-Docker).
  E onde as imagens do projeto (backend e Postgres) sao
  construidas e rodadas. Nao aparece no `docker images` da sua maquina - so
  dentro deste Jenkins.
- `jenkins`: o Jenkins em si, ja configurado, com os 3 jobs (**gameeducator-backend**,
  **gameeducator-frontend** e **gameeducator-automation**) ja
  criado a partir do `Jenkinsfile` de cada repositorio.

Espere uns 15-30 segundos e abra:

**http://127.0.0.1:8090**

Usuario e senha: **admin / admin** (uso local; para trocar, copie
`jenkins/.env.example` para `jenkins/.env` e edite antes do primeiro
`up --build`).

## 2. Rodar a pipeline

No Jenkins, rode **Build Now** (ou **Construir agora**) nos jobs
**gameeducator-backend** e **gameeducator-frontend**. Quando um deles termina
com sucesso, o job **gameeducator-automation** dispara sozinho (backend e
frontend sao construidos a partir das pastas locais) e roda as suites
Cucumber de API e UI. Acompanhe em **Console Output**.

O job **gameeducator-backend** faz, nesta ordem, parando no primeiro erro:

1. Pega o codigo (da pasta local do repositorio).
2. Builda a imagem de **teste** do backend e roda `./mvnw verify` dentro
   dela (os testes + o gate de cobertura do JaCoCo).
3. Builda a imagem **final** do backend (`gameeducator-backend`).
4. Sobe backend + Postgres (a imagem do passo 3) e confere se o backend
   responde.
5. `Publish`: so roda na branch `main` e com um registry configurado -
   nesta configuracao local, sempre fica pulado (ver "Limitacoes" abaixo).

Se um teste quebrar, o estagio correspondente fica vermelho, os estagios
seguintes aparecem como "skipped", e o build final fica **FAILURE** - a
pipeline nao segue em frente com um teste quebrado.

> **gameeducator-frontend**: testes Vitest na imagem + build da imagem Nginx.
> **gameeducator-automation**: builda backend e frontend das pastas locais, sobe
> os dois no perfil `automation` e roda Cucumber API + UI.

## 3. Ver as imagens geradas

As imagens ficam dentro do Docker-in-Docker do Jenkins (servico `docker`),
nao no Docker da sua maquina. Para listar:

```bash
docker compose -f jenkins/docker-compose.yml exec docker docker images 'gameeducator-*'
```

Nome: `gameeducator-backend`. A tag e o commit do Git
(12 caracteres) quando existir, ou `local-<numero do build>`.

Quer usar essa imagem fora do Jenkins (por exemplo, para rodar a
aplicacao)? Builde-a direto na sua maquina, sem passar pelo Jenkins:

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
- **O codigo vem das pastas locais, nao do GitHub.** O Jenkins le cada
  repositorio direto da pasta pai montada so-leitura em `/src` (variavel
  `LOCAL_SOURCES_ROOT`), entao testa o que esta na sua maquina, inclusive
  alteracoes nao commitadas. Sem essa variavel, os Jenkinsfiles usam
  "checkout scm" normalmente.
- **So testado no Docker deste Mac (Apple Silicon, via Colima).** Deve
  funcionar em qualquer Docker com suporte a `privileged: true` (a maioria
  funciona), mas isso nao foi testado em outro sistema operacional.
