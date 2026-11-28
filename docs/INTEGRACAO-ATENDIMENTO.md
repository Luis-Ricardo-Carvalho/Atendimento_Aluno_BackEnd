# Integração do módulo Atendimento ao Aluno ao login único IFSISTEM

Dados do módulo: clientId `atendimento` · produção `https://atendimentoalunofrontend.vercel.app` · porta local `5173` · página inicial `/`.

## Passo 0 · Levantamento
- **Stack:** Spring Boot 4.1.1, Java 21, Maven; front React 19 + Vite.
- **Login anterior:** o módulo nasceu sobre a base do Chaves-ifsistem, que já é *resource server* (JWT RS256 do auth, `SsoMirrorFilter`, `CurrentUser`). Não há JWT próprio, HS256/`JWT_SECRET`, endpoints de login/cadastro/senha, AdminBootstrap nem usuários-semente com senha para remover. Um arquivo de teste/demo do módulo (`atendimento-demo`) existe, mas só roda nos perfis `demo`/`dev`.
- **Usuário atual:** `CurrentUser.require()` (via `UsuarioLogadoProvider`). **Tabela de usuários:** `usuarios` (espelho por `sub`); referenciada pelas tabelas do módulo (`atendimento`, `mensagem_atendimento`, `profissional_atendimento`).
- **Telas de login/cadastro:** nenhuma própria; usuários são geridos pela API do auth (tela **Usuários**, SERVIDOR/ADMINISTRADOR) e a senha por **Trocar minha senha**.

## Passo 1 · Cadastro do cliente no auth (enviar à equipe do auth)
```properties
auth.clientes[N].client-id=atendimento
auth.clientes[N].redirect-uris=https://atendimentoalunofrontend.vercel.app/callback.html,http://localhost:5173/callback.html
auth.clientes[N].post-logout-redirect-uris=https://atendimentoalunofrontend.vercel.app/,http://localhost:5173/
```
Nome do módulo: Atendimento ao Aluno · Página inicial: `/`. Deixe `N` para a equipe do auth. As URIs são comparadas caractere a caractere.

## Passos 2 e 3 · Conferência (checklist do prompt)
| Item | Situação |
|---|---|
| `jwk-set-uri` e `issuer-uri` via `AUTH_URL` | herdado do Chaves (`application.properties`) |
| Converter `perfil` → `ROLE_<PERFIL>`; sem perfil = sem papel | herdado (`SecurityConfig`) |
| Stateless, CSRF off, 401/403 em JSON, estáticos e `/health` liberados | herdado |
| Upsert do usuário a partir das claims, sem senha | herdado (`SsoMirrorFilter` + `CurrentUser`) |
| Rotas do módulo autenticadas (`/api/atendimentos/**`, `/api/horarios-atendimento/**`) | `anyRequest().authenticated()` |
| `ifsistem-sso.js` copiado + `sso.js` com `clientId: 'atendimento'` | feito (`frontend/public`) |
| `callback.html` → `handleCallback()` e vai para `/` (restaura a rota pretendida); erro mostra mensagem + "Tentar novamente" | feito |
| Páginas protegidas: sem token → `login()`; chamadas via `IfsistemSSO.fetch`; nome/perfil de `usuario()` | feito |
| Sair → `IfsistemSSO.logout()` | feito |
| Botão "Entrar com IFSISTEM" e link "← Portal IFSISTEM" | feito (tela de entrada/erro) |
| Só o access_token vai ao backend; sem HTML injetado | feito (React escapa; sem `dangerouslySetInnerHTML`) |

## Passo 4 · Testes
- `AtendimentoSecurityTest` (novo): 401 sem token; aluno autenticado acessa o módulo; 403 para aluno cadastrar horário; professor cadastra horário e o aluno o vê. Usa `SsoTestTokens` e `JwtDecoder` mock, sem chamar o auth.
- `AtendimentoDemoTest` (banco fictício, permissões, fluxo completo) e `DisponibilidadeServiceTest`.
- Front: `npm test` (3) e Chromium headless com API/SSO simulados.

## Banco de dados (Supabase)
Execute `supabase/atendimento-schema.sql` no SQL Editor (uma vez; idempotente). Cria o schema privado `app_private`, as tabelas `profissional_atendimento`, `horario_atendimento`, `atendimento`, `mensagem_atendimento` (e `usuarios`, se ainda não existir), chaves estrangeiras, regras de integridade, índices e RLS. O backend com `ddl-auto=update` reconhece a estrutura e só acrescenta o que faltar.

## Passo 5 · Entrega
Variáveis na Render: `SPRING_PROFILES_ACTIVE=supabase`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `DB_POOL_SIZE`, `APP_ALLOWED_ORIGINS=https://atendimentoalunofrontend.vercel.app`, `AUTH_URL` (opcional). Sem `JWT_SECRET`/`ADMIN_*`.
Vercel: `VITE_API_URL` = URL pública do backend; `VITE_PORTAL_URL` opcional.

## Relatório final
STATUS: PARCIAL
FEITO: levantamento (Passo 0); módulo sobre a base já integrada ao SSO; front com login direto, callback com rota restaurada, usuários, troca de senha e horários; conferência do checklist acima.
ARQUIVOS: `atendimento/**` (backend), `AtendimentoSecurityTest`, `Dockerfile.backend`, `render.yaml`; `frontend/**` (src, public/sso.js, callback.js).
TESTADO: **backend não compilado nem executado aqui (sem JDK/Maven): contagem de testes antes/depois indisponível; rodar `./mvnw clean verify`**; front: `npm test` 3/3 e navegador simulado sem erros; login real não validado (cliente ainda não cadastrado).
PENDÊNCIAS: bloco `auth.clientes[N]` acima; variáveis da Render/Vercel; URL final `https://atendimentoalunofrontend.vercel.app` para ativar o card no portal; cadastrar `profissional_atendimento` só ocorre quando o professor/servidor cria o primeiro horário.
