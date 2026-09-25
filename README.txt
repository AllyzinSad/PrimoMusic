KODA CUT v0.2 - TESTE DE DESENVOLVIMENTO
==========================================

Windows 10 64-bit
Base de otimizacao:
- Ryzen 5 4500
- 16 GB RAM (2x8 DDR4)
- GTX 1660 Super
- SSD SATA 256 GB

IDEIA
-----
O Koda Cut e um editor automatico local.

Fluxo:
1. Voce manda os videos/arquivos para o ChatGPT.
2. O ChatGPT analisa o material e gera um KodaScript (JSON).
3. No Koda Cut voce adiciona:
   - video principal
   - videos extras / B-roll
   - PNGs, imagens, logos e memes
   - efeitos sonoros, musicas e outros audios
4. Cada arquivo recebe um ID.
5. Voce cola o KodaScript.
6. Clica EDITAR / RENDERIZAR.
7. O FFmpeg faz a edicao sozinho no seu PC.

Nao existe mensalidade ou credito do Koda Cut.
O processamento e local.

COMO ELE "RECONHECE" VIDEOS, FOTOS E AUDIOS
--------------------------------------------
O programa classifica o arquivo pelo formato e cria um ID estavel baseado
no nome do arquivo.

Exemplos:

boom.wav
=> audio:boom

logo Koda.png
=> image:logo_koda

broll praia.mp4
=> video:broll_praia

personagem assustado.png
=> image:personagem_assustado

O KodaScript referencia esses IDs.

Exemplo:
{
  "inicio": 12.4,
  "fim": 13.5,
  "texto": "NOSSA???",
  "elementos": [
    {
      "asset": "image:personagem_assustado",
      "posicao": "bottom-left",
      "largura": 380
    }
  ],
  "audios": [
    {
      "asset": "audio:boom",
      "at": 0,
      "volume": 0.7
    }
  ]
}

Assim o editor nao precisa adivinhar qual "boom" ou qual imagem usar:
o comando aponta exatamente para o arquivo.

IMPORTANTE
----------
O v0.2 NAO contem um modelo de IA rodando dentro do programa.
O "cerebro" que decide onde cortar, qual meme usar e em qual segundo
continua sendo o ChatGPT.

Isso deixa o programa leve, gratuito e adequado ao seu PC.

O Koda Cut executa as instrucoes localmente.

PRIMEIRO USO
-------------
1. Extraia o ZIP.
2. Tenha JDK 17 ou superior instalado.
3. Abra ABRIR-KODACUT.bat.
4. No Koda Cut clique CONFIGURAR FFmpeg.
5. Aguarde o download do FFmpeg.
6. Pronto.

TESTE SEM COMPILAR
------------------
Abra:
ABRIR-KODACUT.bat

Ele compila o codigo Java e executa.

GERAR O .EXE PORTATIL
---------------------
Execute:
BUILD-PORTABLE.bat

Resultado:
dist\Koda Cut\Koda Cut.exe

O build usa jpackage do JDK.

ABAS DO APP
-----------
PROJETO
- selecionar video principal
- escolher horizontal / vertical / ambas
- vertical com blur ou crop
- qualidade
- colar prompt/KodaScript
- renderizar

ARQUIVOS / ELEMENTOS
- adicionar varios arquivos de uma vez
- video
- imagem
- PNG transparente
- audio
- SFX
- musica
- excluir arquivos
- copiar mapa de arquivos para o ChatGPT

CONSOLE
- acompanha FFmpeg e erros

FORMATOS RECONHECIDOS
---------------------
Video:
mp4, mov, mkv, webm, avi, m4v, wmv, ts

Imagem:
png, jpg, jpeg, webp, bmp, gif

Audio:
mp3, wav, m4a, aac, flac, ogg, opus, wma

SAIDAS
------
Horizontal:
1920x1080 / 60 FPS

Vertical:
1080x1920 / 60 FPS

Ambos:
gera os dois arquivos.

No vertical:
- blur preserva o video inteiro e coloca fundo borrado
- crop ocupa a tela toda cortando as laterais

MOTOR
-----
FFmpeg 64-bit.

O Koda Cut tenta usar:
h264_nvenc

na GTX 1660 Super.

Se NVENC nao estiver disponivel, usa libx264 na CPU.

QUALIDADE
---------
ECO
Menor carga.

BALANCEADO
Preset recomendado.

QUALIDADE
Render mais pesado.

KODASCRIPT v2
-------------
Estrutura basica:

{
  "version": 2,
  "fps": 60,
  "duracao_saida": 0,

  "audio": {
    "reduzir_ruido": true,
    "normalizar": true,
    "volume": 1.0
  },

  "musica": {
    "asset": "audio:minha_trilha",
    "volume": 0.08
  },

  "timeline": [
    {
      "inicio": 5.0,
      "fim": 8.0,
      "texto": "EXEMPLO",
      "texto_posicao": "top",
      "zoom": 1.12,
      "freeze": false,

      "elementos": [
        {
          "asset": "image:logo",
          "posicao": "top-right",
          "largura": 220,
          "opacity": 1.0
        },

        {
          "asset": "video:broll",
          "posicao": "full",
          "modo": "cover",
          "source_start": 3.0,
          "opacity": 1.0
        }
      ],

      "audios": [
        {
          "asset": "audio:impacto",
          "at": 0.2,
          "source_start": 0,
          "volume": 0.7,
          "duracao": 1.0
        }
      ]
    }
  ]
}

ELEMENTOS VISUAIS
-----------------
asset:
ID do arquivo.

posicao:
bottom-left
bottom-right
bottom-center
top-left
top-right
center
full

modo:
fit
fit-full
cover

largura:
largura do overlay em pixels.

opacity:
0.0 a 1.0.

source_start:
para videos extras, segundo do arquivo de onde comeca.

loop:
true para repetir o video extra se necessario.

AUDIO
-----
Dentro de "audios":

asset:
ex. audio:boom

at:
deslocamento em segundos a partir do inicio do evento.

volume:
ex. 0.70

source_start:
ponto inicial dentro do arquivo de audio.

duracao:
opcional. 0 deixa o audio seguir ate terminar.

MUSICA
------
A musica tambem e apenas um asset de audio:

"musica": {
  "asset": "audio:trilha",
  "volume": 0.08
}

COPYRIGHT
---------
O Koda Cut nao afirma que qualquer arquivo adicionado e livre de copyright.
Voce controla os arquivos que entram na biblioteca.

Se o ChatGPT recomendar uma trilha/SFX, prefira arquivos que voce tenha
licenca para usar ou que sejam explicitamente royalty-free.

Os pequenos SFX gerados por CONFIGURAR.bat sao sintetizados localmente.

SSD DE 256 GB
-------------
O motor evita criar copias intermediarias gigantes.
A renderizacao principal e feita em uma passagem sempre que possivel.
A pasta temp e limpa depois da renderizacao.

PASTAS
------
biblioteca\videos
biblioteca\imagens
biblioteca\audios
projetos
final
scripts
ffmpeg
temp

VERSAO
------
v0.2
- transformado de editor DDtank em editor geral
- biblioteca de videos, imagens e audios
- IDs automaticos
- mapa de arquivos para copiar para o ChatGPT
- overlays de imagem
- B-roll / video extra
- audios e SFX por timestamp
- musica por asset
- horizontal e vertical
- NVENC
