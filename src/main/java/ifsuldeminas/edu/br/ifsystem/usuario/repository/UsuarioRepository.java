package ifsuldeminas.edu.br.ifsystem.usuario.repository;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
}
