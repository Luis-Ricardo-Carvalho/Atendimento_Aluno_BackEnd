package ifsuldeminas.edu.br.ifsystem.usuario.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String telefone;

    @Column(length = 100)
    private String curso;

    private Integer periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusUsuario status = StatusUsuario.ATIVO;

    protected Usuario() {}

    private Usuario(UUID id, String nome, String cpf, Perfil perfil) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.perfil = perfil;
        this.status = StatusUsuario.ATIVO;
    }

    public static Usuario fromSso(UUID id, String nome, String cpf, Perfil perfil) {
        return new Usuario(id, nome, cpf, perfil);
    }

    public void atualizar(String nome, String email, String telefone, String curso, Integer periodo) {
        if (nome != null) this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.curso = curso;
        this.periodo = periodo;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getCurso() { return curso; }
    public Integer getPeriodo() { return periodo; }
    public Perfil getPerfil() { return perfil; }
    public StatusUsuario getStatus() { return status; }
}
