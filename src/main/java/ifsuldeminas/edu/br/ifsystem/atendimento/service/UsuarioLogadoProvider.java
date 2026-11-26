package ifsuldeminas.edu.br.ifsystem.atendimento.service;

import ifsuldeminas.edu.br.ifsystem.common.CurrentUser;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Único ponto do módulo que conhece a autenticação.
 * Produção: JWT do login único IFSISTEM, resolvido pelo CurrentUser do projeto (espelho por UUID).
 * Demonstração: o filtro do perfil "atendimento-demo" coloca o próprio Usuario como principal.
 */
@Component
public class UsuarioLogadoProvider {

    private final CurrentUser currentUser;

    public UsuarioLogadoProvider(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    public Usuario obter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario usuario) {
            return usuario;
        }
        return currentUser.require();
    }
}
