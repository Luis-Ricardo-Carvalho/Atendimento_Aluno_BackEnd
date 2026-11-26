package ifsuldeminas.edu.br.ifsystem.atendimento.demo;

import ifsuldeminas.edu.br.ifsystem.atendimento.api.AtendimentoDtos.*;
import ifsuldeminas.edu.br.ifsystem.atendimento.config.AtendimentoDadosDemo;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.StatusAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.StatusDisponibilidade;
import ifsuldeminas.edu.br.ifsystem.atendimento.service.AtendimentoService;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"demo", "atendimento-demo"})
class AtendimentoDemoTest {

    @MockitoBean JwtDecoder decoder;
    @Autowired AtendimentoService service;
    @Autowired UsuarioRepository usuarios;

    private void entrar(UUID id) {
        var u = usuarios.findById(id).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, List.of()));
    }

    @AfterEach
    void limpar() { SecurityContextHolder.clearContext(); }

    @Test
    void bancoFicticioTemProfissionaisEAlguemDisponivel() {
        entrar(AtendimentoDadosDemo.ANA);
        var lista = service.listarProfissionais(null, null);
        assertTrue(lista.size() >= 8);
        assertTrue(lista.stream().anyMatch(p -> p.status() == StatusDisponibilidade.DISPONIVEL_AGORA));
        assertEquals(1, service.listarProfissionais("redes", null).size());
    }

    @Test
    void alunoSoAcessaOsProprios() {
        entrar(AtendimentoDadosDemo.ANA);
        UUID dela = service.meus(null).stream()
                .filter(a -> a.atendente().id().equals(AtendimentoDadosDemo.EMERSON)).findFirst().orElseThrow().id();
        entrar(AtendimentoDadosDemo.BRUNO);
        var e = assertThrows(ResponseStatusException.class, () -> service.buscar(dela));
        assertEquals(403, e.getStatusCode().value());
        entrar(AtendimentoDadosDemo.LUCIA);
        assertThrows(ResponseStatusException.class, () -> service.listarMensagens(dela));
        entrar(AtendimentoDadosDemo.EMERSON);
        assertEquals(3, service.listarMensagens(dela).size());
    }

    @Test
    void fluxoCompletoDeAtendimento() {
        entrar(AtendimentoDadosDemo.CARLA);
        var criado = service.criar(new CriarAtendimento(AtendimentoDadosDemo.FABIO_CORSINI,
                ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.CategoriaAtendimento.OUTROS, "Teste", "Mensagem inicial"));
        assertEquals(StatusAtendimento.ABERTO, criado.status());
        entrar(AtendimentoDadosDemo.FABIO_CORSINI);
        service.enviarMensagem(criado.id(), new EnviarMensagem("Olá!"));
        assertEquals(StatusAtendimento.EM_ATENDIMENTO, service.buscar(criado.id()).status());
        assertEquals(2, service.listarMensagens(criado.id()).size());
        service.finalizar(criado.id());
        assertEquals(StatusAtendimento.FINALIZADO, service.buscar(criado.id()).status());
        entrar(AtendimentoDadosDemo.CARLA);
        assertThrows(ResponseStatusException.class, () -> service.enviarMensagem(criado.id(), new EnviarMensagem("x")));
    }
}
