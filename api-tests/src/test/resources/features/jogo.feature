# language: pt
Funcionalidade: Jogo - responder desafios, progresso e ranking
  Como aluno, quero responder os desafios de uma missao e ver meu progresso e o ranking da turma.

  Cenario: Aluno responde corretamente a um desafio
    Dado que estou autenticado como o segundo aluno matriculado no curso de automacao
    Quando respondo o desafio de soma com a alternativa correta
    Entao a resposta tem status 200
    E a resposta indica que a alternativa esta correta

  Cenario: Aluno responde incorretamente a um desafio
    Dado que estou autenticado como o segundo aluno matriculado no curso de automacao
    Quando respondo o desafio da capital com uma alternativa errada
    Entao a resposta tem status 200
    E a resposta indica que a alternativa esta incorreta

  Cenario: Responder com indice de alternativa inexistente falha
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando respondo o desafio de soma com o indice de alternativa 99
    Entao a resposta tem status 400

  Cenario: Responder um desafio inexistente falha
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando respondo um desafio inexistente com a alternativa correta
    Entao a resposta tem status 404

  Cenario: Professor nao pode responder desafios
    Dado que estou autenticado como o professor responsavel pelo curso de automacao
    Quando respondo o desafio de soma com a alternativa correta
    Entao a resposta tem status 403

  Cenario: Consultar o ranking do curso
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando consulto o ranking do curso de automacao
    Entao a resposta tem status 200
    E a lista de ranking tem ao menos 1 item

  Cenario: Consultar o progresso do proprio aluno
    Dado que estou autenticado como um aluno matriculado no curso de automacao
    Quando consulto meu progresso no curso de automacao
    Entao a resposta tem status 200
