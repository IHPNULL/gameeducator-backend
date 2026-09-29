# language: pt
Funcionalidade: Forum do curso
  Como aluno matriculado ou professor responsavel, quero ler, publicar e responder posts no forum do curso.

  Cenario: Listar os posts do forum
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando listo os posts do forum do curso de automacao
    Entao a resposta tem status 200
    E a lista de posts tem ao menos 1 item

  Cenario: Publicar um post com sucesso
    Dado que estou autenticado como o segundo aluno matriculado no curso de automacao
    Quando publico o post "Outra duvida" com conteudo "Alguem entendeu o desafio 2?"
    Entao a resposta tem status 200
    E o post publicado tem o titulo "Outra duvida"

  Cenario: Publicar um post sem titulo falha
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando publico um post sem titulo
    Entao a resposta tem status 400

  Cenario: Usuario inexistente nao pode acessar o forum
    Dado que estou autenticado como um usuario inexistente
    Quando listo os posts do forum do curso de automacao
    Entao a resposta tem status 404

  Cenario: Responder o post inicial com sucesso
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando respondo o post inicial com o conteudo "Sim, e sobre fracoes equivalentes"
    Entao a resposta tem status 200

  Cenario: Responder um post inexistente falha
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando respondo um post inexistente
    Entao a resposta tem status 404
