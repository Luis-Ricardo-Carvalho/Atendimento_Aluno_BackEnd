# Integração com o login único IFSISTEM

## Levantamento e linha de base

Spring Boot 4.1.1, Java 21, React/Vite. Principal `main` atualizada por fetch e
fast-forward antes das alterações. Linha de base: 57 testes, sem falhas.
Antes: JwtFilter e JwtService validavam tokens HMAC próprios, com CPF em sub;
CurrentUser lia uma entidade Usuario do SecurityContext. AuthController fornecia
/auth/login, /auth/register e /auth/me; UsuarioController gerenciava usuários em
/administrador/** e /servidor/**. AdminBootstrap criava administrador por ambiente.
O React tinha login, cadastro de aluno e gestão local de pessoas.

`usuarios` é referenciada por reservations.requester/reviewedBy e por
key_movements.responsibleUser/processedByUser/returnedByUser. As FKs e os UUIDs
foram preservados. A identidade agora vem das claims de um access token RS256,
validado pelo Resource Server com JWKS e issuer do auth. O papel deriva de perfil.
Os usuários locais passam a ser espelhos; nenhuma credencial é criada.

## Cadastro pela equipe do auth

Substituir N pelo próximo índice livre, sem mudar barras ou origens:

```properties
auth.clientes[N].client-id=chaves
auth.clientes[N].redirect-uris=https://chaves-ifsistem.vercel.app/callback.html,http://localhost:5173/callback.html
auth.clientes[N].post-logout-redirect-uris=https://chaves-ifsistem.vercel.app/,http://localhost:5173/
```

Acesso local: http://localhost:5173; usar localhost, evitando 127.0.0.1. O auth
local deve estar em http://localhost:9000. Em produção, o script usa
https://ifsistem-auth.onrender.com. Página inicial pós-login: `/`.
Frontend e backend são publicados separadamente: frontend na Vercel, API na Render.
O card do portal deve apontar para https://chaves-ifsistem.vercel.app/.

## Banco e deploy

Antes de atualizar a API, execute `supabase/sso-mirror.sql` no banco persistente.
Remove NOT NULL das colunas não fornecidas pelo auth e apaga hashes locais;
preserva registros e relacionamentos. Não há Flyway/Liquibase no projeto.
Os UUIDs do auth precisam coincidir com os UUIDs legados; nunca remapear FKs
ou criar outro usuário para o mesmo CPF. Faça backup antes da migração e confira
os IDs com a equipe do auth. O cadastro de novos usuários ocorre somente no auth.

Backend: AUTH_URL opcional (padrão https://ifsistem-auth.onrender.com); manter
DB_URL, DB_USERNAME, DB_PASSWORD, SPRING_PROFILES_ACTIVE=supabase,
APP_ALLOWED_ORIGINS e as variáveis de planilha/calendário existentes.
Remover JWT_SECRET, BOOTSTRAP_ADMIN, ADMIN_CPF, ADMIN_EMAIL, ADMIN_PASSWORD.
Frontend: manter VITE_API_URL=https://chaves-ifsistem.onrender.com e, opcionalmente,
VITE_PORTAL_URL=https://ifsistem-portal.onrender.com/. Nunca configurar client secret.

O script oficial foi obtido de /ifsistem-sso.js e copiado sem alterações para
frontend/public. sso.js configura o cliente, callback.html/callback.js concluem
PKCE e todas as chamadas de API usam IfsistemSSO.fetch. Dados de usuario() são
usados só para exibição; autorização continua no backend. O logout chama o auth.
Não há tela de login local: `/`, `/index.html` e `/login.html` iniciam SSO
sem sessão e abrem o painel com token válido. O logout encerra a sessão no auth
e retorna à raiz, que inicia novo login automaticamente. A transição mostra
logo e spinner; após cinco segundos, mostra “Conectando ao IFSISTEM…”. Erros de
callback exigem “Tentar novamente”, sem redirecionamento automático.

## Validação e limitações

Testes de backend usam JwtDecoder simulado; nenhum chama o auth real. A suíte de
negócio mantém reservas, aprovação, conflitos, chaves, concorrência e permissões.
Testes antigos de senha/cadastro local deixam de se aplicar ao módulo.
A validação completa com credenciais reais depende do cadastro do cliente e da
migração do banco; não executar o deploy enquanto esses passos estiverem pendentes.

Validação final: **57 testes antes e 57 depois**, todos verdes. Foram substituídos
3 testes do cadastro/login/gestão locais por 3 testes de SSO (espelhamento e
atualização, ausência de perfil e remoção das rotas antigas). Build React e teste
Playwright passaram: callback com nonce/state, PKCE S256, access token no Bearer,
persistência de sessão, logout, navegação ao portal e tela de 390px. Auth e API
são simulados no teste de navegador. Login real com sessão compartilhada ainda
precisa ser validado após preparar auth e Supabase.

## Gestão de usuários integrada

A aba **Usuários** aparece somente para ADMINISTRADOR e SERVIDOR (Secretaria)
e é renderizada dentro do IFConect. Segue o contrato API-USUARIOS.md fornecido
pela equipe do auth em 25/11/2026. Chamadas diretas ao auth utilizam
IfsistemSSO.fetch, com access token do usuário atual e renovação pelo SDK.
Nenhum segredo de serviço, senha ou cadastro é gravado no banco do Chaves.

Operações: busca por nome/CPF, filtro por perfil/ativo, paginação de 20 itens,
cadastro de Professor/Servidor, edição dos dados, ativação/inativação, redefinição
de senha e exclusão lógica. Senhas têm confirmação e mostrar/ocultar; não são
exibidas na listagem. O PUT envia todos os campos opcionais preservando o valor
editado, pois o auth transforma campos omitidos em null. CPF e matrícula não
são editáveis. A interface impede alterar o próprio perfil, ativar/inativar ou
excluir a própria conta e não permite atribuir/remover ADMINISTRADOR.

Ações que alteram acesso exigem confirmação; formulários impedem duplo envio.
Erros usam o campo `erro` do auth, inclusive 403/409; 401 inicia novo login.
As permissões reais continuam sendo conferidas pelo auth em cada operação.
Alunos se cadastram no auth. A API de alteração da própria senha (/me/senha)
não foi adicionada a esta tela administrativa.

Validação por Playwright com API simulada: menu visível para Administrador e
Servidor e ausente para Aluno/Professor, cadastro, edição, Bearer e celular.
Nenhum usuário real do auth foi modificado pelos testes.

## Validação SCRUM-X — entrada direta

Linha de base: backend 57 testes e frontend 6 testes, todos verdes. Após a
alteração: backend 57 e frontend 6, todos verdes; build Vite e verify Maven
concluídos. Playwright com auth/API simulados validou as três entradas sem
sessão, index/login com sessão nos quatro perfis, callback válido/erro sem loop,
logout seguido de novo login, espera de cinco segundos e layout no celular.
No JAR combinado, raiz, index, login, callback, scripts, CSS e logo responderam
200 sem token. Login real em deploy ainda requer conferência após a publicação;
esses testes automatizados não acessaram nem alteraram contas reais do auth.
