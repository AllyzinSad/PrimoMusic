KODA CUT v0.1 - MVP
===================

Editor automatico local do ecossistema Koda.

ALVO DESTE BUILD
----------------
Windows 10 64-bit
Ryzen 5 4500
16 GB RAM DDR4
GTX 1660 Super
SSD SATA 256 GB

IDEIA
-----
1. Voce envia o gameplay para o ChatGPT.
2. O ChatGPT analisa e devolve um JSON de edicao.
3. Voce abre o Koda Cut.
4. Seleciona o video.
5. Cola o JSON.
6. Escolhe Horizontal, Vertical ou As duas versoes.
7. Clica RENDERIZAR.
8. O FFmpeg faz a edicao localmente no seu PC.

NAO USA CREDITOS DE IA PARA RENDERIZAR.
A edicao e executada pelo seu proprio computador.

PRIMEIRO TESTE
--------------
1. Extraia o ZIP.
2. Tenha JDK 17 ou superior instalado.
3. Execute ABRIR-KODACUT.bat.
4. Clique CONFIGURAR.
5. Aguarde o download automatico do FFmpeg.
6. Coloque seus PNGs na pasta pngtuber.
7. Selecione um video.
8. Cole o JSON do ChatGPT.
9. Escolha o formato.
10. Clique RENDERIZAR.

FORMATOS
--------
Horizontal:
1920x1080, 60 FPS.
Ideal para YouTube.

Vertical:
1080x1920, 60 FPS.
Ideal para Reels, Stories, TikTok e Shorts.

As duas versoes:
gera os dois MP4 automaticamente.

VERTICAL - PRESERVAR GAMEPLAY
-----------------------------
Usa o gameplay inteiro no centro e cria um fundo borrado.
E o modo recomendado para jogos com HUD, como DDtank.

VERTICAL - CROP CENTRAL
-----------------------
Preenche toda a tela 9:16 cortando as laterais.
Use apenas quando a acao importante estiver no centro.

GPU
---
O Koda Cut procura h264_nvenc automaticamente.
Com a GTX 1660 Super, a codificacao H.264 usa o encoder dedicado NVIDIA.
Se o FFmpeg nao encontrar NVENC, ele usa CPU com libx264.

PRESETS
-------
ECO:
menor uso do PC.

BALANCEADO:
padrao recomendado para Ryzen 5 4500 + GTX 1660 Super.

QUALIDADE:
mais pesado e com compressao de melhor qualidade.

AUDIO
-----
O JSON aceita:
reduzir_ruido
normalizar
volume

A limpeza usa filtros locais do FFmpeg.

EFEITOS
-------
O CONFIGURAR gera localmente:
impacto.wav
whoosh.wav
tick.wav
scratch.wav
erro.wav

Eles sao sintetizados no seu computador e nao sao copiados de musicas ou videos.

MUSICA
------
Por padrao o Koda Cut NAO adiciona musica.
Se quiser trilha, coloque um arquivo que voce tenha permissao de usar em:
musicas\

Depois informe o caminho no JSON.

Isso foi escolhido para reduzir risco de Content ID.

PNG TUBER
---------
Coloque suas reacoes em:
pngtuber\

Exemplos:
thinking.png
power.png
shocked.png
stressed.png
happy.png
tired.png

Se o JSON pedir um PNG que nao existe, o programa avisa e continua o render sem ele.

EVENTOS JSON
------------
Cada evento pode usar:
inicio
fim
texto
texto_posicao
font_size
png
png_posicao
png_largura
zoom
freeze
sfx
sfx_volume

Exemplo:

{
  "inicio": 7.0,
  "fim": 8.5,
  "texto": "AGORA VAI",
  "png": "pngtuber\\power.png",
  "zoom": 1.28,
  "sfx": "impacto"
}

COMPILAR
--------
ABRIR-KODACUT.bat:
compila e abre para teste.

BUILD-PORTABLE.bat:
compila o Java, cria o JAR e usa jpackage para gerar:

dist\Koda Cut\Koda Cut.exe

O tipo app-image inclui o runtime Java, entao o portatil final nao depende do usuario ter Java instalado.

DEPENDENCIAS
------------
Para desenvolver/compilar:
JDK 17+.

Para editar:
FFmpeg 64-bit.
O CONFIGURAR baixa o FFmpeg automaticamente.

ESTRUTURA
---------
KodaCut/
  ABRIR-KODACUT.bat
  CONFIGURAR.bat
  BUILD-PORTABLE.bat
  TESTAR-NVENC.bat
  LIMPAR-CACHE.bat
  README.txt
  assets/
    logo.svg
  src/
    kodacut/
      KodaCut.java
  scripts/
    configurar.ps1
    editor.ps1
  projetos/
    exemplo-ddtank.json
  videos/
  pngtuber/
  musicas/
  efeitos/
  final/
  ffmpeg/
    bin/

OBSERVACAO
----------
Esta e a versao 0.1 MVP.
A prioridade e validar o fluxo automatico antes de adicionar recursos mais pesados.
