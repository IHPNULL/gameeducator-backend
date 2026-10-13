# Desafio 1 - benchmark do ranking (PostgreSQL 16, 100.000 usuarios)

Gerado por `labs/desafio-1/benchmark-ranking.sh` em 2026-10-12 21:36. Mediana de 7 execucoes (EXPLAIN ANALYZE, ms).

Massa: 318996 linhas de progresso / 200 cursos.
Curso 1 = pior caso (20.000 alunos). Curso 77 = curso tipico (~1.500 alunos).

| Consulta | Sem indice (ms) | Com ix_progresso_curso_xp (ms) |
|---|---|---|
| Ranking completo, curso tipico | 12.383 | 8.379 |
| Ranking completo, curso de 20k alunos (consulta atual da API) | 20.185 | 12.828 |
| Top-50, curso de 20k alunos | 7.425 | 0.223 |

Plano do top-50 sem indice:

    Limit  (cost=6461.15..6491.76 rows=50 width=23)
      ->  Nested Loop  (cost=6461.15..18340.43 rows=19406 width=23)
            ->  Gather Merge  (cost=6460.86..8672.57 rows=19406 width=12)

Plano do top-50 com indice:

    Limit  (cost=0.71..49.75 rows=50 width=23)
      ->  Nested Loop  (cost=0.71..19593.12 rows=19980 width=23)
            ->  Index Scan using ix_progresso_curso_xp on progresso_aluno p  (cost=0.42..9747.32 rows=19980 width=12)
