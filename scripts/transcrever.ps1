param(
    [Parameter(Mandatory=$true)][string]$Video,
    [ValidateSet("pt","auto","en","es")][string]$Language = "pt",
    [Parameter(Mandatory=$true)][string]$OutputDir
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$FFmpeg = Join-Path $Root "ffmpeg\bin\ffmpeg.exe"
$WhisperDir = Join-Path $Root "tools\whisper"
$Model = Join-Path $WhisperDir "models\ggml-base.bin"
$Temp = Join-Path $Root "temp"

if (-not (Test-Path $Video)) { throw "Video nao encontrado: $Video" }
if (-not (Test-Path $FFmpeg)) { throw "FFmpeg nao configurado. Execute CONFIGURAR.bat." }
if (-not (Test-Path $Model)) { throw "Modelo Whisper nao configurado. Execute CONFIGURAR.bat." }

$Whisper = Get-ChildItem $WhisperDir -Recurse -Filter "whisper-cli.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $Whisper) {
    $Whisper = Get-ChildItem $WhisperDir -Recurse -Filter "main.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
}
if (-not $Whisper) { throw "Executavel do Whisper nao encontrado. Execute CONFIGURAR.bat." }

New-Item -ItemType Directory -Force -Path $OutputDir,$Temp | Out-Null

$stem = [IO.Path]::GetFileNameWithoutExtension($Video)
$safeStem = ($stem -replace '[^\p{L}\p{Nd}\-_ ]','').Trim()
if ([string]::IsNullOrWhiteSpace($safeStem)) { $safeStem = "transcricao" }

$wav = Join-Path $Temp ("whisper_" + [Guid]::NewGuid().ToString("N") + ".wav")
$outBase = Join-Path $OutputDir $safeStem
$srt = $outBase + ".srt"

Write-Host "[Whisper] Extraindo audio..." -ForegroundColor Cyan
& $FFmpeg -hide_banner -loglevel error -y -i $Video -vn -ac 1 -ar 16000 -c:a pcm_s16le $wav
if ($LASTEXITCODE -ne 0) { throw "Falha ao extrair audio para o Whisper." }

Write-Host "[Whisper] Transcrevendo localmente com modelo base..." -ForegroundColor Cyan
$args = @(
    "-m", $Model,
    "-f", $wav,
    "-l", $Language,
    "-osrt",
    "-of", $outBase,
    "-pp"
)

& $Whisper.FullName @args
$code = $LASTEXITCODE
Remove-Item $wav -Force -ErrorAction SilentlyContinue

if ($code -ne 0) { throw "Whisper terminou com erro: $code" }
if (-not (Test-Path $srt)) { throw "Whisper terminou, mas o arquivo SRT nao foi criado." }

Write-Host ("[OK] Transcricao: " + $srt) -ForegroundColor Green
Write-Host ("KODACUT_SRT=" + $srt)
