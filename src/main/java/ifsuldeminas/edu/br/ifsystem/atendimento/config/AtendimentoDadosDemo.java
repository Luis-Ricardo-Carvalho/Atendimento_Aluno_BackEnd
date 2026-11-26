package ifsuldeminas.edu.br.ifsystem.atendimento.config;

import ifsuldeminas.edu.br.ifsystem.atendimento.domain.*;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.CategoriaAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.domain.enums.StatusAtendimento;
import ifsuldeminas.edu.br.ifsystem.atendimento.repository.*;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Perfil;
import ifsuldeminas.edu.br.ifsystem.usuario.domain.Usuario;
import ifsuldeminas.edu.br.ifsystem.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BANCO FICTÍCIO (H2 em memória, apagado ao encerrar). Só roda nos perfis demo/dev com
 * atendimento.seed.enabled=true. Todos os nomes, CPFs e e-mails são inventados; não há senhas.
 */
@Component
@Profile({"demo", "dev"})
@ConditionalOnProperty(name = "atendimento.seed.enabled", havingValue = "true")
public class AtendimentoDadosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AtendimentoDadosDemo.class);

    public static final UUID ANA = id("00000000c001");
    public static final UUID BRUNO = id("00000000c002");
    public static final UUID CARLA = id("00000000c003");
    public static final UUID EMERSON = id("00000000a001");
    public static final UUID FABIO_CORSINI = id("00000000a002");
    public static final UUID LUCIA = id("00000000a003");
    public static final UUID MATHEUS = id("00000000a006");

    private static UUID id(String fim) { return UUID.fromString("00000000-0000-4000-8000-" + fim); }

    private record Faixa(DayOfWeek dia, LocalTime inicio, LocalTime fim) {}
    private record Prof(UUID id, String nome, String email, String cpf, Perfil perfil, String area, List<Faixa> faixas) {}

    private static Faixa f(DayOfWeek d, int h1, int h2) { return new Faixa(d, LocalTime.of(h1, 0), LocalTime.of(h2, 0)); }

    private static final List<Prof> PROFISSIONAIS = List.of(
            new Prof(EMERSON, "Emerson Carvalho", "emerson.carvalho", "90000000101", Perfil.PROFESSOR, "Redes de Computadores", List.of(f(DayOfWeek.MONDAY, 14, 16))),
            new Prof(FABIO_CORSINI, "Fabio Corsini", "fabio.corsini", "90000000102", Perfil.PROFESSOR, "Programação", List.of(f(DayOfWeek.TUESDAY, 9, 11))),
            new Prof(LUCIA, "Lucia Helena", "lucia.helena", "90000000103", Perfil.PROFESSOR, "Banco de Dados", List.of(f(DayOfWeek.WEDNESDAY, 14, 16))),
            new Prof(id("00000000a004"), "André Marques", "andre.marques", "90000000104", Perfil.PROFESSOR, "Desenvolvimento Web", List.of(f(DayOfWeek.THURSDAY, 10, 12))),
            new Prof(id("00000000a005"), "Fabio Junior", "fabio.junior", "90000000105", Perfil.PROFESSOR, "Engenharia de Software", List.of(f(DayOfWeek.FRIDAY, 8, 10))),
            new Prof(MATHEUS, "Matheus Eloy", "matheus.eloy", "90000000106", Perfil.PROFESSOR, "Inteligência Artificial", List.of(f(DayOfWeek.WEDNESDAY, 18, 20))),
            new Prof(id("00000000b001"), "Marcos Pereira (Demo)", "marcos.pereira", "90000000201", Perfil.SERVIDOR, "Secretaria Acadêmica", List.of(f(DayOfWeek.MONDAY, 8, 12), f(DayOfWeek.WEDNESDAY, 13, 17))),
            new Prof(id("00000000b002"), "Renata Lima (Demo)", "renata.lima", "90000000202", Perfil.SERVIDOR, "Assistência Estudantil", List.of(f(DayOfWeek.FRIDAY, 13, 17))));

    private final UsuarioRepository usuarios;
    private final ProfissionalAtendimentoRepository profissionais;
    private final HorarioAtendimentoRepository horarios;
    private final AtendimentoRepository atendimentos;
    private final MensagemAtendimentoRepository mensagens;

    public AtendimentoDadosDemo(UsuarioRepository usuarios, ProfissionalAtendimentoRepository profissionais,
                                HorarioAtendimentoRepository horarios, AtendimentoRepository atendimentos,
                                MensagemAtendimentoRepository mensagens) {
        this.usuarios = usuarios;
        this.profissionais = profissionais;
        this.horarios = horarios;
        this.atendimentos = atendimentos;
        this.mensagens = mensagens;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws InterruptedException {
        if (usuarios.existsById(ANA)) return;

        Usuario ana = aluno(ANA, "Ana Aluna Demo", "ana.aluna", "90000000301", "Informática", 5);
        Usuario bruno = aluno(BRUNO, "Bruno Aluno Demo", "bruno.aluno", "90000000302", "Informática", 3);
        Usuario carla = aluno(CARLA, "Carla Aluna Demo", "carla.aluna", "90000000303", "Administração", 2);

        for (Prof p : PROFISSIONAIS) {
            Usuario u = Usuario.fromSso(p.id(), p.nome(), p.cpf(), p.perfil());
            u.atualizar(p.nome(), p.email() + "@ifsuldeminas.edu.br", null, null, null);
            usuarios.save(u);
            ProfissionalAtendimento perfil = profissionais.save(new ProfissionalAtendimento(u, p.area()));
            List<Faixa> faixas = new ArrayList<>(p.faixas());
            if (p.id().equals(EMERSON)) faixas.add(plantaoDeHoje());
            for (Faixa x : faixas) {
                if (x != null) horarios.save(new HorarioAtendimento(perfil, x.dia(), x.inicio(), x.fim()));
            }
        }

        Usuario emerson = usuarios.getReferenceById(EMERSON);
        Usuario corsini = usuarios.getReferenceById(FABIO_CORSINI);
        Usuario lucia = usuarios.getReferenceById(LUCIA);
        Usuario matheus = usuarios.getReferenceById(MATHEUS);

        Atendimento a1 = novo(ana, emerson, CategoriaAtendimento.DUVIDA_DISCIPLINA, "Dúvida sobre sub-redes");
        msg(a1, ana, "Professor, não entendi como calcular a máscara de sub-rede do exercício 4.");
        msg(a1, emerson, "Olá, Ana! Comece pelo número de hosts necessários e encontre a menor potência de 2 que o comporte.");
        msg(a1, ana, "Entendi! Posso te enviar minha resolução para conferir?");
        a1.alterarStatus(StatusAtendimento.EM_ATENDIMENTO);
        atendimentos.save(a1);

        Atendimento a2 = novo(ana, lucia, CategoriaAtendimento.DUVIDA_ATIVIDADE, "Prazo do trabalho de modelagem");
        msg(a2, ana, "Professora, o prazo do trabalho de modelagem foi prorrogado?");

        Atendimento a3 = novo(ana, corsini, CategoriaAtendimento.DUVIDA_ACADEMICA, "Monitoria de Programação");
        msg(a3, ana, "Gostaria de saber como participar da monitoria.");
        msg(a3, corsini, "Procure a coordenação com seu histórico. Qualquer dúvida, é só chamar!");
        a3.finalizar();
        atendimentos.save(a3);

        Atendimento a4 = novo(bruno, emerson, CategoriaAtendimento.OUTROS, "Revisão da prova");
        msg(a4, bruno, "Posso agendar uma revisão da última prova?");

        Atendimento a5 = novo(carla, matheus, CategoriaAtendimento.DUVIDA_DISCIPLINA, "Material de Inteligência Artificial");
        msg(a5, carla, "Onde encontro os slides da última aula?");
        msg(a5, matheus, "Carla, qual turma você cursa? Preciso confirmar antes de enviar.");
        a5.alterarStatus(StatusAtendimento.AGUARDANDO_ALUNO);
        atendimentos.save(a5);

        log.info("Atendimento (demo): {} profissionais, 3 alunos e 5 atendimentos fictícios criados.", PROFISSIONAIS.size());
    }

    private Usuario aluno(UUID id, String nome, String email, String cpf, String curso, int periodo) {
        Usuario u = Usuario.fromSso(id, nome, cpf, Perfil.ALUNO);
        u.atualizar(nome, email + "@ifsuldeminas.edu.br", null, curso, periodo);
        return usuarios.save(u);
    }

    private Atendimento novo(Usuario aluno, Usuario atendente, CategoriaAtendimento c, String assunto) {
        return atendimentos.save(new Atendimento(aluno, atendente, c, assunto));
    }

    private void msg(Atendimento a, Usuario remetente, String texto) throws InterruptedException {
        mensagens.save(new MensagemAtendimento(a, remetente, texto));
        Thread.sleep(5); // garante ordem cronológica estável
    }

    /** Janela em torno do horário de início do app, para que sempre haja alguém "Disponível agora" nos testes. */
    private Faixa plantaoDeHoje() {
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        LocalDateTime ini = agora.minusHours(1), fim = agora.plusHours(3);
        LocalTime i = ini.toLocalDate().equals(agora.toLocalDate()) ? ini.toLocalTime() : LocalTime.MIN;
        LocalTime e = fim.toLocalDate().equals(agora.toLocalDate()) ? fim.toLocalTime() : LocalTime.of(23, 59);
        return i.isBefore(e) ? new Faixa(agora.getDayOfWeek(), i, e) : null;
    }
}
