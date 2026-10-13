"""Desafio 1 - compara as dependencias entre classes do Core antes/depois da modularizacao.

Entrada: saidas do jdeps (-verbose:class) das classes compiladas do Core, uma por estado.
    jdeps -verbose:class -e 'org\\.facens\\..*' target/classes > jdeps-depois.txt
Uso:
    python3 analisar_dependencias.py jdeps-antes.txt jdeps-depois.txt > dependencias-antes-depois.md

Metricas (so dependencias entre classes do proprio Core):
  - fan-out do JogoService (quantas classes do Core ele conhece);
  - quem, fora do modulo de gamificacao, conhece os internos da gamificacao (entidades, repositorios,
    regras) - o "raio de impacto" de mudar uma regra ou a persistencia de XP/medalhas;
  - arestas entre pacotes de primeiro nivel.
"""
import re
import sys
from collections import defaultdict

BASE = "org.facens.grupo_6_gameeducator."
LINHA = re.compile(r"^\s+(\S+)\s+->\s+(\S+)")

# Conceitos de gamificacao (XP, medalhas, ranking) nos dois estados
INTERNOS_ANTES = {
    "domain.ProgressoAluno", "domain.Medalha",
    "repository.ProgressoAlunoRepository", "repository.MedalhaRepository",
}
API_DEPOIS = re.compile(r"^gamificacao\.[A-Z]")  # tipos do pacote raiz = API publica


def ler(caminho):
    arestas = set()
    for linha in open(caminho, encoding="utf-8"):
        m = LINHA.match(linha)
        if not m:
            continue
        origem, destino = m.group(1), m.group(2)
        if origem.startswith(BASE) and destino.startswith(BASE):
            origem = origem[len(BASE):].split("$")[0]
            destino = destino[len(BASE):].split("$")[0]
            if origem != destino:
                arestas.add((origem, destino))
    return arestas


def interno_da_gamificacao(classe, estado):
    if estado == "antes":
        return classe in INTERNOS_ANTES
    return classe.startswith("gamificacao.") and not API_DEPOIS.match(classe)


def dentro_do_modulo(classe, estado):
    return estado == "depois" and classe.startswith("gamificacao.")


def pacote(classe):
    return classe.split(".")[0]


def analisar(arestas, estado):
    fan_out_jogo = sorted(d for o, d in arestas if o == "service.JogoService")
    conhecem_internos = defaultdict(set)
    for o, d in arestas:
        if interno_da_gamificacao(d, estado) and not interno_da_gamificacao(o, estado) and not dentro_do_modulo(o, estado):
            conhecem_internos[o].add(d)
    pacotes = defaultdict(int)
    for o, d in arestas:
        if pacote(o) != pacote(d):
            pacotes[(pacote(o), pacote(d))] += 1
    return fan_out_jogo, conhecem_internos, pacotes


def main(antes, depois):
    a = analisar(ler(antes), "antes")
    d = analisar(ler(depois), "depois")
    print("# Desafio 1 - dependencias antes x depois da modularizacao da gamificacao\n")
    print("Gerado por `labs/desafio-1/analisar_dependencias.py` a partir do `jdeps` das classes compiladas.\n")
    print("| Metrica | Antes (AC1, camadas) | Depois (modulo gamificacao) |")
    print("|---|---|---|")
    print(f"| Classes do Core que o `JogoService` conhece (fan-out) | {len(a[0])} | {len(d[0])} |")
    print(f"| Classes FORA da gamificacao que conhecem internos dela (entidades/repositorios/regras) | {len(a[1])} | {len(d[1])} |")
    print(f"| Dependencias de fora para internos da gamificacao (arestas) | {sum(map(len, a[1].values()))} | {sum(map(len, d[1].values()))} |")
    print()
    for titulo, dados in (("Antes", a), ("Depois", d)):
        print(f"## {titulo}\n")
        print("**Fan-out do `JogoService`:** " + ", ".join(f"`{c}`" for c in dados[0]) + "\n")
        print("**Quem conhece internos da gamificacao:**\n")
        if not dados[1]:
            print("- ninguem fora do modulo (somente `GamificacaoApi` e seus records sao usados)\n")
        for origem, destinos in sorted(dados[1].items()):
            print(f"- `{origem}` -> " + ", ".join(f"`{x}`" for x in sorted(destinos)))
        print()
        print("**Arestas entre pacotes de primeiro nivel (qtde de dependencias entre classes):**\n")
        print("| De | Para | Qtde |\n|---|---|---|")
        for (o, dd), n in sorted(dados[2].items()):
            print(f"| {o} | {dd} | {n} |")
        print()


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
