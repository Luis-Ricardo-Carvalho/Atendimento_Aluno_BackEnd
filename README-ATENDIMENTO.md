# Módulo Atendimento ao Aluno · backend (arquitetura SSO do Chaves-ifsistem)

Pacote `ifsuldeminas.edu.br.ifsystem.atendimento`. Copie `src/` para a raiz do repositório (as pastas se mesclam; nenhum arquivo existente é alterado).

## O que ele reutiliza do projeto
- `CurrentUser.require()` (JWT RS256 do login único IFSISTEM → espelho `Usuario` por UUID), `Perfil`, `StatusUsuario`, `UsuarioRepository`, `Clock`.
- Rotas no padrão do projeto: `/api/atendimentos/**` e `/api/horarios-atendimento/**` (já exigem autenticação no `SecurityConfig`).
- Erros no formato `ProblemDetail` (`detail`) por meio de um `@RestControllerAdvice` próprio, pois o `ModuleExceptionHandler` só cobre os pacotes listados nele.
- Disponibilidade calculada em `America/Sao_Paulo` (o `Clock` do projeto é UTC).

## Banco fictício e execução local (sem o auth IFSISTEM)
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo,atendimento-demo     # porta 8081
```
H2 em memória, apagado ao encerrar. Cria 6 professores, 2 servidores, 3 alunos e 5 atendimentos fictícios
(status ABERTO, EM_ATENDIMENTO, AGUARDANDO_ALUNO e FINALIZADO). Emerson Carvalho ganha uma janela em torno do horário da
inicialização, então sempre há alguém "Disponível agora". Sem senhas: no modo demo o front envia `Authorization: Demo <uuid>`
(somente rotas do módulo, somente perfis demo/dev). Em produção esse caminho não existe.

| Usuário | Perfil | UUID |
|---|---|---|
| Ana Aluna Demo | ALUNO | 00000000-0000-4000-8000-00000000c001 |
| Bruno Aluno Demo | ALUNO | ...c002 |
| Carla Aluna Demo | ALUNO | ...c003 |
| Emerson Carvalho … Matheus Eloy | PROFESSOR | ...a001 a ...a006 |
| Marcos Pereira, Renata Lima | SERVIDOR | ...b001, ...b002 |

`GET /api/demo/usuarios` lista os usuários (usado pela tela de entrada do front).

## Produção / Supabase
Rode antes `supabase/atendimento-schema.sql` no SQL Editor (cria schema, tabelas, índices e RLS; idempotente).
`ddl-auto=update` cria `profissional_atendimento`, `horario_atendimento`, `atendimento` e `mensagem_atendimento`. Nenhum seed roda sem
`atendimento.seed.enabled=true` + perfil demo/dev. Para o professor aparecer na lista é preciso um registro em `profissional_atendimento`
(a tela de cadastro dos profissionais ainda não existe; `POST /api/horarios-atendimento` cadastra horários do próprio profissional).

## Testes
`./mvnw test` → `DisponibilidadeServiceTest` (regra de horário) e `AtendimentoDemoTest` (banco fictício, permissões e fluxo completo).
