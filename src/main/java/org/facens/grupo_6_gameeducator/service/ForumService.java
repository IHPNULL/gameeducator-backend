package org.facens.grupo_6_gameeducator.service;

import java.util.List;
import org.facens.grupo_6_gameeducator.domain.Curso;
import org.facens.grupo_6_gameeducator.domain.Post;
import org.facens.grupo_6_gameeducator.domain.RespostaPost;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.facens.grupo_6_gameeducator.exception.AcessoNegadoException;
import org.facens.grupo_6_gameeducator.exception.RecursoNaoEncontradoException;
import org.facens.grupo_6_gameeducator.exception.RegraDeNegocioException;
import org.facens.grupo_6_gameeducator.repository.CursoRepository;
import org.facens.grupo_6_gameeducator.repository.MatriculaRepository;
import org.facens.grupo_6_gameeducator.repository.PostRepository;
import org.facens.grupo_6_gameeducator.repository.RespostaPostRepository;
import org.facens.grupo_6_gameeducator.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aba de foruns: "Como aluno, quero ter acesso aos foruns, para interagir e ajudar."
 * Vejo os posts, respondo, sou respondido e publico o meu proprio post.
 */
@Service
@Transactional
public class ForumService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final MatriculaRepository matriculaRepository;
    private final PostRepository postRepository;
    private final RespostaPostRepository respostaPostRepository;

    public ForumService(UsuarioRepository usuarioRepository,
                        CursoRepository cursoRepository,
                        MatriculaRepository matriculaRepository,
                        PostRepository postRepository,
                        RespostaPostRepository respostaPostRepository) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.matriculaRepository = matriculaRepository;
        this.postRepository = postRepository;
        this.respostaPostRepository = respostaPostRepository;
    }

    /** Posts do forum do curso, do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<Post> listarPosts(Long usuarioId, Long cursoId) {
        exigirAcessoAoCurso(usuarioId, cursoId);
        List<Post> posts = postRepository.findByCursoIdOrderByDataHoraDescIdDesc(cursoId);
        // Inicializa autor e respostas (LAZY) enquanto a sessao ainda esta aberta: o controller
        // monta o PostResponse fora da transacao, onde eles nao poderiam mais ser carregados.
        posts.forEach(post -> {
            post.getAutor().getNome();
            post.getRespostas().forEach(resposta -> resposta.getAutor().getNome());
        });
        return posts;
    }

    /** Publica um post proprio no forum do curso. */
    public Post publicar(Long usuarioId, Long cursoId, String titulo, String conteudo) {
        Acesso acesso = exigirAcessoAoCurso(usuarioId, cursoId);
        if (titulo == null || titulo.isBlank()) {
            throw new RegraDeNegocioException("O post precisa de um titulo");
        }
        if (conteudo == null || conteudo.isBlank()) {
            throw new RegraDeNegocioException("O post precisa de um conteudo");
        }
        return postRepository.save(new Post(acesso.curso(), acesso.usuario(), titulo, conteudo));
    }

    /** Responde um post: e assim que o aluno responde e e respondido. */
    public RespostaPost responder(Long usuarioId, Long postId, String conteudo) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Post", postId));
        Acesso acesso = exigirAcessoAoCurso(usuarioId, post.getCurso().getId());
        if (conteudo == null || conteudo.isBlank()) {
            throw new RegraDeNegocioException("A resposta precisa de um conteudo");
        }
        RespostaPost resposta = new RespostaPost(acesso.usuario(), conteudo);
        post.adicionarResposta(resposta);
        return respostaPostRepository.save(resposta);
    }

    @Transactional(readOnly = true)
    public List<RespostaPost> respostasDo(Long postId) {
        return respostaPostRepository.findByPostIdOrderByIdAsc(postId);
    }

    /** Quem acessou o forum e em qual curso. */
    private record Acesso(Usuario usuario, Curso curso) {
    }

    /** Acesso ao forum: aluno matriculado ou o professor responsavel pelo curso. */
    private Acesso exigirAcessoAoCurso(Long usuarioId, Long cursoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", usuarioId));
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Curso", cursoId));
        if (matriculaRepository.existsByCursoIdAndAlunoId(cursoId, usuarioId) || curso.ehResponsavel(usuario)) {
            return new Acesso(usuario, curso);
        }
        throw new AcessoNegadoException(
                "Usuario " + usuarioId + " nao tem acesso ao forum do curso " + cursoId);
    }
}
