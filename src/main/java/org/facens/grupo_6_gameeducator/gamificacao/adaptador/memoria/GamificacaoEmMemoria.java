package org.facens.grupo_6_gameeducator.gamificacao.adaptador.memoria;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.facens.grupo_6_gameeducator.gamificacao.MedalhaDoAluno;
import org.facens.grupo_6_gameeducator.gamificacao.PosicaoNoRanking;
import org.facens.grupo_6_gameeducator.gamificacao.dominio.Progresso;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeMedalhas;
import org.facens.grupo_6_gameeducator.gamificacao.porta.RepositorioDeProgresso;

/**
 * Adaptador em memoria das duas portas de gamificacao. Prova (Desafio 2) que o caso de uso nao
 * depende de JPA: e selecionado com {@code app.gamificacao.persistencia=memoria} e usado nos
 * testes de dominio/aplicacao, que rodam sem Spring e sem banco. Nao e para producao (nao persiste).
 */
public class GamificacaoEmMemoria implements RepositorioDeProgresso, RepositorioDeMedalhas {

    private record Chave(Long alunoId, Long cursoId) {
    }

    private final Map<Chave, Integer> xpPorAlunoECurso = new ConcurrentHashMap<>();
    private final Map<Chave, Map<Integer, LocalDateTime>> medalhas = new ConcurrentHashMap<>();
    private final Map<Long, String> nomes = new ConcurrentHashMap<>();

    /** Nome exibido no ranking. Sem cadastro, o ranking mostra "Aluno {id}". */
    public void registrarNome(Long alunoId, String nome) {
        nomes.put(alunoId, nome);
    }

    @Override
    public Progresso carregar(Long alunoId, Long cursoId) {
        return new Progresso(alunoId, cursoId, xpPorAlunoECurso.getOrDefault(new Chave(alunoId, cursoId), 0));
    }

    @Override
    public void salvar(Progresso progresso) {
        xpPorAlunoECurso.put(new Chave(progresso.alunoId(), progresso.cursoId()), progresso.xpTotal());
    }

    @Override
    public List<PosicaoNoRanking> ranking(Long cursoId) {
        List<Map.Entry<Chave, Integer>> doCurso = xpPorAlunoECurso.entrySet().stream()
                .filter(entrada -> entrada.getKey().cursoId().equals(cursoId))
                .sorted(Map.Entry.<Chave, Integer>comparingByValue(Comparator.reverseOrder()))
                .toList();
        return IntStream.range(0, doCurso.size())
                .mapToObj(i -> {
                    Long alunoId = doCurso.get(i).getKey().alunoId();
                    return new PosicaoNoRanking(i + 1, alunoId,
                            nomes.getOrDefault(alunoId, "Aluno " + alunoId), doCurso.get(i).getValue());
                })
                .toList();
    }

    @Override
    public Set<Integer> marcosConquistados(Long alunoId, Long cursoId) {
        return Set.copyOf(medalhas.getOrDefault(new Chave(alunoId, cursoId), Map.of()).keySet());
    }

    @Override
    public void registrar(Long alunoId, Long cursoId, int marcoXp) {
        medalhas.computeIfAbsent(new Chave(alunoId, cursoId), chave -> new TreeMap<>())
                .putIfAbsent(marcoXp, LocalDateTime.now());
    }

    @Override
    public List<MedalhaDoAluno> listar(Long alunoId, Long cursoId) {
        return new ArrayList<>(medalhas.getOrDefault(new Chave(alunoId, cursoId), Map.of()).entrySet()).stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entrada -> new MedalhaDoAluno(entrada.getKey(), entrada.getValue()))
                .collect(Collectors.toList());
    }
}
