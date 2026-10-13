# Desafio 1 - dependencias antes x depois da modularizacao da gamificacao

Gerado por `labs/desafio-1/analisar_dependencias.py` a partir do `jdeps` das classes compiladas.

| Metrica | Antes (AC1, camadas) | Depois (modulo gamificacao) |
|---|---|---|
| Classes do Core que o `JogoService` conhece (fan-out) | 20 | 19 |
| Classes FORA da gamificacao que conhecem internos dela (entidades/repositorios/regras) | 4 | 0 |
| Dependencias de fora para internos da gamificacao (arestas) | 8 | 0 |

## Antes

**Fan-out do `JogoService`:** `domain.Curso`, `domain.Desafio`, `domain.Matricula`, `domain.Medalha`, `domain.Missao`, `domain.ProgressoAluno`, `domain.Tentativa`, `domain.Usuario`, `exception.AcessoNegadoException`, `exception.RecursoNaoEncontradoException`, `exception.RegraDeNegocioException`, `repository.CursoRepository`, `repository.DesafioRepository`, `repository.MatriculaRepository`, `repository.MedalhaRepository`, `repository.ProgressoAlunoRepository`, `repository.TentativaRepository`, `repository.UsuarioRepository`, `service.dto.DesempenhoAluno`, `service.dto.ResultadoResposta`

**Quem conhece internos da gamificacao:**

- `service.JogoService` -> `domain.Medalha`, `domain.ProgressoAluno`, `repository.MedalhaRepository`, `repository.ProgressoAlunoRepository`
- `web.JogoController` -> `domain.Medalha`, `domain.ProgressoAluno`
- `web.dto.MedalhaResponse` -> `domain.Medalha`
- `web.dto.RankingItemResponse` -> `domain.ProgressoAluno`

**Arestas entre pacotes de primeiro nivel (qtde de dependencias entre classes):**

| De | Para | Qtde |
|---|---|---|
| config | domain | 14 |
| config | repository | 6 |
| config | service | 12 |
| repository | domain | 11 |
| service | domain | 20 |
| service | exception | 12 |
| service | repository | 20 |
| web | domain | 21 |
| web | exception | 3 |
| web | service | 9 |

## Depois

**Fan-out do `JogoService`:** `domain.Curso`, `domain.Desafio`, `domain.Matricula`, `domain.Missao`, `domain.Tentativa`, `domain.Usuario`, `exception.AcessoNegadoException`, `exception.RecursoNaoEncontradoException`, `exception.RegraDeNegocioException`, `gamificacao.GamificacaoApi`, `gamificacao.RespostaPontuavel`, `gamificacao.ResultadoPontuacao`, `repository.CursoRepository`, `repository.DesafioRepository`, `repository.MatriculaRepository`, `repository.TentativaRepository`, `repository.UsuarioRepository`, `service.dto.DesempenhoAluno`, `service.dto.ResultadoResposta`

**Quem conhece internos da gamificacao:**

- ninguem fora do modulo (somente `GamificacaoApi` e seus records sao usados)


**Arestas entre pacotes de primeiro nivel (qtde de dependencias entre classes):**

| De | Para | Qtde |
|---|---|---|
| config | domain | 14 |
| config | repository | 6 |
| config | service | 12 |
| gamificacao | domain | 8 |
| repository | domain | 9 |
| service | domain | 18 |
| service | exception | 12 |
| service | gamificacao | 3 |
| service | repository | 18 |
| web | domain | 16 |
| web | exception | 3 |
| web | gamificacao | 5 |
| web | service | 9 |

