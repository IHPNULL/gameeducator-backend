# ADR-B1-001: Evoluir o Core para Monólito Modular (sem decompor em serviços)

**Status:** Aceito · **Desafio:** 1 · **Data:** 2026-10 · **Substitui parcialmente:** ADR-002 da AC1 (camadas puras)

## Contexto

O MVP da AC1 é um monólito em camadas (`web → service → repository → domain`). O cenário B1 projeta
crescimento de 1.000 para até 100.000 usuários, com o **ranking (RF01)** crescendo mais que o resto e as
**conquistas (RF02)** e demais funcionalidades continuando a evoluir. A pergunta é se o crescimento exige
escala ou deploy independentes.

No código da AC1, as regras de XP, medalhas e ranking estavam dentro do `JogoService`, que conhecia
diretamente as entidades e os repositórios `ProgressoAluno` e `Medalha`. Os controllers e DTOs web também
liam essas entidades. Na prática, qualquer mudança de regra ou de persistência espalhava efeito por 4 classes
de outras camadas.

## ASRs e RPCs

| ASR | Como altera a arquitetura |
|---|---|
| **ASR-01 Manutenibilidade/modificabilidade** (FURPS+ *Supportability*) | Exige **estrutura** com fronteiras explícitas por capacidade, para que 6 devs evoluam conquistas e cursos sem pisar no mesmo código. |
| **ASR-02 Desempenho/escalabilidade do ranking** (FURPS+ *Performance*) | Pode mudar **dados** (índice, consulta top-N) e **implantação** (réplicas). É o único ASR que poderia justificar um serviço separado. |
| **ASR-03 Complexidade operacional** (FURPS+ *Supportability*, restrição de operação) | Limita o número de unidades de **implantação/operação** que a equipe consegue sustentar. |

RPCs: Core em Java 21/Spring Boot (RPC-01), equipe de 6 desenvolvedores (RPC-02), banco relacional e legado da AC1 (RPC-03).

## Alternativas

| Alternativa | Atende ASR-01? | Atende ASR-02? | ASR-03 (custo operacional) | Veredito |
|---|---|---|---|---|
| (A) Manter monólito em camadas | Não: camadas horizontais não isolam capacidades; mudança de regra atravessa web/service/repository | Sim (o gargalo é a consulta, não o processo) | Mínimo | Rejeitada: não resolve o problema que mais muda |
| **(B) Monólito modular** | **Sim**: módulo `gamificacao` com API pública e fronteira verificada no build | **Sim**: índice + top-N + réplicas stateless do mesmo artefato | **Baixo**: 1 deployável, 1 banco, 1 pipeline | **Escolhida** |
| (C) Serviço de ranking separado | Sim | Sim, mas resolve um problema que não foi medido | Alto: rede, consistência entre bancos, 2º pipeline, tracing | Rejeitada agora (sem evidência de necessidade) |

## Decisão

Adotar **Monólito Modular**: o Core continua sendo um único deployável Spring Boot, organizado por módulos
de negócio com API pública e internos protegidos. O primeiro módulo extraído é `gamificacao` (XP, medalhas e
ranking), que é a parte de maior volatilidade e de maior crescimento de leitura.

- Fora do módulo, só `GamificacaoApi` e os records do pacote raiz são visíveis.
- Fronteiras verificadas por **ArchUnit** no `mvn verify` (`ArquiteturaModularTest`).
- O ranking escala **dentro** do monólito: índice `ix_progresso_curso_xp (curso_id, xp_total DESC)` e o
  Core continua stateless (identidade por header), então pode rodar em N réplicas atrás de um balanceador.

**Gatilho para revisar (decompor o ranking):** p95 do `GET /ranking` acima de 300 ms com índice e top-N, ou
a necessidade de um armazenamento especializado (ex.: sorted sets em Redis) que o banco relacional não atenda.

## Consequências

- **Mais fácil:** mudar regra de gamificação toca um módulo; a fronteira não "apodrece" porque o build quebra;
  um deploy, um banco, transações locais.
- **Mais difícil:** o módulo ainda compartilha o banco e as entidades `Usuario`/`Curso` (*shared kernel*
  aceito). Escala é do Core inteiro, não só do ranking.
- **Trade-off aceito:** não ter escala independente do ranking em troca de não pagar complexidade distribuída
  sem evidência.
- **Pendência para a AF:** a API de ranking devolve a turma inteira. Com 20 mil alunos em um curso, o próximo
  passo é paginar ou limitar ao top-N, que custa 0,2 ms com o índice.

## Evidência

- `docs/arquitetura/evidencias/desafio-1/dependencias-antes-depois.md`: dependências medidas com `jdeps`.
- `docs/arquitetura/evidencias/desafio-1/fitness-function.txt`: violação proposital quebra o build.
- `docs/arquitetura/evidencias/desafio-1/benchmark-ranking.md`: ranking com 100 mil usuários no PostgreSQL 16.
