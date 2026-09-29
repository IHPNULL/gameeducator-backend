# language: pt
Funcionalidade: Painel do professor - desempenho da turma
  Como professor responsavel, quero acompanhar o desempenho da turma no curso.

  Cenario: Professor consulta o desempenho da turma
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando consulto o desempenho da turma do curso de automacao
    Entao a resposta tem status 200
    E a lista de desempenho tem ao menos 2 itens

  Cenario: Aluno nao pode consultar o desempenho da turma
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando consulto o desempenho da turma do curso de automacao
    Entao a resposta tem status 403

  Cenario: Consultar o desempenho da turma de um curso inexistente
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando consulto o desempenho da turma de um curso inexistente
    Entao a resposta tem status 404
