# GameEducator — Backlog, User Stories e BDD

**Grupo 1 — Projeto:** GameEducator (plataforma gamificada de ensino)
**Integrantes:** 2

**Visão do produto:** o professor publica o conteúdo do curso em forma de **missões** compostas por **desafios** (quizzes/exercícios). O aluno joga as missões, acerta os desafios, ganha **XP** e **medalhas**, e acompanha seu progresso no **ranking** da turma.

**Legenda de prioridade (#P):** `E` = Essencial · `I` = Importante · `D` = Desejável
**Legenda de complexidade (#C):** story points (1, 2, 3, 5, 8)

---

## 1. PRODUCT BACKLOG

| #P | #C | As a type of user | I want | so that I can |
|----|----|-------------------|--------|---------------|
| E | 5 | Professor | Criar uma missão com seus desafios em um curso | Disponibilizar o conteúdo do curso de forma gamificada |
| E | 5 | Aluno | Resolver os desafios de uma missão do meu curso | Aprender o conteúdo jogando |
| E | 3 | Aluno | Receber XP ao concluir um desafio corretamente | Saber o meu progresso no curso |
| I | 3 | Aluno | Ver o ranking de XP da minha turma | Me comparar com os colegas e me motivar |
| I | 3 | Aluno | Ganhar medalhas ao atingir marcos de XP | Ter reconhecimento pelo meu esforço |
| I | 5 | Professor | Acompanhar o desempenho da turma nas missões | Identificar quem precisa de ajuda |
| D | 2 | Aluno | Ver o histórico das minhas tentativas em um desafio | Entender onde errei e revisar |
| D | 2 | Professor | Definir o valor de XP de cada desafio | Balancear a dificuldade da missão |

---

## 2. USER STORIES POR INTEGRANTE

### Integrante 1: _<nome>_ — **US01** (prioridade: Essencial)

| EU COMO | PRECISO / QUERO | PARA |
|---------|-----------------|------|
| Aluno | Resolver os desafios de uma missão e receber XP por acerto | Aprender jogando e saber o meu progresso |

### Integrante 2: _<nome>_ — **US02** (prioridade: Essencial)

| EU COMO | PRECISO / QUERO | PARA |
|---------|-----------------|------|
| Professor | Criar missões com desafios dentro de um curso | Disponibilizar o conteúdo do curso de forma gamificada |

> As duas user stories são de prioridade **essencial** porque juntas fecham o ciclo mínimo válido do produto: sem a US02 não existe conteúdo para jogar, e sem a US01 o conteúdo publicado não gera aprendizado nem progresso.

---

## 3. SPRINT BACKLOG — BDD / SCENARIOS / ACCEPTANCE CRITERIA

| Given | And | When | And | Then | And | And |
|-------|-----|------|-----|------|-----|-----|
| Estou autenticado como aluno | Estou matriculado em um curso que possui uma missão disponível | Clico no curso | Clico na missão e respondo o desafio | O sistema informa se a resposta está correta | Recebo o XP do desafio quando acerto | Meu XP total do curso é atualizado |
| Estou autenticado como professor | Sou responsável por um curso | Clico no curso e acesso a aba de missões | Preencho o formulário de nova missão com título e seus desafios | A missão é criada e listada no curso | A missão fica visível para os alunos matriculados | Cada desafio armazena o seu valor de XP |

### 3.1 BDD por integrante

**Integrante 1 (BDD) — relativo à User Story US01**

| Dado que | E | Quando | E | Então | E | E |
|----------|---|--------|---|-------|---|---|
| Estou autenticado como aluno | Estou matriculado em um curso que possui uma missão disponível | Clico no curso | Clico na missão e respondo o desafio | O sistema informa se a resposta está correta | Recebo o XP do desafio quando acerto | Meu XP total do curso é atualizado |

**Integrante 2 (BDD) — relativo à User Story US02**

| Dado que | E | Quando | E | Então | E |
|----------|---|--------|---|-------|---|
| Estou autenticado como professor | Sou responsável por um curso | Clico no curso e acesso a aba de missões | Preencho o formulário de nova missão com título e seus desafios | A missão é criada e listada no curso | A missão fica visível para os alunos matriculados |

_\* a etapa "E" é opcional._

---

## 4. SPRINT — IMPLEMENTAÇÃO

| SCENARIO | EXECUTION | RESULTS (ASSERTS) |
|----------|-----------|-------------------|
| Aluno resolve um desafio de uma missão e recebe XP | 1. Autenticar como aluno (login)<br>2. Garantir matrícula do aluno em um curso com uma missão publicada (fixture/setup)<br>3. Acessar o curso<br>4. Abrir a missão<br>5. Enviar a resposta do desafio | - Página da missão é exibida com os desafios<br>- `assert(respostaAvaliada === true)` — o sistema retorna se a resposta está correta ou incorreta<br>- `assert(xpGanho === xpDoDesafio)` — em caso de acerto, o XP creditado é o valor configurado no desafio<br>- `assert(xpGanho === 0)` — em caso de erro, nenhum XP é creditado<br>- `assert(xpTotalDepois === xpTotalAntes + xpGanho)` — o XP total do aluno no curso é atualizado |
| Professor cria uma missão com desafios em um curso sob sua responsabilidade | 1. Autenticar como professor (login)<br>2. Garantir que o professor é responsável por um curso (fixture/setup)<br>3. Acessar o curso<br>4. Clicar na aba "Missões" e em "Nova missão"<br>5. Preencher título, descrição e ao menos um desafio com enunciado, alternativas, resposta correta e XP<br>6. Salvar | - `assert(missaoCriada === true)` — a missão é persistida e aparece na listagem do curso<br>- `assert(desafiosSalvos === desafiosInformados)` — todos os desafios enviados são gravados na missão<br>- `assert(xpDoDesafio === xpInformado)` — o valor de XP de cada desafio é o informado pelo professor<br>- `assert(missaoVisivelParaAluno === true)` — aluno matriculado no curso enxerga a missão<br>- `assert(criacaoNegadaParaOutroProfessor === true)` — professor não responsável pelo curso não consegue criar a missão |

---

## 5. RASTREABILIDADE — CENÁRIO → CÓDIGO

| Cenário (seção 4) | Serviço | Classe de teste |
|-------------------|---------|-----------------|
| Aluno resolve um desafio e recebe XP (US01) | `JogoService.responder(...)` | `AlunoResolveDesafioTest` |
| Professor cria uma missão com desafios (US02) | `MissaoService.criar(...)` | `ProfessorCriaMissaoTest` |

**Entidades:** `Usuario` (papel ALUNO/PROFESSOR), `Curso`, `Matricula`, `Missao`, `Desafio`, `Tentativa`, `ProgressoAluno`.

**Regras de negócio implementadas:**
- Só o professor responsável pelo curso cria missões nele.
- Missão exige ao menos um desafio; desafio exige ≥ 2 alternativas, resposta correta válida e XP > 0.
- Só aluno matriculado enxerga as missões e responde os desafios.
- Acerto credita o XP do desafio; erro credita 0.
- O XP de um desafio é creditado **uma única vez** — reacertar não acumula (evita farm de XP).
- Toda resposta vira uma `Tentativa`, alimentando o histórico e o ranking.

**Rodar os testes:** `./mvnw test`

---

## 6. API REST

O usuário autenticado é identificado pelo header `X-Usuario-Id` (substituto do login enquanto não há Spring Security).

| Método | Endpoint | Quem | O que faz |
|--------|----------|------|-----------|
| POST | `/api/cursos/{cursoId}/missoes` | Professor responsável | Cria a missão com seus desafios → `201 Created` |
| GET | `/api/cursos/{cursoId}/missoes` | Aluno matriculado ou professor | Lista as missões do curso |
| POST | `/api/desafios/{desafioId}/respostas` | Aluno matriculado | Envia a resposta e recebe `{correta, xpGanho, xpTotalNoCurso}` |
| GET | `/api/desafios/{desafioId}/tentativas` | Aluno | Histórico das próprias tentativas |
| GET | `/api/cursos/{cursoId}/progresso` | Aluno | XP acumulado no curso |
| GET | `/api/cursos/{cursoId}/ranking` | Turma | Ranking de XP, com posição |

**Códigos de erro:** `400` regra de negócio / header ausente · `403` sem permissão (não matriculado, professor sem vínculo) · `404` recurso inexistente.

O índice da alternativa correta **nunca** é devolvido no JSON — o aluno descobre respondendo.

### Testes da camada web

| Classe | Tipo | Cobre |
|--------|------|-------|
| `MissaoControllerTest` | `@WebMvcTest` (service dublado) | Contrato HTTP/JSON da criação e listagem de missões |
| `JogoControllerTest` | `@WebMvcTest` (service dublado) | Contrato HTTP/JSON de resposta, progresso, histórico e ranking |
| `CenarioBddIntegracaoTest` | `@SpringBootTest` ponta a ponta | O cenário BDD inteiro: HTTP → service → banco |
