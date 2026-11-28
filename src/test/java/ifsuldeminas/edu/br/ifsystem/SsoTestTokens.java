package ifsuldeminas.edu.br.ifsystem;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.Instant;
import java.util.*;
final class SsoTestTokens {
 private static final Map<String,Jwt> tokens=new java.util.concurrent.ConcurrentHashMap<>();
 static Jwt claims(Usuario user) { return Jwt.withTokenValue(UUID.randomUUID().toString()).header("alg","RS256").subject(user.getId().toString()).claim("cpf",user.getCpf()).claim("nome",user.getNome()).claim("perfil",user.getPerfil().name()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build(); }
 static String token(Usuario user) { var jwt=claims(user);tokens.put(jwt.getTokenValue(),jwt);return jwt.getTokenValue(); }
 static Jwt decode(String token) { var jwt=tokens.get(token);if(jwt==null)throw new BadJwtException("Invalid test token");return jwt; }
 static JwtAuthenticationToken authentication(Usuario user) { return new JwtAuthenticationToken(claims(user),List.of(new SimpleGrantedAuthority("ROLE_"+user.getPerfil()))); }
}
