# Grade de aulas e atualização anual

A disponibilidade agora reúne as reservas aprovadas e os horários da planilha.
Aulas aparecem como **Em aula**, junto dos detalhes com turma, início, fim e texto
original de disciplina/docente. Reservas aprovadas aparecem como **Reservada**.
Intervalos ocupados sobrepostos/adjacentes são unidos antes do cálculo dos livres.
O servidor também bloqueia solicitações e aprovações sobrepostas a aulas; alterar
apenas a interface ou enviar POST diretamente não contorna a regra. Horários
adjacentes continuam permitidos.

## Fonte e calendário atuais

A fonte fornecida é `Horarios_Turmas_IFSULDEMINAS_2026_2`, aba Horários. O leitor
utiliza Sala / Local, Dia da semana, Início, Fim, Turma e Disciplina e docente
(texto). Linhas com sala não atribuída são ignoradas; horários/dias inválidos ou
colunas de grade incompletas tornam a fonte indisponível (503), em vez de indicar
uma sala falsamente livre. Linhas idênticas são consolidadas. O texto de turma e
disciplina é preservado conforme a fonte; incorreções de extração do PDF precisam
ser corrigidas na própria planilha.

Referência do calendário: PDF `calendario-ascom-17-3.pdf`, enviado pelo usuário,
Campus Machado, ano 2026, revisão de 17/03/2026. O 2º semestre começa em 27/07 e
termina em 18/12. A grade atual só cobre esse semestre; não é aplicada retroativamente
às aulas de 2026/1. Para datas após o término das aulas, mas ainda no restante de
2026, não há recorrência automática das aulas regulares.

`src/main/resources/calendar/academic-2026.csv` contém a validade, período,
recessos/feriados e dias letivos de referência. Inclui 07/09 como letivo conforme
o PDF, e os sábados letivos conforme o dia da semana equivalente indicado no
calendário. Atividades extraordinárias sem horários/salas informados, como exames
finais, não recebem alocação inventada pelo sistema.

Feriado/recesso significa ausência de bloqueio por aula regular, não fechamento
operacional obrigatório do campus. Reservas aprovadas continuam ocupando seus
horários nessas datas. Nenhuma reserva aprovada antiga é cancelada automaticamente
por uma atualização de grade; conflitos passam a aparecer na disponibilidade e
novas aprovações são recusadas.

## Atualizar no próximo ano ou semestre

1. Atualizar a planilha com salas, dias e horários reais do novo período.
2. Criar o CSV do novo calendário a partir da versão aprovada pela instituição.
3. Atualizar CALENDAR_LOCATION no backend (padrão atual:
   `classpath:calendar/academic-2026.csv`) e, se necessário, ROOMS_LOCATION.
4. Reiniciar/republicar o backend para carregar o novo calendário. A leitura da
   planilha já possui cache configurável, padrão 5 minutos, e reflete alterações
   da fonte após expirar. Não se exige duplicar aulas/salas no PostgreSQL.
5. Conferir uma data letiva, feriado, sábado letivo e os limites do período.

Formato do calendário:

```csv
start,end,type,weekday
2027-01-01,2027-12-31,COVERAGE,
```

O exemplo acima mostra somente a sintaxe da cobertura: para um calendário real,
**adicione TERM com início/fim aprovados**, NO_CLASS para exceções e TEACHING_DAY
para equivalências de dia. Não foram presumidas datas letivas de 2027.

- COVERAGE: datas cuja grade/calendário foram configurados e podem ser consultados.
- TERM: intervalo em que as aulas regulares se repetem de segunda a sexta.
- NO_CLASS: feriado/recesso, inclusive nas extremidades.
- TEACHING_DAY: aplica um dia equivalente (MONDAY a SUNDAY), como sábado letivo.

Aulas fora da cobertura configurada retornam 409 com orientação para atualizar o
período. Assim uma grade vencida não é extrapolada silenciosamente para outro ano.
Calendários de anos/períodos diferentes podem ser reunidos no mesmo arquivo se as
grade(s) correspondentes também estiverem corretamente configuradas; a versão
atual da fonte deve representar o período consultado, sem misturar semestres com
horários diferentes. Não configurar a cobertura além da validade real da grade.

## Cadastro e testes

CPF é mascarado como 000.000.000-00, mas somente números são enviados ao backend.
Dígitos verificadores continuam obrigatórios; a máscara não torna CPF inválido
válido. Cadastro exige confirmação de CPF e senha; os dois campos de confirmação
não são enviados nem persistidos. Login e cadastro permitem mostrar/ocultar senha.

Testes: `npm --prefix frontend test` para máscara/confirmações;
`./mvnw clean verify` para leitura de grade, calendário, conflitos e demais APIs.

## Consulta de salas e horários no celular

Clicar numa sala carrega automaticamente a agenda do dia, em ordem cronológica,
com intervalos livres, aulas e reservas. É possível mudar a data e usar um
intervalo livre para preencher o formulário de reserva. Aulas do dia mostram os
dados da planilha. A navegação e as listagens se adaptam a telas pequenas.

A aba **Horários de aulas** permite buscar curso/turma e consultar as 55 páginas
do PDF de 16/11/2026 enviado pela instituição. A visualização tem ampliação,
rolagem, texto extraído e download do documento completo. O PDF é carregado
apenas ao abrir essa aba. Conforme confirmado pelo usuário, a planilha foi feita
a partir desses horários e continua sendo a fonte dos bloqueios de sala.

Para atualizar a cópia de consulta no próximo período, execute na raiz:

```bash
python3 scripts/update-timetable.py '/caminho/novo-horario.pdf' --label '2027/1 · revisão atual'
npm --prefix frontend run build
```

O script exige `pdftotext` (pacote Poppler), copia o PDF para
`frontend/public/horarios/horario-atual.pdf` e gera o índice de turmas em
`frontend/src/data/timetable-index.json`. Confira os nomes extraídos antes de
publicar o frontend. Atualize também a planilha e o calendário do backend pelas
etapas acima; atualizar somente o PDF de consulta não altera os bloqueios.

## Período disponível para reservas

A agenda considera somente **07:00 a 23:00**, no fuso America/Sao_Paulo.
A madrugada não aparece como intervalo livre. Novas solicitações e aprovações
fora dessa faixa ou atravessando dias são recusadas pelo backend. Reservas
legadas são preservadas, mas a exibição dos intervalos ocupados é limitada à
faixa de funcionamento. A ausência de aulas significa disponibilidade entre
07h e 23h, respeitando reservas aprovadas.

O índice de consulta do PDF também contém a altura útil de cada página,
extraída das posições do texto pelo script de atualização. A visualização
recorta apenas o espaço vazio inferior, incluindo a última linha e o rodapé;
o PDF completo para abrir/baixar permanece original.
