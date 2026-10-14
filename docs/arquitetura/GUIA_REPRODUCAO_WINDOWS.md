# Guia: reproduzir o B1 (Jenkins + labs + Prometheus/Grafana) em uma máquina Windows

Objetivo: subir a esteira Jenkins dos 4 repositórios, rodar os mini-labs dos desafios 3 a 6 e tirar os prints
(Jenkins, Prometheus, Grafana) que compõem o relatório `B1_Decisoes_Arquiteturais_e_Evidencias.docx`.

Tudo roda em containers. A única coisa que você **escreve** são comandos de terminal (copie e cole os blocos) e,
no Jenkins, o login local. Não é preciso instalar Java, Maven, Node nem Python no Windows para a esteira; o Python
só é usado pelos scripts dos labs, e roda **dentro do WSL** (passo 2).

## 0. O que você precisa ter

| Item | Versão | Para quê |
|---|---|---|
| Windows 10 22H2 ou 11 | 64 bits, virtualização ligada na BIOS | WSL2 e Docker |
| 16 GB de RAM (mínimo 12) | — | Jenkins + Docker-in-Docker + lab juntos pesam |
| ~25 GB de disco livre | — | imagens Docker (o dind do Jenkins guarda as suas) |
| Docker Desktop | 4.30 ou mais novo (Compose v2) | motor de containers |
| WSL2 com Ubuntu | — | executa os scripts `.sh` dos labs |
| Git | qualquer recente (dentro do Ubuntu) | clonar os repositórios |

Resumo do que será digitado: instalar WSL/Docker (passo 1), 3 comandos de `git config`/`git clone` (passo 3),
2 comandos `docker compose` (passos 4 e 6) e 4 comandos `bash labs/desafio-N/executar.sh` (passo 7).

## 1. Instalar WSL2 e Docker Desktop

1. Abra o **PowerShell como administrador** e rode:

   ```powershell
   wsl --install -d Ubuntu
   ```

   Reinicie quando pedir e crie usuário/senha do Ubuntu.
2. Instale o **Docker Desktop** (docker.com/products/docker-desktop). Em *Settings → General* deixe marcado
   **Use the WSL 2 based engine**. Em *Settings → Resources → WSL integration* ligue a integração com **Ubuntu**.
3. Em *Settings → Resources*, dê ao Docker **pelo menos 8 GB de memória** e 4 CPUs (ou crie o arquivo
   `%UserProfile%\.wslconfig` com `[wsl2]` e `memory=10GB`, depois rode `wsl --shutdown`).
4. Abra o **Ubuntu** (menu Iniciar) e confira:

   ```bash
   docker --version
   docker compose version
   ```

> Todos os comandos dos passos seguintes são digitados **no terminal do Ubuntu**, não no PowerShell.

## 2. Instalar o que os scripts dos labs usam (dentro do Ubuntu)

```bash
sudo apt update && sudo apt install -y git curl python3
```

(O Chrome headless usado por um dos scripts para fotografar o Grafana é opcional: sem ele o script só pula essa foto;
os prints do relatório foram tirados à mão.)

## 3. Clonar os 4 repositórios lado a lado — dentro do Linux, não em `C:\`

**Importante:** clone dentro do sistema de arquivos do Ubuntu (`~`), e não em `/mnt/c/...`. Em `/mnt/c` o Git do Windows
converte finais de linha para CRLF e os scripts `.sh` quebram com `bad interpreter` / `\r: command not found`.

```bash
git config --global core.autocrlf input
mkdir -p ~/gameeducator && cd ~/gameeducator

git clone -b b1-architecture-lab https://github.com/IHPNULL/gameeducator-backend.git
git clone -b b1-architecture-lab https://github.com/IHPNULL/gameeducator-frontend.git
git clone -b b1-architecture-lab https://github.com/IHPNULL/gameeducator-automation.git
git clone https://github.com/IHPNULL/gameeducator-ia.git
ls
```

As 4 pastas **precisam manter exatamente esses nomes** e ficar na mesma pasta pai: o Jenkins monta a pasta pai em
`/src` e cada job lê o `Jenkinsfile` do repositório pelo nome.

## 4. Subir o Jenkins

```bash
cd ~/gameeducator/gameeducator-backend
docker compose -f jenkins/docker-compose.yml up -d --build
```

A primeira vez baixa e constrói imagens (3 a 6 minutos). Quando terminar, abra no navegador do Windows:

**http://127.0.0.1:8090**

Login local (só para uso na própria máquina, definido em `jenkins/README.md`): usuário `admin`, senha `admin`.

Devem existir 4 jobs: `gameeducator-backend`, `gameeducator-frontend`, `gameeducator-ia`, `gameeducator-automation`.

## 5. Rodar a esteira e tirar os prints

1. Clique em **Construir agora** (*Build Now*) nos jobs `gameeducator-backend`, `gameeducator-ia` e `gameeducator-frontend`.
   O `gameeducator-automation` dispara sozinho quando backend ou frontend termina com sucesso.
2. O primeiro build do backend leva uns 6–7 minutos (baixa as dependências dentro do Docker-in-Docker); os seguintes são mais rápidos.
3. Esperado: os 4 jobs verdes. No backend, o **Stage View** mostra: Código-fonte → Backend: testes → **Backend: PMD** →
   Backend: imagem → Integração (Postgres) → Publish (pulado, só roda na `main`).
4. Prints a tirar (tecla **Win + Shift + S**):
   - painel inicial com os 4 jobs;
   - Stage View de cada job (página do job, rolando até a tabela);
   - `gameeducator-backend` → build → **Tests** (161 testes) e a página do build com a cobertura JaCoCo;
   - `gameeducator-automation` com os relatórios Cucumber (60 testes).

