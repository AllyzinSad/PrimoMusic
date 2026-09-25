KODA CONNECT - BETA 0.1

OBJETIVO
Koda Connect e a ponte local entre um assistente de IA e o motor de edicao do Koda Cut.

FOCO DO BETA
- sem login
- sem licenca
- sem pagamento
- servidor local automatico
- workspace local autorizada
- receber pedidos de edicao
- iniciar render
- informar o progresso da edicao
- devolver o caminho do resultado

COMO TESTAR
1. Extraia o projeto.
2. Execute ABRIR-KODA-CONNECT.bat.
3. O app abre e sobe automaticamente em:
   http://127.0.0.1:17777
4. Clique em TESTAR CONEXAO LOCAL.
5. Clique em ABRIR WORKSPACE.
6. Coloque os arquivos nas pastas:
   videos
   images
   audio
   music
   broll
7. O app passa a listar apenas esses arquivos autorizados.

API LOCAL DO BETA
GET  /health
GET  /api/status
GET  /api/files
GET  /api/projects
POST /api/projects        corpo = KodaScript JSON
POST /api/render          corpo = JSON simples com video e project
GET  /api/jobs?id=...
POST /api/cancel?id=...

EXEMPLO DE POST /api/render
{
  "video": "meu-video.mp4",
  "project": "project-123.json",
  "outputMode": "horizontal",
  "verticalMode": "blur",
  "quality": "balanceado",
  "captionMode": "destaques",
  "language": "pt",
  "style": "clean"
}

STATUS
O Koda Connect informa etapas como:
- Preparando projeto
- Analisando e preparando edicao
- Transcrevendo audio
- Aplicando legendas
- Renderizando video
- Finalizando arquivo
- Concluido

IMPORTANTE
Este beta prova a ponte LOCAL e o controle do motor de edicao.
Para um chat hospedado fora do PC acessar 127.0.0.1 sera necessaria uma ponte segura/remota.
Essa parte sera integrada depois, sem obrigar o usuario a configurar servidor manualmente.
