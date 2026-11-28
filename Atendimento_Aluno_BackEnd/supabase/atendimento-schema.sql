-- ============================================================================
-- Atendimento ao Aluno · estrutura do banco (PostgreSQL / Supabase)
-- Execute UMA vez no SQL Editor do Supabase (Run). É idempotente: pode rodar de novo.
-- Substitui o supabase/setup.sql do Chaves (já inclui a criação do schema privado).
-- O backend (ddl-auto=update) reconhece estas tabelas e só acrescenta o que faltar.
-- ============================================================================
BEGIN;

-- Schema privado, fora da Data API do Supabase (acesso somente pelo backend).
CREATE SCHEMA IF NOT EXISTS app_private;
REVOKE ALL ON SCHEMA app_private FROM PUBLIC;
REVOKE ALL ON SCHEMA app_private FROM anon, authenticated;

-- ---------------------------------------------------------------------------
-- usuarios: espelho dos usuários do login único IFSISTEM (id = claim "sub").
-- Criado pelo backend no primeiro login; aqui apenas garantimos que exista
-- para que as chaves estrangeiras abaixo possam ser criadas. Sem senha.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_private.usuarios (
    id             uuid         PRIMARY KEY,
    nome           varchar(255) NOT NULL,
    cpf            varchar(11)  NOT NULL,
    email          varchar(255),
    matricula      varchar(255),
    senha          varchar(255),
    telefone       varchar(255),
    curso          varchar(255),
    periodo        integer,
    perfil         varchar(255) NOT NULL,
    status         varchar(255) NOT NULL,
    criado_em      timestamptz  NOT NULL,
    atualizado_em  timestamptz  NOT NULL,
    CONSTRAINT uk_usuario_cpf       UNIQUE (cpf),
    CONSTRAINT uk_usuario_email     UNIQUE (email),
    CONSTRAINT uk_usuario_matricula UNIQUE (matricula)
);

-- ---------------------------------------------------------------------------
-- profissional_atendimento: professor/servidor habilitado a atender alunos.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_private.profissional_atendimento (
    id          uuid         PRIMARY KEY,
    usuario_id  uuid         NOT NULL,
    area        varchar(120) NOT NULL,
    ativo       boolean      NOT NULL DEFAULT true,
    CONSTRAINT uk_profissional_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_profissional_usuario FOREIGN KEY (usuario_id) REFERENCES app_private.usuarios (id)
);

-- ---------------------------------------------------------------------------
-- horario_atendimento: janelas semanais em que o profissional atende.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_private.horario_atendimento (
    id              uuid        PRIMARY KEY,
    profissional_id uuid        NOT NULL,
    dia_semana      varchar(10) NOT NULL,
    horario_inicio  time        NOT NULL,
    horario_fim     time        NOT NULL,
    ativo           boolean     NOT NULL DEFAULT true,
    CONSTRAINT fk_horario_profissional FOREIGN KEY (profissional_id)
        REFERENCES app_private.profissional_atendimento (id) ON DELETE CASCADE,
    CONSTRAINT ck_horario_dia CHECK (dia_semana IN
        ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    CONSTRAINT ck_horario_intervalo CHECK (horario_inicio < horario_fim)
);

-- ---------------------------------------------------------------------------
-- atendimento: a solicitação (aluno -> professor/servidor) e seu status.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_private.atendimento (
    id                uuid         PRIMARY KEY,
    aluno_id          uuid         NOT NULL,
    atendente_id      uuid         NOT NULL,
    categoria         varchar(40)  NOT NULL,
    assunto           varchar(150) NOT NULL,
    status            varchar(30)  NOT NULL,
    data_criacao      timestamptz  NOT NULL,
    data_atualizacao  timestamptz  NOT NULL,
    data_finalizacao  timestamptz,
    CONSTRAINT fk_atendimento_aluno     FOREIGN KEY (aluno_id)     REFERENCES app_private.usuarios (id),
    CONSTRAINT fk_atendimento_atendente FOREIGN KEY (atendente_id) REFERENCES app_private.usuarios (id),
    CONSTRAINT ck_atendimento_partes    CHECK (aluno_id <> atendente_id),
    CONSTRAINT ck_atendimento_status    CHECK (status IN
        ('ABERTO','EM_ATENDIMENTO','AGUARDANDO_ALUNO','FINALIZADO','CANCELADO')),
    CONSTRAINT ck_atendimento_categoria CHECK (categoria IN
        ('DUVIDA_DISCIPLINA','DUVIDA_ATIVIDADE','DUVIDA_ACADEMICA','SOLICITACAO_ADMINISTRATIVA','OUTROS'))
);

-- ---------------------------------------------------------------------------
-- mensagem_atendimento: o histórico do chat de cada atendimento.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_private.mensagem_atendimento (
    id              uuid          PRIMARY KEY,
    atendimento_id  uuid          NOT NULL,
    remetente_id    uuid          NOT NULL,
    conteudo        varchar(2000) NOT NULL,
    data_envio      timestamptz   NOT NULL,
    lida            boolean       NOT NULL DEFAULT false,
    CONSTRAINT fk_mensagem_atendimento FOREIGN KEY (atendimento_id)
        REFERENCES app_private.atendimento (id) ON DELETE CASCADE,
    CONSTRAINT fk_mensagem_remetente   FOREIGN KEY (remetente_id) REFERENCES app_private.usuarios (id)
);

-- Índices para as consultas do módulo (meus atendimentos, chat, agenda).
CREATE INDEX IF NOT EXISTS idx_atendimento_aluno     ON app_private.atendimento (aluno_id, data_atualizacao DESC);
CREATE INDEX IF NOT EXISTS idx_atendimento_atendente ON app_private.atendimento (atendente_id, data_atualizacao DESC);
CREATE INDEX IF NOT EXISTS idx_atendimento_status    ON app_private.atendimento (status);
CREATE INDEX IF NOT EXISTS idx_mensagem_conversa     ON app_private.mensagem_atendimento (atendimento_id, data_envio);
CREATE INDEX IF NOT EXISTS idx_horario_profissional  ON app_private.horario_atendimento (profissional_id);

-- Defesa em profundidade: RLS ligado e sem políticas = ninguém, exceto o backend (dono das tabelas), lê/escreve.
ALTER TABLE app_private.usuarios                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_private.profissional_atendimento ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_private.horario_atendimento      ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_private.atendimento              ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_private.mensagem_atendimento     ENABLE ROW LEVEL SECURITY;

COMMENT ON TABLE app_private.profissional_atendimento IS 'Professores/servidores habilitados a atender alunos (criado no primeiro horário cadastrado).';
COMMENT ON TABLE app_private.horario_atendimento      IS 'Janelas semanais de atendimento; base do status Disponível agora / Próximo atendimento.';
COMMENT ON TABLE app_private.atendimento              IS 'Solicitação de atendimento do aluno e seu status.';
COMMENT ON TABLE app_private.mensagem_atendimento     IS 'Mensagens do chat institucional de cada atendimento.';

COMMIT;

-- Conferência (rode separado, depois): deve listar as 5 tabelas.
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'app_private' ORDER BY 1;
