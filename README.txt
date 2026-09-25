KODA CUT v0.3 - AUTOMATIC VIDEO EDITOR
=========================================

IDENTIDADE
----------
Koda Cut faz parte do ecossistema Koda.
Visual oficial:
- preto + branco
- K estilizado
- simbolo de PLAY integrado
- corte diagonal simbolizando edicao

PLATAFORMA / BASE DE OTIMIZACAO
-------------------------------
Windows 10 64-bit

PC de referencia:
- Ryzen 5 4500
- 16 GB RAM (2x8 GB DDR4)
- GTX 1660 Super
- SSD SATA 256 GB

O QUE E
-------
Koda Cut e um editor automatico local.

O objetivo e simples:

1. Voce adiciona o video principal.
2. Adiciona PNGs, fotos, logos, memes, B-rolls, musicas e SFX.
3. O Koda Cut cria um ID para cada arquivo.
4. Voce copia o mapa de arquivos para o ChatGPT.
5. O ChatGPT devolve um KodaScript JSON dizendo exatamente o que fazer.
6. Voce cola o KodaScript no Koda Cut.
7. Clica EDITAR / RENDERIZAR.
8. FFmpeg, Whisper e NVENC executam a edicao no seu PC.

Nao existe mensalidade ou credito de renderizacao do Koda Cut.
O processamento e local.

IMPORTANTE SOBRE "PROMPT"
-------------------------
Na EDICAO AUTOMATICA, o campo aceita o KodaScript JSON que o ChatGPT gera.
Ele tambem aceita o JSON dentro de um bloco de codigo, porque o programa
extrai o primeiro objeto JSON encontrado.

O Koda Cut v0.3 NAO possui um modelo de linguagem embutido para interpretar
qualquer texto livre como o ChatGPT faria.

No modo CANAL DE CORTES existe interpretacao local simples de instrucoes como:
- "gere 8 cortes"
- "40 a 70 segundos"
- "vertical"
- "legenda completa"
- "somente legendas de destaque"

Para uma edicao detalhada e criativa, o fluxo recomendado continua sendo:
material -> ChatGPT -> KodaScript -> Koda Cut.

BARRA LATERAL RETRATIL
----------------------
A versao v0.3 possui:

- Inicio
- Edicao Automatica
- Canal de Cortes
- Gameplay
- Dark / Narrado
- Podcast / Cortes
- Shorts / Reels
- Elementos
- Legendas
- Audio
- Formato
- Estilos
- Lote
- Renderizacao
- Configuracoes

Use o botao da barra para recolher e deixar apenas os atalhos.

FORMATOS DE SAIDA
-----------------
Horizontal:
1920x1080 / 60 FPS
YouTube e video tradicional.

Vertical:
1080x1920 / 60 FPS
Reels, Stories, TikTok e Shorts.

Quadrado:
1080x1080 / 60 FPS.

Tambem e possivel gerar:
- horizontal + vertical
- todos os formatos

VERTICAL
--------
Preservar video + fundo borrado:
mantem o quadro inteiro e preenche 9:16 com fundo desfocado.
E o modo recomendado para gameplay/HUD.

Crop central:
preenche o 9:16 cortando as laterais.

Safe zone:
desloca overlays e legendas para evitar as principais areas de botoes
das plataformas verticais.

LEGENDAS LOCAIS
----------------
O Koda Cut usa whisper.cpp localmente.

Modos:
- Desligada
- Completa
- Destaques
- Palavra por palavra

Completa:
queima todas as frases reconhecidas no video.

Destaques:
seleciona localmente frases com mais sinais de impacto/reacao.
O algoritmo considera pontuacao, expressoes, tamanho de frase e outros sinais.
E um modo heuristico, nao uma analise semantica completa de IA.

Palavra por palavra:
gera ASS/Karaoke local e destaca cada palavra durante a frase.

Perfis sugeridos:
Gameplay / Meme -> Destaques
Dark / Narrado -> Completa
Podcast / Cortes -> Completa
Shorts / Reels -> Completa
Cinematico -> Desligada

AUDIO
-----
- filtro high-pass/low-pass
- reducao de ruido FFmpeg
- normalizacao dinamica
- controle do volume da voz
- musica por asset
- SFX por timestamp
- ducking automatico: abaixa a musica enquanto existe voz
- limitador final

ELEMENTOS / ASSETS
------------------
Formatos reconhecidos:

Video:
mp4, mov, mkv, webm, avi, m4v, wmv, ts

Imagem:
png, jpg, jpeg, webp, bmp, gif

Audio:
mp3, wav, m4a, aac, flac, ogg, opus, wma

Exemplos de IDs:

boom.wav
=> audio:boom

logo Koda.png
=> image:logo_koda

praia.mp4
=> video:praia

personagem assustado.png
=> image:personagem_assustado

Use COPIAR MAPA P/ CHATGPT para copiar todos os IDs.

KODASCRIPT v3
-------------
Exemplo:

{
  "version": 3,
  "fps": 60,
  "duracao_saida": 0,

  "musica": {
    "asset": "audio:trilha",
    "volume": 0.08
  },

  "timeline": [
    {
      "inicio": 12.4,
      "fim": 13.8,

      "texto": "NAO E POSSIVEL",
      "texto_posicao": "center",

      "zoom": 1.22,
      "shake": 8,
      "freeze": true,

      "elementos": [
        {
          "asset": "image:assustado",
          "posicao": "bottom-left",
          "largura": 400,
          "opacity": 1.0
        }
      ],

      "audios": [
        {
          "asset": "audio:impacto",
          "at": 0.0,
          "volume": 0.75
        }
      ]
    }
  ]
}

