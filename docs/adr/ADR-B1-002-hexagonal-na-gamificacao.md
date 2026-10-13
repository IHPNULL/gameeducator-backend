# ADR-B1-002: Ports & Adapters (Hexagonal) só no módulo de gamificação, com DDD tático leve

**Status:** Aceito · **Desafio:** 2 · **Data:** 2026-10 · **Complementa:** ADR-B1-001

## Contexto

As regras de ranking, XP, medalhas e recompensas mudam com frequência. Na AC1 elas estavam em
`JogoService`, acopladas a Spring (`@Service`, `@Transactional`), JPA (entidades e repositórios) e às
constantes `MARCOS_XP = [50, 100, 250, 500]`. Para testar uma regra de medalha era preciso subir o contexto
Spring com banco H2. Mudar os marcos exigia mudar código.

## ASRs e RPCs

| ASR | Como altera a arquitetura |
|---|---|
| **ASR-04 Modificabilidade das regras de gamificação** (FURPS+ *Supportability*) | Muda a **estrutura** interna do Core: as regras precisam ficar em código sem dependências técnicas e parametrizáveis. |
| **ASR-05 Testabilidade do domínio** (FURPS+ *Supportability*) | Exige **inverter a dependência** da persistência: o domínio define portas e a infraestrutura as implementa. Assim as regras são testadas sem Spring e sem banco. |

Os demais RNFs (manutenibilidade e facilidade de evolução) são consequência desses dois e não acrescentam decisão nova.

RPCs: Core continua Spring Boot (RPC-01); regras existentes devem ser reaproveitadas (RPC-04); proibido
reescrever o produto inteiro para aplicar um padrão (RPC-04).

## Respostas do Tech Lead

- **Qual dependência precisa ser invertida?** Só *regra de gamificação → persistência (JPA/Spring Data)*.
  Web → service continua como estava. Controllers dependerem de Spring não é problema.
- **O custo é proporcional?** Sim, porque está restrito a **um** módulo: 3 classes de domínio, 2 portas, 2 adaptadores
  e 1 configuração. Fórum, notas e missões continuam em camadas, porque mudam pouco.
- **DDD, Hexagonal, Clean e Onion concorrem?** Não. Hexagonal, Clean e Onion são a mesma ideia (dependências
  apontando para o domínio) com vocabulários diferentes. Usamos o vocabulário de *Ports & Adapters*, e do DDD
  só a parte tática necessária: objeto de valor `Progresso`, políticas de domínio e linguagem ubíqua em português.
  Não criamos agregados, eventos de domínio dentro do hexágono nem *bounded contexts* formais.

## Alternativas

| Alternativa | ASR-04 | ASR-05 | Custo | Veredito |
|---|---|---|---|---|
| Manter camadas (regra no service com JPA) | Marcos fixos em código; regra misturada com persistência | Teste exige Spring + H2 | Zero | Rejeitada |
| Extrair regras para classes puras, sem portas | Melhora | Regras testáveis, mas o caso de uso continua preso ao JPA | Baixo | Insuficiente: o caso de uso não troca adaptador |
| **Ports & Adapters no módulo gamificação** | **Regras puras + marcos por configuração** | **Domínio e caso de uso testados sem Spring; mesmo contrato contra JPA e memória** | **Médio, localizado** | **Escolhida** |
| Clean Architecture em todo o Core | Sim | Sim | Alto: reescreve fórum/notas/missões sem ganho | Rejeitada (viola RPC-04) |

## Decisão

```
gamificacao/
├── GamificacaoApi, RespostaPontuavel, ResultadoPontuacao, ...   API pública (porta de entrada)
├── dominio/      Progresso, PoliticaDeXp, PoliticaDeMedalhas    Java puro
├── porta/        RepositorioDeProgresso, RepositorioDeMedalhas  portas de saída
├── aplicacao/    GamificacaoService                             caso de uso (sem Spring/JPA)
├── adaptador/jpa/      ProgressoJpaAdapter, MedalhaJpaAdapter   (tabelas da AC1 inalteradas)
├── adaptador/memoria/  GamificacaoEmMemoria                     testes e app.gamificacao.persistencia=memoria
└── config/       GamificacaoConfig                              único lugar que conhece as implementações
```

- Marcos de medalha viram propriedade: `app.gamificacao.marcos-xp=50,100,250,500`.
- Adaptador escolhido por propriedade: `app.gamificacao.persistencia=jpa|memoria`.
- Regras de dependência verificadas por ArchUnit: o domínio só depende de `java.*`, e a aplicação não conhece
  adaptadores, Spring nem JPA.

## Consequências

- **Mais fácil:** testar e mudar regra; trocar persistência (cache, outro banco) sem tocar no caso de uso.
  O schema da AC1 não mudou: nenhuma migração.
- **Mais difícil:** há mapeamento domínio ↔ entidade JPA (código a mais) e dois estilos convivendo no Core (camadas
  e hexagonal). Esse é um custo cognitivo aceito e documentado.
- **Transações:** o caso de uso não abre transação; ela pertence ao chamador (`JogoService.responder`). Isso é
  explícito no código e nos testes.

## Evidência

`docs/arquitetura/evidencias/desafio-2/troca-de-adaptador.md`. O mesmo contrato
(`ContratoRepositoriosDeGamificacao`) passa contra o adaptador JPA e contra o em memória. As regras e o caso de
uso rodam sem Spring (≈ 2 ms), e a aplicação sobe com o adaptador em memória trocando só uma propriedade.
