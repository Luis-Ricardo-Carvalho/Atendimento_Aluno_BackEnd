package ifsuldeminas.edu.br.ifsystem.common;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class CurrentUser {

    private final UsuarioRepository usuarios;

    public CurrentUser(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado.");
        }
        if (auth.getPrincipal() instanceof Usuario u) {
            return u;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            try {
                UUID id = UUID.fromString(jwt.getSubject());
                return usuarios.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido.");
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação inválida.");
    }
}