RECURSOS DO KODASCRIPT
----------------------
Evento:
inicio
fim
texto
texto_posicao
font_size
zoom
shake
freeze
elementos
audios

Posicoes:
bottom-left
bottom-right
bottom-center
top-left
top-right
center
full

Elemento:
asset
posicao
modo
largura
opacity
source_start
loop

Modos:
fit
fit-full
cover

Audio:
asset
at
source_start
volume
duracao

CANAL DE CORTES
---------------
Entrada:
- link de video autorizado
OU
- arquivo local

O modo por link usa yt-dlp.

ANTES de importar um link, o programa exige confirmacao de que voce tem
permissao/direito para usar ou reutilizar o conteudo.

O Koda Cut nao foi feito para burlar DRM, controles de acesso ou direitos
autorais.

Fluxo:
1. importa video autorizado
2. transcreve com Whisper
3. cria varias janelas candidatas
4. pontua frases por sinais de impacto
5. evita cortes muito sobrepostos
6. renderiza os melhores trechos
7. adiciona legenda completa/destaques se selecionado
8. usa NVENC quando disponivel

O seletor de cortes e HEURISTICO/local. Ele nao substitui o julgamento do
ChatGPT para uma curadoria sofisticada.

PERFIS
------
Gameplay / Meme:
- legenda de destaques
- zoom, freeze e shake via KodaScript
- PNG/memes/SFX
- 1080p60

Dark / Narrado:
- legenda completa
- B-rolls
- musica baixa
- ducking
- visual menos agressivo

Podcast / Cortes:
- transcricao completa
- Canal de Cortes
- vertical/horizontal

Shorts / Reels:
- 9:16
- safe zones
- legenda grande
- fundo blur ou crop

LOTE
----
Adicione varios videos e use RENDERIZAR LOTE.
O mesmo KodaScript/configuracao e aplicado a cada arquivo em sequencia.

MOTOR / PERFORMANCE
-------------------
FFmpeg 64-bit.

Encoder preferencial:
h264_nvenc

A GTX 1660 Super faz a codificacao H.264 por hardware quando NVENC esta
disponivel.

Fallback:
libx264 na CPU.

Presets:
ECO
Balanceado
Qualidade

O modo Balanceado e o padrao recomendado para o PC de referencia.

SSD 256 GB
----------
- evita copias intermediarias grandes sempre que possivel
- usa pasta temp local
- limpa cache temporario
- ferramentas sao baixadas uma vez

FERRAMENTAS GRATUITAS
---------------------
CONFIGURAR FERRAMENTAS prepara:

- FFmpeg / FFprobe
- yt-dlp
- whisper.cpp Windows x64
- modelo Whisper base
- SFX sintetizados localmente

Os SFX padrao sao gerados pelo FFmpeg:
- impacto
- whoosh
- tick
- scratch
- erro
- pop
- snap

Depois de gerados, eles sao copiados para biblioteca\audios e recebem IDs.

MUSICAS / COPYRIGHT
-------------------
O Koda Cut nao declara automaticamente que uma musica adicionada pelo usuario
e livre de direitos autorais.

Use somente arquivos para os quais voce tenha permissao/licenca.

O Koda Cut nao baixa musica comercial automaticamente.

PRIMEIRO USO - CODIGO FONTE
---------------------------
1. Extraia o ZIP.
2. Instale um JDK 17 ou superior.
3. Execute ABRIR-KODACUT.bat.
4. Clique CONFIGURAR FERRAMENTAS.
5. Aguarde os downloads do primeiro uso.
6. Escolha seu fluxo na barra lateral.

TESTAR NVENC
------------
Execute:
TESTAR-NVENC.bat

Ou use a tela Renderizacao.

GERAR APP PORTATIL
------------------
Execute:
BUILD-PORTABLE.bat

O build:
- compila o codigo Java
- gera PNG/ICO oficial do Koda Cut
- cria o app-image com jpackage
- usa o icone oficial
- copia scripts, FFmpeg, ferramentas e recursos

Resultado:
dist\Koda Cut\Koda Cut.exe

LOGO / ICONE
-------------
Fontes vetoriais:
assets\koda-cut-icon.svg
assets\koda-cut-logo.svg

Na compilacao:
IconMaker.java gera:
assets\koda-cut-icon.png
assets\koda-cut.ico

O app tambem desenha o mesmo simbolo oficialmente via Java2D.

VALIDACAO
---------
O branch v0.3 inclui GitHub Actions para:
- compilar o Java no Windows com JDK 17
- gerar PNG/ICO
- validar sintaxe dos scripts PowerShell
- gerar JAR

A validacao de compilacao nao substitui o primeiro teste real de render no
hardware final, porque FFmpeg, driver NVIDIA, Whisper e codecs sao executados
localmente no PC.

PASTAS
------
assets
biblioteca\videos
biblioteca\imagens
biblioteca\audios
downloads
efeitos
ffmpeg
final
final\cortes
projetos
scripts
src
temp
tools
transcricoes

VERSAO
------
v0.3

Principais mudancas:
- identidade visual oficial Koda Cut
- icone oficial no app/EXE
- barra lateral retratil
- edicao automatica geral
- Canal de Cortes
- importacao autorizada por link
- Whisper local
- legenda completa
- legenda de destaques
- legenda palavra por palavra
- safe zones
- perfis Gameplay, Dark, Podcast e Shorts
- audio ducking
- shake
- quadrado 1:1
- gerar varias proporcoes
- lote
- biblioteca de assets com IDs
- SFX locais
- CI Windows
