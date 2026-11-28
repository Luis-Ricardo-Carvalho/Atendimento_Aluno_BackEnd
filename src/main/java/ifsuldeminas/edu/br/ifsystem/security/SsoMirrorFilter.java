package ifsuldeminas.edu.br.ifsystem.security;

import ifsuldeminas.edu.br.ifsystem.common.CurrentUser;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

final class SsoMirrorFilter extends OncePerRequestFilter {
 private final CurrentUser users;
 SsoMirrorFilter(CurrentUser users) { this.users=users; }
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(auth!=null && auth.getPrincipal() instanceof Jwt) {
   try { users.require(); }
   catch(org.springframework.web.server.ResponseStatusException ex) {
    res.setStatus(ex.getStatusCode().value());res.setContentType("application/json;charset=UTF-8");
    res.getWriter().write("{\"detail\":\"Claims de usuario invalidas ou perfil ausente\"}");return;
   }
   catch(org.springframework.dao.DataIntegrityViolationException ex) {
    res.setStatus(409);res.setContentType("application/json;charset=UTF-8");
    res.getWriter().write("{\"detail\":\"Identidade local divergente. Confira o UUID e o CPF com a equipe do IFSISTEM\"}");return;
   }
  }
  chain.doFilter(req,res);
 }
}
