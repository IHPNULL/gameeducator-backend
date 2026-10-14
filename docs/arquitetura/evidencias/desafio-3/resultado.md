# Desafio 3 - Core (Spring Boot) consumindo a IA (FastAPI) com falhas provocadas

Gerado por `labs/desafio-3/executar.sh` em 2026-10-14 18:13. Aluno Bruno (id 2), curso 1.
Protecao no Core: timeout conexao 300 ms / leitura 2 s; disjuntor abre apos 3 falhas e testa de novo apos 15 s.

| Cenario | Chamada ao Core | HTTP | Tempo | Observado |
|---|---|---|---|---|
| 1. IA no ar | `GET /recomendacoes` | 200 | 96 ms | origem=ia, modelo=baseline-0.1, itens=1 |
| 1. IA no ar | `POST /assistente` | 200 | 29 ms | provedor=extrativo, fontes=['Missao 1 - Fracoes'] |
| 2. IA lenta (3 s) | `GET /recomendacoes` | 200 | 2019 ms | origem=fallback (timeout de 2 s do Core) |
| 2. IA volta ao normal | `GET /recomendacoes` | 200 | 20 ms | origem=ia |
| 3. IA parada (chamada 1) | `GET /recomendacoes` | 200 | 313 ms | origem=fallback, itens=1 |
| 3. IA parada (chamada 2) | `GET /recomendacoes` | 200 | 322 ms | origem=fallback, itens=1 |
| 3. IA parada (chamada 3) | `GET /recomendacoes` | 200 | 313 ms | origem=fallback, itens=1 |
| 3. IA parada (chamada 4) | `GET /recomendacoes` | 200 | 14 ms | origem=fallback, itens=1 |
| 3. IA parada (chamada 5) | `GET /recomendacoes` | 200 | 14 ms | origem=fallback, itens=1 |
| 3. IA parada | `POST /assistente` | 503 | 20 ms | Assistente educacional indisponivel no momento. O restante da plataforma segue funcionando. |
| 3. IA parada - fluxo essencial | `POST /desafios/1/respostas` | 200 | 24 ms | correta=True, xpTotal=10 |
| 3. IA parada - fluxo essencial | `GET /ranking` | 200 | 12 ms | alunos no ranking=2 |
| 4. IA de volta (apos 15 s) | `GET /recomendacoes` | 200 | 25 ms | origem=ia |

Metricas do Core logo apos o cenario 3 (`/actuator/prometheus`):

```
gameeducator_ia_chamadas_total{application="gameeducator-core",operacao="assistente",resultado="disjuntor_aberto"} 1.0
gameeducator_ia_chamadas_total{application="gameeducator-core",operacao="assistente",resultado="sucesso"} 1.0
gameeducator_ia_chamadas_total{application="gameeducator-core",operacao="recomendacao",resultado="disjuntor_aberto"} 2.0
gameeducator_ia_chamadas_total{application="gameeducator-core",operacao="recomendacao",resultado="falha"} 4.0
gameeducator_ia_chamadas_total{application="gameeducator-core",operacao="recomendacao",resultado="sucesso"} 2.0
gameeducator_ia_disjuntor_aberto{application="gameeducator-core"} 1.0
```
