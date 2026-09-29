# language: pt
Funcionalidade: Notas do curso
  Como professor, quero lancar as notas dos alunos matriculados.
  Como aluno, quero ver as minhas proprias notas.

  Cenario: Professor lanca uma nota com sucesso
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando lanco a nota "Prova 2" com valor 9.0 para o primeiro aluno
    Entao a resposta tem status 200
    E a nota lancada tem o valor 9.0

  Cenario: Lancar nota com valor fora da faixa permitida falha
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando lanco a nota "Prova 3" com valor 15 para o primeiro aluno
    Entao a resposta tem status 400

  Cenario: Aluno nao pode lancar notas
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando lanco a nota "Prova 2" com valor 9.0 para o primeiro aluno
    Entao a resposta tem status 403

  Cenario: Lancar nota em um curso inexistente falha
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando lanco uma nota para o primeiro aluno em um curso inexistente
    Entao a resposta tem status 404

  Cenario: Aluno matriculado consulta as proprias notas
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando consulto minhas notas no curso de automacao
    Entao a resposta tem status 200
    E a lista de notas tem ao menos 1 item

  Cenario: Usuario sem matricula nao pode consultar notas do curso
    Dado que estou autenticado como um usuario inexistente
    Quando consulto minhas notas no curso de automacao
    Entao a resposta tem status 403
