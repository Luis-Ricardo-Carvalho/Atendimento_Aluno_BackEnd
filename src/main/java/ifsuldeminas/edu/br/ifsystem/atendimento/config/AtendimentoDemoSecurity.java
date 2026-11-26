package ifsuldeminas.edu.br.ifsystem.atendimento.config;

import ifsuldeminas.edu.br.ifsystem.usuario.domain.StatusUsuario;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * SOMENTE DEMONSTRAÇÃO LOCAL (perfis demo/dev + atendimento.demo.enabled=true).
 * Permite testar o módulo sem o auth IFSISTEM: o front envia "Authorization: Demo &lt;uuid&gt;" de um usuário
 * do banco fictício. Vale apenas para as rotas do módulo; o restante do sistema continua exigindo o JWT real.
 */
@Configuration
@Profile({"demo", "dev"})
@ConditionalOnProperty(name = "atendimento.demo.enabled", havingValue = "true")
public class AtendimentoDemoSecurity {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain atendimentoDemoChain(HttpSecurity http, UsuarioRepository usuarios) throws Exception {
        return http
                .securityMatcher("/api/atendimentos/**", "/api/horarios-atendimento/**", "/api/demo/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new DemoUserFilter(usuarios), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    static final class DemoUserFilter extends OncePerRequestFilter {
        private final UsuarioRepository usuarios;

        DemoUserFilter(UsuarioRepository usuarios) {
            this.usuarios = usuarios;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Demo ")) {
                try {
                    Usuario u = usuarios.findById(UUID.fromString(header.substring(5).trim())).orElse(null);
                    if (u != null && u.getStatus() == StatusUsuario.ATIVO) {
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                                u, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name()))));
                    }
                } catch (IllegalArgumentException ignored) {
                    // cabeçalho inválido: segue sem autenticação (o módulo responderá 401)
                }
            }
            chain.doFilter(req, res);
        }
    }
}
