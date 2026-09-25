param()

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Bin = Join-Path $Root "ffmpeg\bin"
$Temp = Join-Path $Root "temp"
$Effects = Join-Path $Root "efeitos"

New-Item -ItemType Directory -Force -Path $Bin,$Temp,$Effects,(Join-Path $Root "videos"),(Join-Path $Root "pngtuber"),(Join-Path $Root "musicas"),(Join-Path $Root "projetos"),(Join-Path $Root "final") | Out-Null

$ffmpeg = Join-Path $Bin "ffmpeg.exe"
$ffprobe = Join-Path $Bin "ffprobe.exe"

if (-not (Test-Path $ffmpeg) -or -not (Test-Path $ffprobe)) {
    Write-Host ""
    Write-Host "[Koda Cut] Baixando FFmpeg 64-bit..." -ForegroundColor Cyan

    $zip = Join-Path $Temp "ffmpeg.zip"
    $extract = Join-Path $Temp "ffmpeg_extract"
    if (Test-Path $extract) { Remove-Item $extract -Recurse -Force }

    Invoke-WebRequest -UseBasicParsing -Uri "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip" -OutFile $zip
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

Write-Host "[Koda Cut] Gerando SFX originais..." -ForegroundColor Cyan

& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=72:duration=0.34:sample_rate=48000" -af "volume=0.9,afade=t=out:st=0.05:d=0.29" (Join-Path $Effects "impacto.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "anoisesrc=color=pink:duration=0.42:sample_rate=48000" -af "highpass=f=450,lowpass=f=6000,volume=0.32,afade=t=in:st=0:d=0.05,afade=t=out:st=0.20:d=0.22" (Join-Path $Effects "whoosh.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=1850:duration=0.06:sample_rate=48000" -af "volume=0.42,afade=t=out:st=0.02:d=0.04" (Join-Path $Effects "tick.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "aevalsrc=0.22*sin(2*PI*(1700-4000*t)*t):s=48000:d=0.30" -af "afade=t=out:st=0.15:d=0.15" (Join-Path $Effects "scratch.wav")
& $ffmpeg -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=210:duration=0.23:sample_rate=48000" -af "volume=0.5,afade=t=out:st=0.10:d=0.13" (Join-Path $Effects "erro.wav")

$enc = (& $ffmpeg -hide_banner -encoders 2>&1 | Out-String)
if ($enc -match "h264_nvenc") {
    Write-Host "[OK] NVIDIA NVENC encontrado." -ForegroundColor Green
} else {
    Write-Host "[AVISO] NVENC nao apareceu. O Koda Cut usara CPU." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Configuracao concluida." -ForegroundColor Green
