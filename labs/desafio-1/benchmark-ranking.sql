-- Desafio 1 - o ranking (RF01) cresce mais que o resto: o monolito aguenta 100.000 usuarios?
-- Massa: 100.000 alunos, 200 cursos, cada aluno em 3 cursos (300.000 linhas de progresso).
-- O curso 1 e o "curso viral" com 20.000 alunos (pior caso de ranking).
\timing off
DROP TABLE IF EXISTS progresso_aluno, usuario;
CREATE TABLE usuario (id bigint PRIMARY KEY, nome varchar(255) NOT NULL);
CREATE TABLE progresso_aluno (
    id bigserial PRIMARY KEY,
    aluno_id bigint NOT NULL REFERENCES usuario(id),
    curso_id bigint NOT NULL,
    xp_total int NOT NULL,
    CONSTRAINT uk_progresso_aluno_curso UNIQUE (aluno_id, curso_id)
);
INSERT INTO usuario SELECT g, 'Aluno ' || g FROM generate_series(1, 100000) g;
INSERT INTO progresso_aluno (aluno_id, curso_id, xp_total)
SELECT a, c, (random() * 600)::int
FROM generate_series(1, 100000) a,
     LATERAL (SELECT DISTINCT 2 + ((a::bigint * k * 7919) % 199) AS c FROM generate_series(1, 3) k) cursos;
-- curso viral: 20.000 alunos no curso 1
INSERT INTO progresso_aluno (aluno_id, curso_id, xp_total)
SELECT a, 1, (random() * 600)::int FROM generate_series(1, 20000) a;
ANALYZE;
SELECT count(*) AS linhas_progresso, count(DISTINCT curso_id) AS cursos FROM progresso_aluno;
