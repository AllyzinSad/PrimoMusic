param(
    [Parameter(Mandatory=$true)][string]$Url,
    [Parameter(Mandatory=$true)][string]$OutputDir
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Yt = Join-Path $Root "tools\yt-dlp.exe"
$FFmpegDir = Join-Path $Root "ffmpeg\bin"

if (-not (Test-Path $Yt)) { throw "yt-dlp nao configurado. Execute CONFIGURAR.bat." }
if (-not (Test-Path (Join-Path $FFmpegDir "ffmpeg.exe"))) { throw "FFmpeg nao configurado. Execute CONFIGURAR.bat." }
if ($Url -notmatch '^https?://') { throw "Link invalido." }

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

Write-Host "[Koda Cut] Importando video autorizado..." -ForegroundColor Cyan
Write-Host "Use este recurso apenas para conteudo que voce tenha direito/permissao para reutilizar." -ForegroundColor Yellow

$template = Join-Path $OutputDir "%(title).120B [%(id)s].%(ext)s"
$args = @(
    "--no-playlist",
    "--ffmpeg-location", $FFmpegDir,
    "-f", "bv*+ba/b",
    "--merge-output-format", "mp4",
    "--windows-filenames",
    "-o", $template,
    "--print", "after_move:filepath",
    $Url
)

$result = & $Yt @args
if ($LASTEXITCODE -ne 0) { throw "Falha ao importar o video." }

$path = ($result | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 1).Trim()
if (-not $path -or -not (Test-Path $path)) {
    $path = Get-ChildItem $OutputDir -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
}

if (-not $path -or -not (Test-Path $path)) { throw "Download terminou, mas o arquivo nao foi localizado." }

Write-Host ("[OK] Importado: " + $path) -ForegroundColor Green
Write-Host ("KODACUT_OUTPUT=" + $path)