Se um build falhar, a causa está em **Console Output**. Erro de `fingerprint`/`Invalid parameter "artifacts"` significa que o
clone está numa versão antiga do backend: rode `git pull` na pasta do backend, depois
`docker compose -f jenkins/docker-compose.yml restart jenkins` (o `Jenkinsfile` é relido na reinicialização).

## 6. Subir a stack do laboratório (desafios 3 a 6)

Antes, confira que nada mais usa as portas 8080, 5432, 8000, 8085, 9090, 3000, 15672 e 1883
(o Jenkins usa só a **8090**, então pode ficar ligado).

```bash
cd ~/gameeducator/gameeducator-backend
docker compose -f labs/docker-compose.yml up -d --build
docker compose -f labs/docker-compose.yml ps
```

Aguarde ~1 minuto até o Core responder (`curl http://localhost:8080/actuator/health` deve mostrar `"status":"UP"`).

| Serviço | URL no navegador do Windows |
|---|---|
| Core (Swagger) | http://localhost:8080/swagger-ui/index.html |
| Serviço de IA | http://localhost:8000/docs |
| RabbitMQ | http://localhost:15672 (usuário/senha `gameeducator` / `gameeducator`) |
| Prometheus | http://localhost:9090 (alvos em `/targets`, alertas em `/alerts`) |
| Grafana | http://localhost:3000 (anônimo; ou `admin` / `admin`) |

## 7. Rodar os mini-labs (cada um grava o resultado em `docs/arquitetura/evidencias/desafio-N/`)

Use `bash` explicitamente (os arquivos não têm permissão de execução):

```bash
cd ~/gameeducator/gameeducator-backend
bash labs/desafio-3/executar.sh      # IA lenta/parada -> fallback e disjuntor        (~2 min)
bash labs/desafio-4/executar.sh      # consumidor/broker fora -> sincrono x assincrono (~2 min)
bash labs/desafio-5/executar.sh      # MQTT com queda, duplicados e invalido           (~1 min)
bash labs/desafio-6/executar.sh      # provoca degradacoes e le Prometheus/alertas     (~6 min)
```

Não mexa nos containers enquanto cada script roda: eles derrubam e sobem serviços de propósito.

**Quando tirar os prints do Prometheus/Grafana:**

- Durante o `desafio-6`, abra `http://localhost:9090/alerts` por volta de 1:30–2:00 minutos depois de iniciar: com a IA parada
  o alerta `IaEmModoDegradado` fica **FIRING** (some quando a IA volta).
- `http://localhost:9090/targets` — print com os 4 alvos (core, ia, notificacao, rabbitmq) em **UP**.
- Grafana: `http://localhost:3000/d/gameeducator-b1/?orgId=1&from=now-15m&to=now&kiosk` — abra **depois** do `desafio-6` terminar
  (janela de 15 min cobre todos os labs) e espere uns 10 s os painéis carregarem.

## 8. Resultado esperado (conferência)

| Lab | O que deve aparecer |
|---|---|
| D3 | IA lenta: HTTP 200 com `origem=fallback` em ~2 s; IA parada: fallback em ~14 ms com disjuntor aberto; assistente 503 em ~20 ms; fluxo essencial (resposta, ranking) segue 200 |
| D4 | síncrono com consumidor fora: 503 e conclusão desfeita; assíncrono: HTTP 200, evento retido e entregue ao voltar; broker fora: evento pendente no outbox e reenviado |
| D5 | 20 eventos únicos gerados e 20 registrados; 3 duplicados descartados; 1 inválido na DLQ; ~152 bytes por evento MQTT |
| D6 | `IaEmModoDegradado` e `NotificacoesAcumulando` entram em *firing* e normalizam; p95 de `/ranking` ~3 ms |

Observação conhecida: no D4, a linha de controle "síncrono com consumidor no ar" pode retornar 503; o lab só comprova a metade da falha do modo síncrono.

## 9. Desligar e limpar

```bash
cd ~/gameeducator/gameeducator-backend
docker compose -f labs/docker-compose.yml down -v          # derruba o lab e apaga os volumes
docker compose -f jenkins/docker-compose.yml down          # para o Jenkins (mantém histórico)
docker compose -f jenkins/docker-compose.yml down -v       # para e apaga tudo (Jenkins do zero)
```

## 10. Problemas comuns

| Sintoma | Causa provável | Correção |
|---|---|---|
| `bad interpreter` ou `$'\r': command not found` ao rodar `.sh` | clonou em `/mnt/c` ou com `autocrlf=true` | refazer o passo 3 dentro de `~` com `core.autocrlf input` |
| `port is already allocated` | outro programa usa 8080/5432/9090/3000 | fechar o programa (ex.: Postgres local) ou desligar o outro compose |
| Jenkins não mostra os 4 jobs | pasta pai errada ou nomes das pastas diferentes | conferir os 4 nomes do passo 3 e rodar `docker compose -f jenkins/docker-compose.yml down -v` e subir de novo |
| Build do backend muito lento ou `OOMKilled` | pouca memória para o Docker | subir para 8–10 GB (passo 1.3) |
| `permission denied` em `labs/.../executar.sh` | falta de bit de execução | usar `bash labs/desafio-N/executar.sh` |
| Stage View não aparece no Jenkins | imagem do Jenkins antiga (sem o plugin) | `down -v` e `up -d --build` de novo |

## 11. Para o relatório

O relatório pronto está em `docs/arquitetura/B1_Decisoes_Arquiteturais_e_Evidencias.docx`, e os prints usados nele em
`docs/arquitetura/evidencias/prints/`. Para o seu próprio relatório, troque as imagens por prints seus dessa execução
(a avaliação pede evidências do processo executado pela própria equipe).
