# language: pt
Funcionalidade: Missoes do curso
  Como professor, quero criar missoes com desafios no meu curso.
  Como aluno, quero ver as missoes do curso em que estou matriculado.

  Cenario: Professor cria uma missao com sucesso
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando crio uma missao valida chamada "Missao de Fracoes"
    Entao a resposta tem status 201
    E a missao criada tem o titulo "Missao de Fracoes"

  Cenario: Criacao de missao falha com dados invalidos
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando tento criar uma missao sem titulo
    Entao a resposta tem status 400

  Cenario: Aluno nao pode criar missao
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando crio uma missao valida chamada "Missao Proibida"
    Entao a resposta tem status 403

  Cenario: Aluno matriculado lista as missoes do curso
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando listo as missoes do curso de automacao
    Entao a resposta tem status 200
    E a lista de missoes tem ao menos 1 item

  Cenario: Listar missoes de um curso inexistente
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando listo as missoes de um curso inexistente
    Entao a resposta tem status 404
