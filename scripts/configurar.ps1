param()

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Bin = Join-Path $Root "ffmpeg\bin"
$Tools = Join-Path $Root "tools"
$WhisperDir = Join-Path $Tools "whisper"
$Models = Join-Path $WhisperDir "models"
$Temp = Join-Path $Root "temp"
$Effects = Join-Path $Root "efeitos"

$dirs = @(
    $Bin,$Tools,$WhisperDir,$Models,$Temp,$Effects,
    (Join-Path $Root "videos"),
    (Join-Path $Root "pngtuber"),
    (Join-Path $Root "musicas"),
    (Join-Path $Root "projetos"),
    (Join-Path $Root "final"),
    (Join-Path $Root "final\cortes"),
    (Join-Path $Root "downloads"),
    (Join-Path $Root "transcricoes"),
    (Join-Path $Root "biblioteca\videos"),
    (Join-Path $Root "biblioteca\imagens"),
    (Join-Path $Root "biblioteca\audios")
)
New-Item -ItemType Directory -Force -Path $dirs | Out-Null

function Download-File([string]$Url, [string]$Destination) {
    Write-Host ("Baixando: " + $Url) -ForegroundColor DarkGray
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $Destination
}

$ffmpeg = Join-Path $Bin "ffmpeg.exe"
$ffprobe = Join-Path $Bin "ffprobe.exe"

if (-not (Test-Path $ffmpeg) -or -not (Test-Path $ffprobe)) {
    Write-Host ""
    Write-Host "[Koda Cut] Baixando FFmpeg 64-bit..." -ForegroundColor Cyan

    $zip = Join-Path $Temp "ffmpeg.zip"
    $extract = Join-Path $Temp "ffmpeg_extract"
    if (Test-Path $extract) { Remove-Item $extract -Recurse -Force }

    Download-File "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip" $zip
    Expand-Archive -Path $zip -DestinationPath $extract -Force

    $f1 = Get-ChildItem $extract -Recurse -Filter "ffmpeg.exe" | Select-Object -First 1
    $f2 = Get-ChildItem $extract -Recurse -Filter "ffprobe.exe" | Select-Object -First 1

    if (-not $f1 -or -not $f2) {
        throw "Nao foi possivel localizar ffmpeg.exe e ffprobe.exe."
    }

    Copy-Item $f1.FullName $ffmpeg -Force
    Copy-Item $f2.FullName $ffprobe -Force
    Write-Host "[OK] FFmpeg instalado dentro do Koda Cut." -ForegroundColor Green
} else {
    Write-Host "[OK] FFmpeg ja esta configurado." -ForegroundColor Green
}

$yt = Join-Path $Tools "yt-dlp.exe"
if (-not (Test-Path $yt)) {
    Write-Host "[Koda Cut] Baixando yt-dlp para importacao de conteudo autorizado..." -ForegroundColor Cyan
    Download-File "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe" $yt
    Write-Host "[OK] yt-dlp instalado." -ForegroundColor Green
} else {
    Write-Host "[OK] yt-dlp ja esta configurado." -ForegroundColor Green
}

$whisper = Get-ChildItem $WhisperDir -Recurse -Filter "whisper-cli.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $whisper) {
    $whisper = Get-ChildItem $WhisperDir -Recurse -Filter "main.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
}

