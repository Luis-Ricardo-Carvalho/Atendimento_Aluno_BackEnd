package ifsuldeminas.edu.br.ifsystem.atendimento.api;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Lista os usuários do banco fictício para a tela de entrada do modo demonstração. Inexistente fora dele. */
@RestController
@RequestMapping("/api/demo")
@Profile({"demo", "dev"})
@ConditionalOnProperty(name = "atendimento.demo.enabled", havingValue = "true")
public class DemoUsuariosController {

    public record DemoUsuario(UUID id, String nome, Perfil perfil) {}

    private final UsuarioRepository usuarios;

    public DemoUsuariosController(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/usuarios")
    public List<DemoUsuario> usuarios() {
        return usuarios.findAll().stream()
                .map(u -> new DemoUsuario(u.getId(), u.getNome(), u.getPerfil()))
                .sorted(Comparator.comparing(DemoUsuario::perfil).thenComparing(DemoUsuario::nome))
                .toList();
    }
}
