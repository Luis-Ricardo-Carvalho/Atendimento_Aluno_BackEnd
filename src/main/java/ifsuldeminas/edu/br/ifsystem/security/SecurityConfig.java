package ifsuldeminas.edu.br.ifsystem.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;


import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String perfil = jwt.getClaimAsString("perfil");
            if (perfil == null || !java.util.Set.of("ALUNO", "PROFESSOR", "SERVIDOR", "ADMINISTRADOR").contains(perfil)) return java.util.List.of();
            return java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + perfil));
        });
        return converter;
    }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ifsuldeminas.edu.br.ifsystem.common.CurrentUser users) throws Exception {
        return http.addFilterAfter(new SsoMirrorFilter(users), org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class).cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> jsonError(res,401)).accessDeniedHandler((req,res,ex) -> jsonError(res,403)))
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((req,res,ex) -> jsonError(res,401)).accessDeniedHandler((req,res,ex) -> jsonError(res,403)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/callback.html", "/login.html", "/ifsistem-sso.js", "/sso.js", "/callback.js", "/sso-transition.css", "/brand/**", "/assets/**", "/horarios/**", "/favicon.svg", "/error", "/health", "/config").permitAll()
                .requestMatchers("/administrador/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/servidor/**").hasAnyRole("ADMINISTRADOR", "SERVIDOR")
                .anyRequest().authenticated()).build();
    }
    private static void jsonError(jakarta.servlet.http.HttpServletResponse response, int status) throws java.io.IOException {
        response.setStatus(status); response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + (status == 401 ? "Autenticacao IFSISTEM necessaria" : "Acesso nao permitido para este perfil") + "\"}");
    }
    @Bean
    org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource(
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String origins) {
        var config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowedOrigins(java.util.Arrays.stream(origins.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Authorization","Content-Type","Accept"));
        config.setAllowCredentials(false);
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",config);
        return source;
    }
}
