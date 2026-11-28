package ifsuldeminas.edu.br.ifsystem;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ifsuldeminas.edu.br.ifsystem.atendimento.repository.AtendimentoRepository;
import ifsuldeminas.edu.br.ifsystem.atendimento.repository.HorarioAtendimentoRepository;
import ifsuldeminas.edu.br.ifsystem.atendimento.repository.MensagemAtendimentoRepository;
import ifsuldeminas.edu.br.ifsystem.atendimento.repository.ProfissionalAtendimentoRepository;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Segurança real (resource server + espelho SSO) nas rotas do módulo, com tokens de teste: nenhum teste chama o auth. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:atendimento-sec;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class AtendimentoSecurityTest {

    private static final String HORARIO = "{\"diaSemana\":\"MONDAY\",\"horarioInicio\":\"14:00\",\"horarioFim\":\"16:00\",\"area\":\"Redes\"}";

    @MockitoBean JwtDecoder decoder;
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository users;
    @Autowired MensagemAtendimentoRepository mensagens;
    @Autowired AtendimentoRepository atendimentos;
    @Autowired HorarioAtendimentoRepository horarios;
    @Autowired ProfissionalAtendimentoRepository profissionais;
    Usuario aluno, professor;

    @BeforeEach
    void setup() {
        when(decoder.decode(anyString())).thenAnswer(i -> SsoTestTokens.decode(i.getArgument(0)));
        limpar();
        aluno = users.save(new Usuario("Aluno Teste", "90000004001", "aluno.teste@ifsuldeminas.edu.br", "ALU-T1", "x", null, "Informática", 3, Perfil.ALUNO));
        professor = users.save(new Usuario("Prof Teste", "90000004002", "prof.teste@ifsuldeminas.edu.br", "PRO-T1", "x", null, null, null, Perfil.PROFESSOR));
    }

    @AfterEach
    void limpar() {
        mensagens.deleteAll();
        atendimentos.deleteAll();
        horarios.deleteAll();
        profissionais.deleteAll();
        users.deleteAll();
    }

    private String bearer(Usuario u) { return "Bearer " + SsoTestTokens.token(u); }

    @Test
    void semTokenRetorna401() throws Exception {
        mvc.perform(get("/api/atendimentos/meus")).andExpect(status().isUnauthorized());
    }

    @Test
    void alunoAutenticadoAcessaOModulo() throws Exception {
        mvc.perform(get("/api/atendimentos/meus").header("Authorization", bearer(aluno)))
                .andExpect(status().isOk()).andExpect(content().string("[]"));
    }

    @Test
    void alunoNaoCadastraHorarioDeAtendimento() throws Exception {
        mvc.perform(post("/api/horarios-atendimento").header("Authorization", bearer(aluno))
                        .contentType(MediaType.APPLICATION_JSON).content(HORARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    void professorCadastraHorarioEAlunoPassaAVerOProfessor() throws Exception {
        mvc.perform(post("/api/horarios-atendimento").header("Authorization", bearer(professor))
                        .contentType(MediaType.APPLICATION_JSON).content(HORARIO))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/atendimentos/profissionais").header("Authorization", bearer(aluno)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Prof Teste"))
                .andExpect(jsonPath("$[0].area").value("Redes"));
    }
}