if (-not $whisper) {
    Write-Host "[Koda Cut] Baixando Whisper local para Windows x64..." -ForegroundColor Cyan
    $headers = @{ "User-Agent" = "KodaCut" }
    $releases = Invoke-RestMethod -Headers $headers -Uri "https://api.github.com/repos/ggml-org/whisper.cpp/releases?per_page=12"

    $asset = $null
    foreach ($release in $releases) {
        $candidate = $release.assets | Where-Object { $_.name -eq "whisper-bin-x64.zip" } | Select-Object -First 1
        if ($candidate) {
            $asset = $candidate
            Write-Host ("[Koda Cut] Whisper build: " + $release.tag_name) -ForegroundColor DarkGray
            break
        }
    }

    if (-not $asset) {
        throw "Nao encontrei o pacote whisper-bin-x64.zip nas releases recentes do whisper.cpp."
    }

    $zip = Join-Path $Temp "whisper.zip"
    $extract = Join-Path $Temp "whisper_extract"
    if (Test-Path $extract) { Remove-Item $extract -Recurse -Force }
    Download-File $asset.browser_download_url $zip
    Expand-Archive -Path $zip -DestinationPath $extract -Force

    Get-ChildItem $extract -Force | ForEach-Object {
        Copy-Item $_.FullName $WhisperDir -Recurse -Force
    }

    $whisper = Get-ChildItem $WhisperDir -Recurse -Filter "whisper-cli.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $whisper) {
        $whisper = Get-ChildItem $WhisperDir -Recurse -Filter "main.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    }
    if (-not $whisper) {
        throw "Whisper foi baixado, mas o executavel nao foi localizado."
    }
    Write-Host ("[OK] Whisper instalado: " + $whisper.FullName) -ForegroundColor Green
} else {
    Write-Host ("[OK] Whisper ja esta configurado: " + $whisper.FullName) -ForegroundColor Green
}

$model = Join-Path $Models "ggml-base.bin"
if (-not (Test-Path $model)) {
    Write-Host "[Koda Cut] Baixando modelo Whisper base..." -ForegroundColor Cyan
    Download-File "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin" $model
    Write-Host "[OK] Modelo Whisper instalado." -ForegroundColor Green
} else {
    Write-Host "[OK] Modelo Whisper ja esta instalado." -ForegroundColor Green
}

Write-Host "[Koda Cut] Gerando SFX originais..." -ForegroundColor Cyan

& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=72:duration=0.34:sample_rate=48000" -af "volume=0.9,afade=t=out:st=0.05:d=0.29" (Join-Path $Effects "impacto.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=color=pink:duration=0.42:sample_rate=48000" -af "highpass=f=450,lowpass=f=6000,volume=0.32,afade=t=in:st=0:d=0.05,afade=t=out:st=0.20:d=0.22" (Join-Path $Effects "whoosh.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=1850:duration=0.06:sample_rate=48000" -af "volume=0.42,afade=t=out:st=0.02:d=0.04" (Join-Path $Effects "tick.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "aevalsrc=0.22*sin(2*PI*(1700-4000*t)*t):s=48000:d=0.30" -af "afade=t=out:st=0.15:d=0.15" (Join-Path $Effects "scratch.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=210:duration=0.23:sample_rate=48000" -af "volume=0.5,afade=t=out:st=0.10:d=0.13" (Join-Path $Effects "erro.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=520:duration=0.12:sample_rate=48000" -af "volume=0.35,afade=t=out:st=0.04:d=0.08" (Join-Path $Effects "pop.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=color=white:duration=0.16:sample_rate=48000" -af "highpass=f=1400,lowpass=f=7600,volume=0.18,afade=t=out:st=0.05:d=0.11" (Join-Path $Effects "snap.wav")

$DefaultAudioLibrary = Join-Path $Root "biblioteca\audios"
New-Item -ItemType Directory -Force -Path $DefaultAudioLibrary | Out-Null
Get-ChildItem $Effects -Filter "*.wav" | ForEach-Object { Copy-Item $_.FullName (Join-Path $DefaultAudioLibrary $_.Name) -Force }

$enc = (& $ffmpeg -hide_banner -encoders 2>&1 | Out-String)
if ($enc -match "h264_nvenc") {
    Write-Host "[OK] NVIDIA NVENC encontrado." -ForegroundColor Green
} else {
    Write-Host "[AVISO] NVENC nao apareceu. O Koda Cut usara CPU ate o driver/encoder estar disponivel." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Configuracao concluida." -ForegroundColor Green
Write-Host "FFmpeg, yt-dlp, Whisper e SFX estao prontos." -ForegroundColor Green
