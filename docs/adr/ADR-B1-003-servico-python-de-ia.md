# ADR-B1-003: Serviço Python independente para IA (ML + RAG juntos por enquanto), com degradação no Core

**Status:** Aceito · **Desafio:** 3 · **Data:** 2026-10

## Contexto

A plataforma passa a ter **recomendação inteligente (RF03)** e **assistente educacional com RAG/LLM (RF04)**.
Os modelos são desenvolvidos em Python, têm ciclo de atualização próprio e carga variável, e **podem ficar
indisponíveis sem impedir as operações essenciais** do Core (responder desafios, XP, ranking).

## ASRs e RPCs

| ASR | Como altera a arquitetura |
|---|---|
| **ASR-06 Tolerância a falhas do fluxo essencial** (FURPS+ *Reliability*) | Muda a **comunicação**: a chamada à IA precisa de timeout, disjuntor e um comportamento degradado definido. A IA nunca pode estar no caminho de escrita essencial. |
| **ASR-07 Deploy independente** (FURPS+ *Supportability*) | Muda a **implantação**: o modelo muda em outro ritmo, com outra stack e outro pipeline. |
| **ASR-08 Escalabilidade de carga variável** (FURPS+ *Performance*) | Muda a **implantação/operação**: picos do assistente (LLM) não podem disputar CPU/memória com o Core. |

Não selecionados como ASR: *interoperabilidade*, que HTTP/JSON resolve e por isso não diferencia as alternativas;
*desempenho*, que é limitado pelo timeout e pelo fallback (é consequência do ASR-06); e *manutenibilidade*, que é
consequência do ASR-07.

RPCs: Core permanece Java/Spring Boot (RPC-01); ML/RAG em Python (RPC-05); indisponibilidade da IA não pode
bloquear o fluxo essencial (RPC-06).

## Respostas do Tech Lead

- **Python por si só justifica um serviço?** Não. Seria possível embutir Python (Jython/GraalPy, ou um processo
  filho). O que justifica a fronteira de rede são os ASR-07 e ASR-08 (ciclo e escala próprios) somados à RPC-06.
- **ML e RAG precisam da mesma fronteira?** Hoje **não precisam de fronteiras diferentes**: mesma equipe, mesma
  stack, volume baixo e nenhum dos dois guarda estado. Ficam no mesmo deployável, em módulos separados
  (`app/recomendacao`, `app/assistente`). **Gatilho de separação:** o RAG passar a usar LLM com GPU/custo por
  token, ou ter um p95 muito diferente do ML, ou exigir escala própria. A separação seria barata porque os
  módulos já não compartilham código e o Core trata as duas operações separadamente (métricas por `operacao`).
- **Comportamento degradado aceitável:**
  - Recomendação: o Core responde **200 com `origem: "fallback"`**, com as próximas missões não concluídas na ordem
    do curso (regra local). Piora a qualidade, mas nunca quebra a tela.
  - Assistente: **503 com mensagem clara**. Não há resposta "inventada" pelo Core, e todo o resto continua funcionando.

## Alternativas

| Alternativa | ASR-06 (falha isolada) | ASR-07 (deploy próprio) | ASR-08 (escala própria) | Custo | Veredito |
|---|---|---|---|---|---|
| (A) IA embutida no Core (Java/ONNX ou GraalPy) | Falha/latência do modelo derruba ou trava o Core | Modelo novo = deploy do Core | Escala junto com o Core | Baixo na rede, alto em acoplamento | Rejeitada |
| (B) Módulo acoplado (biblioteca Python chamada via processo no mesmo container) | Parcial: mesmo host, mesmos recursos | Parcial: mesma imagem | Não | Médio | Rejeitada |
| **(C) Serviço Python independente (FastAPI, HTTP/JSON)** | **Timeout + disjuntor + fallback no Core** | **Imagem e pipeline próprios** | **Réplicas próprias** | Rede, contrato, mais um container | **Escolhida** |

**Comunicação síncrona (HTTP/JSON) e não por mensageria:** recomendação e resposta do assistente são consultas que o
aluno espera na tela (request/response). Uma fila adicionaria latência e complexidade sem ganho. A resiliência vem
de timeout e fallback, não de buffer.

## Decisão

- Novo repositório **`gameeducator-ia`** (Python 3.12, FastAPI). O serviço é **stateless**: não lê o banco do
  Core, que envia o contexto em cada chamada (progresso por missão e material do curso). Isso evita acoplamento
  de dados e permite trocar o modelo sem migração.
- No Core, a porta `ServicoDeIa` é implementada por `ClienteHttpIa` (RestClient com **timeout de conexão de 300 ms
  e de leitura de 2 s**) e decorada por `ServicoDeIaProtegido`, que tem **disjuntor** (abre após 3 falhas seguidas e
  testa de novo após 15 s) e métricas.
- O contexto é lido do banco em uma transação curta, e a chamada HTTP acontece **fora** dela, para não prender
  conexão do pool enquanto espera a IA.
- O LLM é plugável: provedor `extrativo` (padrão, determinístico, sem custo) ou `ollama` (local). Se o LLM falhar, o
  serviço cai para o extrativo, o que dá uma segunda camada de degradação.

## Consequências

- **Mais fácil:** atualizar o modelo (`modeloVersao` no contrato) sem tocar no Core; escalar a IA isoladamente;
  falhas de IA viram uma métrica (`gameeducator_ia_chamadas_total{resultado}`) em vez de incidente.
- **Mais difícil:** há um contrato entre repositórios, coberto por `ClienteHttpIaTest` no Core e pelo smoke no
  `Jenkinsfile` da IA; mais um container para operar; e o contexto trafega a cada chamada (aceitável no volume atual).
- **Risco aceito:** o disjuntor é próprio (≈ 40 linhas) e não uma biblioteca. Com mais integrações, deve ser trocado
  por Resilience4j.

## Evidência

`docs/arquitetura/evidencias/desafio-3/`: Spring Boot consumindo o FastAPI, com o serviço Python interrompido
e lento, e a resposta e a latência observadas em cada caso.
