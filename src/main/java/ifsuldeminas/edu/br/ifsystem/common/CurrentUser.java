package ifsuldeminas.edu.br.ifsystem.common;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {
    private final ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository users;
    private final RoomOperationLock lock;
    public CurrentUser(ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository users, RoomOperationLock lock) { this.users = users; this.lock = lock; }
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Usuario require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticacao IFSISTEM necessaria");
        try {
            var id = java.util.UUID.fromString(jwt.getSubject());
            var perfil = Perfil.valueOf(jwt.getClaimAsString("perfil"));
            String nome = jwt.getClaimAsString("nome"), cpf = jwt.getClaimAsString("cpf");
            if (nome == null || nome.isBlank() || cpf == null || !cpf.matches("[0-9]{11}")) throw new IllegalArgumentException();
            lock.acquire("sso-user:" + id);
            var user = users.findById(id).orElse(null);
            if (user == null) return users.save(Usuario.fromSso(id, nome, cpf, perfil));
            if (!nome.equals(user.getNome()) || !cpf.equals(user.getCpf()) || perfil != user.getPerfil() || user.getStatus() != StatusUsuario.ATIVO || user.getSenha() != null) {
                user.syncSso(nome, cpf, perfil); return users.save(user);
            }
            return user;
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Claims de usuario invalidas ou perfil ausente");
        }
    }

    public boolean isOperator(Usuario user) {
        return user.getPerfil() == Perfil.ADMINISTRADOR || user.getPerfil() == Perfil.SERVIDOR;
    }

    public Usuario requireOperator() {
        Usuario user = require();
        if (!isOperator(user))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator permission required");
        return user;
    }
}
