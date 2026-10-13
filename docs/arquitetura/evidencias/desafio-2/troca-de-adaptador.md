# Desafio 2 - mesma regra, adaptadores diferentes

Gerado por `labs/desafio-2/relatorio-adaptadores.sh` em 2026-10-12 21:36.

| Classe de teste | O que prova | Adaptador | Spring? | Testes | Falhas | Tempo (s) |
|---|---|---|---|---|---|---|
| `RegrasDeGamificacaoTest` | regras de XP/medalhas isoladas | nenhum | nao | 5 | 0 | 0.003 |
| `GamificacaoServiceTest` | caso de uso completo | memoria | nao | 3 | 0 | 0.002 |
| `RepositoriosEmMemoriaTest` | contrato das portas | memoria | nao | 4 | 0 | 0.002 |
| `RepositoriosJpaTest` | MESMO contrato das portas | JPA + H2 | sim | 4 | 0 | 0.030 |
| `TrocaDeAdaptadorNoContextoTest` | app sobe com outro adaptador so por propriedade | memoria | sim | 1 | 0 | 0.184 |
| `AlunoGanhaMedalhaTest` | cenarios BDD da AC1 inalterados | JPA + H2 | sim | 4 | 0 | 2.109 |
| `ArquiteturaModularTest` | dominio sem Spring/JPA; aplicacao sem adaptador | - | nao | 6 | 0 | 0.491 |
