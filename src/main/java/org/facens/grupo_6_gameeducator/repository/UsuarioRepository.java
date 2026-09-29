package org.facens.grupo_6_gameeducator.repository;

import java.util.Optional;
import org.facens.grupo_6_gameeducator.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
}
