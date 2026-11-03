# Desafio 1 - benchmark do ranking (PostgreSQL 16, 100.000 usuarios)

Gerado por `labs/desafio-1/benchmark-ranking.sh` em 2026-11-03 20:06. Mediana de 7 execucoes (EXPLAIN ANALYZE, ms).

Massa: 318996 linhas de progresso / 200 cursos.
Curso 1 = pior caso (20.000 alunos). Curso 77 = curso tipico (~1.500 alunos).

| Consulta | Sem indice (ms) | Com ix_progresso_curso_xp (ms) |
|---|---|---|
| Ranking completo, curso tipico | 13.284 | 8.843 |
| Ranking completo, curso de 20k alunos (consulta atual da API) | 21.344 | 13.289 |
| Top-50, curso de 20k alunos | 7.996 | 0.237 |

Plano do top-50 sem indice:

    Limit  (cost=6492.02..6522.30 rows=50 width=23)
      ->  Nested Loop  (cost=6492.02..18668.51 rows=20107 width=23)
            ->  Gather Merge  (cost=6491.73..8783.34 rows=20107 width=12)

Plano do top-50 com indice:

    Limit  (cost=0.71..50.25 rows=50 width=23)
      ->  Nested Loop  (cost=0.71..19501.14 rows=19682 width=23)
            ->  Index Scan using ix_progresso_curso_xp on progresso_aluno p  (cost=0.42..9747.72 rows=19682 width=12)
