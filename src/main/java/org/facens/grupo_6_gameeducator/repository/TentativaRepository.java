package org.facens.grupo_6_gameeducator.repository;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Tentativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TentativaRepository extends JpaRepository<Tentativa, Long> {

    List<Tentativa> findByAlunoIdAndDesafioIdOrderByDataHoraDesc(Long alunoId, Long desafioId);

    boolean existsByAlunoIdAndDesafioIdAndCorretaTrue(Long alunoId, Long desafioId);

    /** Tentativas do aluno em qualquer desafio de qualquer missao do curso: base do painel do professor. */
    List<Tentativa> findByAlunoIdAndDesafio_Missao_Curso_Id(Long alunoId, Long cursoId);

    /** Quantos desafios distintos da missao o aluno ja acertou: base da deteccao de "missao concluida". */
    @Query("""
            select count(distinct t.desafio.id) from Tentativa t
            where t.aluno.id = :alunoId and t.desafio.missao.id = :missaoId and t.correta = true
            """)
    long contarDesafiosAcertadosNaMissao(Long alunoId, Long missaoId);
}
