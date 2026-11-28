# Hospedagem: Vercel + backend Java + Supabase

Arquitetura: navegador → React na Vercel → API Spring Boot → PostgreSQL do
Supabase. A leitura da planilha continua sendo feita pelo backend. Mantemos o
JWT/BCrypt do projeto; não estamos migrando para Supabase Auth. Nenhuma senha de

## Escolha do backend

Recomendação para demonstração acadêmica com baixo custo: **Render**, com Docker.
O plano gratuito entra em repouso após 15 minutos sem requisições; o primeiro
acesso depois pode demorar. Para uso frequente, considerar uma instância paga.
Alternativa: **Railway**, cujo plano Hobby tem compromisso mínimo de US$ 5/mês,
com cobrança por uso acima do valor incluído. Conferir preços ao contratar.

Fontes consultadas: [Render Java](https://render.com/hosting/java),
[Render gratuito](https://render.com/docs/free),
[Railway preços](https://docs.railway.com/pricing).

## Supabase

1. Crie o projeto e execute `supabase/setup.sql` no SQL editor.
2. Não adicione `app_private` aos schemas expostos pela Data API. As tabelas da
   aplicação serão criadas nesse schema; o React só conversa com nossa API.
3. Em Connect, copie os dados PostgreSQL. Para hosts que precisam de IPv4, prefira
   **Session pooler**, porta 5432. Não usar transaction pooler nesta configuração.
4. Configure no serviço do backend:

```text
SPRING_PROFILES_ACTIVE=supabase
DB_URL=jdbc:postgresql://HOST-DO-SESSION-POOLER:5432/postgres?sslmode=require
DB_USERNAME=postgres.REFERENCIA-DO-PROJETO
DB_PASSWORD=<senha do banco>
DB_POOL_SIZE=5
APP_ALLOWED_ORIGINS=https://SEU-FRONT.vercel.app
BOOTSTRAP_ADMIN=true
ADMIN_CPF=<CPF válido do primeiro administrador>
ADMIN_EMAIL=<email @ifsuldeminas.edu.br>
ADMIN_PASSWORD=<senha forte com pelo menos 12 caracteres>
```

Use host e usuário exatos do painel; DB_URL é JDBC, não a URL HTTPS do projeto.
O perfil exige SSL e limita o pool. H2 fica somente em dev/demo. O bootstrap não
altera administrador existente; após a criação inicial pode ser desabilitado.
O Hibernate mantém a estratégia update existente, sem apagar tabelas. O perfil
não migra automaticamente dados de H2 ou de outro schema já existente.

Fontes: [Conexões PostgreSQL](https://supabase.com/docs/guides/database/connecting-to-postgres),
[schemas expostos](https://supabase.com/docs/guides/api/using-custom-schemas).

## Render / Railway

Crie serviço a partir do repositório, runtime Docker, usando `Dockerfile.backend`.
Esse Dockerfile compila somente a API. Configure as variáveis acima e o domínio
HTTPS público. O backend lê PORT da plataforma. Configure o health check em `/health`. Não ative os perfis dev/demo em
produção. A conexão efetiva ao Supabase depende de configurar o projeto e as
credenciais na hospedagem; ainda não foi executada nesta sessão.

## Vercel

Importe o repositório com Root Directory `frontend`, framework Vite,
build `npm run build`, output `dist`. O arquivo `frontend/vercel.json` já define
build e fallback de SPA.

Configure no frontend:

```text
VITE_API_URL=https://SUA-API.onrender.com
```

Substitua pela URL real do Render ou Railway. Depois de alterar a variável,
republique o frontend, pois Vite incorpora o valor no build. No backend, configure
APP_ALLOWED_ORIGINS com a URL exata da Vercel, sem barra final; para mais de uma,
separe por vírgula. Não há curinga para previews. O proxy API_TARGET é só para o
Vite local. Acesso remoto usa VITE_API_URL com CORS e cabeçalho Bearer.

Fonte: [Vite na Vercel](https://vercel.com/docs/frameworks/frontend/vite).

## Login único

A autenticação e a gestão de usuários pertencem ao IFSISTEM. Siga
[as instruções SSO](sso-integration.md), incluindo a migração do espelho local e
o cadastro do cliente. Não configurar JWT_SECRET ou variáveis ADMIN_*.

## Ping do backend com cron-job.org

O endpoint público `GET /health` retorna HTTP 200 e `{"status":"UP"}` sem
consultar o banco ou a planilha. É uma verificação de resposta da aplicação,
não uma garantia de que todas as dependências estão disponíveis.

No painel do cron-job.org, crie um cronjob com:

| Campo | Valor |
| --- | --- |
| Título | IFConect · Saúde do backend |
| URL | https://chaves-ifsistem.onrender.com/health |
| Método | GET |
| Agendamento | A cada 10 minutos (`*/10 * * * *`) |
| Fuso | America/Sao_Paulo |
| Autenticação/corpo | Nenhum |

Ative o job e use a execução de teste para conferir HTTP 200. Não inclua senha,
JWT ou credenciais do Supabase. Se desejar, habilite notificações de falha e
recuperação no próprio painel. Não foi criado um agendamento externo
automaticamente: essa configuração é feita na conta do responsável.

A Render suspende serviços gratuitos após 15 minutos sem tráfego. Os pings
periódicos ajudam a manter tráfego, mas não garantem disponibilidade e consomem
as horas gratuitas compartilhadas pelo workspace (750/mês). O cron-job.org tem
timeout padrão de 30 segundos; uma inicialização a frio da Render pode demorar
cerca de um minuto e causar falha no primeiro ping. Confira as execuções
seguintes e os logs antes de interpretar isso como erro do backend.

Fontes: [Render Free](https://render.com/docs/free),
[FAQ cron-job.org](https://cron-job.org/en/faq/).


## Retorno ao portal integrado

O menu do IFConect oferece **Voltar ao portal**, acessível também no celular,
para `https://ifsistem-portal.onrender.com/`. A navegação ocorre na mesma aba.
A URL pode ser alterada por `VITE_PORTAL_URL` no frontend; é necessário novo
build/deploy após mudar a variável. O valor padrão já aponta para o portal.
Esse acesso é um link de navegação: não transmite JWT nem implementa login
compartilhado entre os sistemas. A sessão do portal segue suas próprias regras.
