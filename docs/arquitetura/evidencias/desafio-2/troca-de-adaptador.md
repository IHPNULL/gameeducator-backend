# Desafio 2 - mesma regra, adaptadores diferentes

Gerado por `labs/desafio-2/relatorio-adaptadores.sh` em 2026-11-03 20:04.

| Classe de teste | O que prova | Adaptador | Spring? | Testes | Falhas | Tempo (s) |
|---|---|---|---|---|---|---|
| `RegrasDeGamificacaoTest` | regras de XP/medalhas isoladas | nenhum | nao | 5 | 0 | 0.003 |
| `GamificacaoServiceTest` | caso de uso completo | memoria | nao | 3 | 0 | 0.001 |
| `RepositoriosEmMemoriaTest` | contrato das portas | memoria | nao | 4 | 0 | 0.002 |
| `RepositoriosJpaTest` | MESMO contrato das portas | JPA + H2 | sim | 4 | 0 | 0.036 |
| `TrocaDeAdaptadorNoContextoTest` | app sobe com outro adaptador so por propriedade | memoria | sim | 1 | 0 | 0.272 |
| `AlunoGanhaMedalhaTest` | cenarios BDD da AC1 inalterados | JPA + H2 | sim | 4 | 0 | 2.431 |
| `ArquiteturaModularTest` | dominio sem Spring/JPA; aplicacao sem adaptador | - | nao | 9 | 0 | 0.581 |
