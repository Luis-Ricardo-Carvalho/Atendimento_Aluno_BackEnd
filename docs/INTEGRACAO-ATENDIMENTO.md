# Integração do módulo Atendimento ao Aluno ao IFSISTEM

## Deploy do backend na Render (erro "requirements.txt")
O log mostra `Using Python ... pip install -r requirements.txt`: o serviço foi criado com a linguagem **Python**. Este backend é **Java/Spring Boot**.
A Render não permite trocar a linguagem de um serviço existente; crie um novo:
1. Render → New → **Web Service** → repositório do backend (a raiz deve ter `pom.xml` e `Dockerfile.backend`, que estão neste zip).
2. **Language/Runtime: Docker** · Dockerfile Path: `./Dockerfile.backend` · Health Check Path: `/health`. (Ou use o `render.yaml` deste zip: New → Blueprint.)
3. Variáveis de ambiente (nunca no Git):
   | Variável | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `supabase` |
   | `DB_URL` | `jdbc:postgresql://HOST-DO-SESSION-POOLER:5432/postgres?sslmode=require` |
   | `DB_USERNAME` | `postgres.REFERENCIA-DO-PROJETO` |
   | `DB_PASSWORD` | senha do banco do SEU projeto Supabase |
   | `DB_POOL_SIZE` | `4` |
   | `APP_ALLOWED_ORIGINS` | URL do front (ex.: `https://SEU-FRONT.vercel.app`) |
   | `AUTH_URL` | opcional (padrão `https://ifsistem-auth.onrender.com`) |
   Remova `JWT_SECRET`, `ADMIN_*` e `BOOTSTRAP_ADMIN` se existirem: o login é do IFSISTEM. Não ative os perfis `demo`/`dev`/`atendimento-demo` em produção.
4. No Supabase, execute `supabase/setup.sql` (do repositório do Chaves) uma vez; o Hibernate cria as tabelas do módulo no schema `app_private`.
5. Render Free dorme após 15 min; se usar keep-alive, restrinja ao horário útil (`*/5 7-18 * * 1-5` em `/health`).

## Etapa 1 · Pedido de cadastro no auth (para a coordenação)
```properties
auth.clientes[N].client-id=atendimento
auth.clientes[N].redirect-uris={URL_FRONT}/callback.html,http://localhost:5173/callback.html
auth.clientes[N].post-logout-redirect-uris={URL_FRONT}/,http://localhost:5173/
```
Substitua `{URL_FRONT}` pela URL de produção do front, sem barra final (comparação caractere por caractere). Até o cadastro sair, `/oauth2/authorize` responde 400: esperado.

## Para o portal
`{"id": "atendimento", "nome": "Atendimento ao Aluno", "descricao": "Fale com professores e servidores e acompanhe suas solicitações.", "url": "{URL_FRONT}", "ativo": true}`

## Relatório
- **STATUS:** PARCIAL (código pronto e testado em navegador simulado; falta cadastro do cliente no auth, deploy e validação com o auth real).
- **FEITO:** backend como resource server (herdado do Chaves: `CurrentUser`, espelho de `Usuario` por `sub`); módulo com `/api/atendimentos` e `/api/horarios-atendimento`;
  front com login direto (sem botão intermediário, rota pretendida restaurada após o callback, "Conectando ao IFSISTEM…" após 5 s, erro com "Tentar novamente"),
  401 → login, 403 → "Seu perfil não tem acesso a esta ação.", tela **Usuários** (SERVIDOR/ADMINISTRADOR) pela API do auth, **Trocar minha senha** (todos),
  diálogos próprios (sem `alert/confirm/prompt`), menu em gaveta ≤1024 px, tabela em cards ≤600 px, sem rolagem horizontal em 360 px.
  Professores/servidores cadastram os próprios horários em **Meus horários** (o primeiro horário os habilita como profissionais de atendimento).
- **TESTADO:** `npm test` (3 ok); Chromium headless com API/SSO simulados (entrada, filtros, atendimento, chat, usuários, senha, horários, gaveta a 360 px, 0 erros de console); screenshots em `docs/screenshots/ifsistem/`.
  Backend **não compilado aqui** (sem JDK/Maven): rode `./mvnw clean verify`.
- **PENDÊNCIAS:** (a) bloco `auth.clientes` acima; (b) variáveis da Render acima; (c) JSON do portal acima; (d) usar o auth real exige o cliente cadastrado; (e) o visual segue o Guia Visual IF Conect enviado (verdes #14532D/#22C55E/#15803D), não o #2F9E41 do prompt: avise se preferir o do prompt.
