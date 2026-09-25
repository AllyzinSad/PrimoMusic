param(
    [Parameter(Mandatory=$true)][string]$Video,
    [Parameter(Mandatory=$true)][string]$PromptFile,
    [int]$Count = 6,
    [int]$MinSeconds = 35,
    [int]$MaxSeconds = 70,
    [ValidateSet("vertical","horizontal")][string]$Format = "vertical",
    [ValidateSet("blur","crop")][string]$VerticalMode = "blur",
    [ValidateSet("off","completa","destaques")][string]$CaptionMode = "completa",
    [ValidateSet("pt","auto","en","es")][string]$Language = "pt",
    [ValidateSet("eco","balanceado","qualidade")][string]$Quality = "balanceado",
    [Parameter(Mandatory=$true)][string]$OutputDir
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$FFmpeg = Join-Path $Root "ffmpeg\bin\ffmpeg.exe"
$FFprobe = Join-Path $Root "ffmpeg\bin\ffprobe.exe"
$Transcriptions = Join-Path $Root "transcricoes"
$Temp = Join-Path $Root "temp"

function Num([double]$n) {
    return $n.ToString("0.###",[Globalization.CultureInfo]::InvariantCulture)
}

function Time-ToSeconds([string]$t) {
    $m = [regex]::Match($t.Trim(),'(?<h>\d{2}):(?<m>\d{2}):(?<s>\d{2})[,.](?<ms>\d{3})')
    if (-not $m.Success) { return 0.0 }
    return ([int]$m.Groups["h"].Value * 3600) +
           ([int]$m.Groups["m"].Value * 60) +
           [int]$m.Groups["s"].Value +
           ([int]$m.Groups["ms"].Value / 1000.0)
}

function Seconds-ToSrt([double]$sec) {
    if ($sec -lt 0) { $sec = 0 }
    $ts = [TimeSpan]::FromSeconds($sec)
    $hours = [Math]::Floor($ts.TotalHours)
    return ("{0:00}:{1:00}:{2:00},{3:000}" -f $hours,$ts.Minutes,$ts.Seconds,$ts.Milliseconds)
}

function Parse-Srt([string]$path) {
    $text = Get-Content $path -Raw -Encoding UTF8
    $blocks = [regex]::Split($text.Trim(),"\r?\n\r?\n+")
    $items = @()

    foreach ($block in $blocks) {
        $lines = $block -split "\r?\n"
        if ($lines.Count -lt 2) { continue }
        $timeIndex = 0
        if ($lines[0] -match '^\d+$') { $timeIndex = 1 }
        if ($timeIndex -ge $lines.Count) { continue }

        $tm = [regex]::Match($lines[$timeIndex],'(?<a>\d{2}:\d{2}:\d{2}[,.]\d{3})\s*-->\s*(?<b>\d{2}:\d{2}:\d{2}[,.]\d{3})')
        if (-not $tm.Success) { continue }

        $bodyStart = $timeIndex + 1
        if ($bodyStart -ge $lines.Count) { continue }
        $body = (($lines[$bodyStart..($lines.Count-1)] -join " ") -replace '<[^>]+>','').Trim()
        if ([string]::IsNullOrWhiteSpace($body)) { continue }

        $items += [pscustomobject]@{
            Start = Time-ToSeconds $tm.Groups["a"].Value
            End = Time-ToSeconds $tm.Groups["b"].Value
            Text = $body
        }
    }
    return @($items)
}

function Score-Text([string]$text, [string[]]$promptWords) {
    $score = 0.0
    if ($text -match '[!?]') { $score += 3.0 }
    if ($text -match '(?i)\b(nossa|caraca|mano|meu deus|impossivel|absurdo|segredo|verdade|mentira|nunca|sempre|olha|agora|espera|como|por que|porque|serio|acertou|errou|ganhou|perdeu|problema|dinheiro|resultado|erro|melhor|pior)\b') { $score += 4.0 }
    if ($text -match '(?i)(kkk|haha|rsrs)') { $score += 2.5 }
    if ($text -cmatch '[A-ZÁÉÍÓÚÃÕÇ]{4,}') { $score += 1.0 }

    $words = @($text -split '\s+' | Where-Object { $_ })
    if ($words.Count -ge 8 -and $words.Count -le 85) { $score += 1.5 }
    $score += [Math]::Min(2.0,$words.Count / 40.0)

    foreach ($pw in $promptWords) {
        if ($pw.Length -ge 5 -and $text -match [regex]::Escape($pw)) { $score += 0.8 }
    }
    return $score
}

function Escape-FilterPath([string]$p) {
    return $p.Replace("\","/").Replace(":","\:").Replace("'","\'")
}

function Write-SegmentSrt($entries, [double]$start, [double]$end, [string]$path, [string]$mode) {
    $selected = @($entries | Where-Object { $_.End -gt $start -and $_.Start -lt $end })
    if ($mode -eq "destaques") {
        $selected = @($selected | ForEach-Object {
            [pscustomobject]@{ Item=$_; Score=(Score-Text $_.Text @()) }
        } | Sort-Object Score -Descending | Select-Object -First ([Math]::Max(1,[Math]::Ceiling($selected.Count * 0.40))) | ForEach-Object { $_.Item } | Sort-Object Start)
    }

    $sb = New-Object Text.StringBuilder
    $n = 1
    foreach ($it in $selected) {
        $a = [Math]::Max(0,$it.Start - $start)
        $b = [Math]::Min($end - $start,$it.End - $start)
        if ($b -le $a) { continue }
        [void]$sb.AppendLine($n.ToString())
        [void]$sb.AppendLine((Seconds-ToSrt $a) + " --> " + (Seconds-ToSrt $b))
        [void]$sb.AppendLine($it.Text)
        [void]$sb.AppendLine("")
        $n++
    }
    [IO.File]::WriteAllText($path,$sb.ToString(),(New-Object Text.UTF8Encoding($false)))
}

function Sanitize-Title([string]$text, [int]$index) {
    $clean = ($text -replace '<[^>]+>','' -replace '[\\/:*?"<>|]','' -replace '\s+',' ').Trim()
    if ($clean.Length -gt 58) { $clean = $clean.Substring(0,58).Trim() }
    if ([string]::IsNullOrWhiteSpace($clean)) { $clean = "Corte " + $index }
    return $clean
}

if (-not (Test-Path $Video)) { throw "Video nao encontrado: $Video" }
if (-not (Test-Path $FFmpeg) -or -not (Test-Path $FFprobe)) { throw "FFmpeg nao configurado." }

New-Item -ItemType Directory -Force -Path $OutputDir,$Transcriptions,$Temp | Out-Null

$prompt = ""
if (Test-Path $PromptFile) { $prompt = Get-Content $PromptFile -Raw -Encoding UTF8 }

if ($prompt -match '(?i)(\d+)\s*cortes?') { $Count = [Math]::Max(1,[Math]::Min(30,[int]$Matches[1])) }
if ($prompt -match '(?i)(\d+)\s*(?:a|-)\s*(\d+)\s*(?:segundos?|seg|s)\b') {
    $MinSeconds = [int]$Matches[1]
    $MaxSeconds = [int]$Matches[2]
}
if ($prompt -match '(?i)\b(vertical|reels?|shorts?|tiktok|stories)\b') { $Format = "vertical" }
if ($prompt -match '(?i)\b(horizontal|youtube 16:9)\b') { $Format = "horizontal" }
if ($prompt -match '(?i)(legenda completa|legendar tudo|legende tudo)') { $CaptionMode = "completa" }
if ($prompt -match '(?i)(legenda de destaque|somente.*destaque|legendas? relevantes)') { $CaptionMode = "destaques" }
if ($prompt -match '(?i)(sem legenda|nao legendar)') { $CaptionMode = "off" }

if ($MinSeconds -lt 10) { $MinSeconds = 10 }
if ($MaxSeconds -lt $MinSeconds) { $MaxSeconds = $MinSeconds + 10 }

$stem = [IO.Path]::GetFileNameWithoutExtension($Video)
$safeStem = ($stem -replace '[^\p{L}\p{Nd}\-_ ]','').Trim()
if ([string]::IsNullOrWhiteSpace($safeStem)) { $safeStem = "transcricao" }
$srt = Join-Path $Transcriptions ($safeStem + ".srt")

if (-not (Test-Path $srt)) {
    Write-Host "[Cortes] Transcrevendo fonte..." -ForegroundColor Cyan
    & (Join-Path $Root "scripts\transcrever.ps1") -Video $Video -Language $Language -OutputDir $Transcriptions
}
if (-not (Test-Path $srt)) { throw "Nao foi possivel gerar a transcricao para os cortes." }

$entries = Parse-Srt $srt
if ($entries.Count -eq 0) { throw "A transcricao nao possui falas utilizaveis." }

$promptWords = @(
    ($prompt.ToLowerInvariant() -replace '[^\p{L}\p{Nd} ]',' ' -split '\s+') |
    Where-Object { $_.Length -ge 5 } |
    Select-Object -Unique
)

$candidates = @()
for ($i=0; $i -lt $entries.Count; $i += 2) {
    $start = [Math]::Max(0,$entries[$i].Start - 1.2)
    $end = $start
    $textParts = New-Object Collections.Generic.List[string]
    $j = $i

    while ($j -lt $entries.Count -and ($entries[$j].End - $start) -le $MaxSeconds) {
        $end = $entries[$j].End
        $textParts.Add($entries[$j].Text)
        $j++
    }

    $duration = $end - $start
    if ($duration -lt $MinSeconds) { continue }

    $text = $textParts -join " "
    $score = Score-Text $text $promptWords

    $opening = $entries[$i].Text
    if ($opening -match '[!?]') { $score += 1.0 }
    if ($duration -ge (($MinSeconds + $MaxSeconds) / 2.0)) { $score += 0.6 }

    $candidates += [pscustomobject]@{
        Start=$start
        End=$end
        Duration=$duration
        Score=$score
        Text=$text
        Opening=$opening
    }
}

if ($candidates.Count -eq 0) {
    throw "Nao encontrei janelas com duracao suficiente. Reduza a duracao minima."
}

$selected = New-Object Collections.Generic.List[object]
foreach ($candidate in ($candidates | Sort-Object Score -Descending)) {
    $overlap = $false
    foreach ($existing in $selected) {
        $interStart = [Math]::Max($candidate.Start,$existing.Start)
        $interEnd = [Math]::Min($candidate.End,$existing.End)
        if ($interEnd - $interStart -gt 8) { $overlap = $true; break }
    }
    if (-not $overlap) {
        $selected.Add($candidate)
        if ($selected.Count -ge $Count) { break }
    }
}

if ($selected.Count -lt $Count) {
    foreach ($candidate in ($candidates | Sort-Object Start)) {
        if ($selected.Count -ge $Count) { break }
        $exists = $false
        foreach ($existing in $selected) {
            if ([Math]::Abs($candidate.Start - $existing.Start) -lt 5) { $exists = $true; break }
        }
        if (-not $exists) { $selected.Add($candidate) }
    }
}

$encoders = (& $FFmpeg -hide_banner -encoders 2>&1 | Out-String)
$canNvenc = $false
if ($encoders -match "h264_nvenc") {
    & $FFmpeg -hide_banner -loglevel error -f lavfi -i "color=black:s=64x64:r=30:d=0.1" -frames:v 1 -c:v h264_nvenc -f null NUL 2>$null | Out-Null
    $canNvenc = ($LASTEXITCODE -eq 0)
    if (-not $canNvenc) {
        Write-Host "[AVISO] NVENC existe no FFmpeg, mas nao iniciou neste PC. Usando CPU." -ForegroundColor Yellow
    }
}

switch ($Quality) {
    "eco"       { $cq="25"; $nvPreset="p4"; $crf="24"; $cpuPreset="veryfast" }
    "qualidade" { $cq="18"; $nvPreset="p6"; $crf="18"; $cpuPreset="medium" }
    default     { $cq="21"; $nvPreset="p5"; $crf="21"; $cpuPreset="fast" }
}

$index = 1
foreach ($clip in $selected) {
    $duration = [Math]::Min($MaxSeconds,$clip.End - $clip.Start)
    if ($duration -lt $MinSeconds) { continue }

    $title = Sanitize-Title $clip.Opening $index
    $out = Join-Path $OutputDir (("{0:00} - {1}.mp4" -f $index,$title))
    $segmentSrt = Join-Path $Temp (("cut_{0:00}.srt" -f $index))

    if ($CaptionMode -ne "off") {
        Write-SegmentSrt $entries $clip.Start ($clip.Start + $duration) $segmentSrt $CaptionMode
    }

    $filters = New-Object Collections.Generic.List[string]

    if ($Format -eq "vertical" -and $VerticalMode -eq "blur") {
        $filters.Add("[0:v]fps=60,split=2[bg][fg]")
        $filters.Add("[bg]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,gblur=sigma=28[bg2]")
        $filters.Add("[fg]scale=1080:1920:force_original_aspect_ratio=decrease[fg2]")
        $filters.Add("[bg2][fg2]overlay=(W-w)/2:(H-h)/2,setsar=1[v0]")
    } elseif ($Format -eq "vertical") {
        $filters.Add("[0:v]fps=60,scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,setsar=1[v0]")
    } else {
        $filters.Add("[0:v]fps=60,scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2:black,setsar=1[v0]")
    }

    $vout = "v0"
    if ($CaptionMode -ne "off" -and (Test-Path $segmentSrt)) {
        $sub = Escape-FilterPath $segmentSrt
        if ($Format -eq "vertical") {
            $force = "FontName=Arial,FontSize=24,Bold=1,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,Outline=3,Shadow=1,Alignment=2,MarginV=300"
        } else {
            $force = "FontName=Arial,FontSize=21,Bold=1,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,Outline=3,Shadow=1,Alignment=2,MarginV=80"
        }
        $filters.Add(("[v0]subtitles=filename='{0}':force_style='{1}'[v1]" -f $sub,$force))
        $vout = "v1"
    }

    $args = New-Object Collections.Generic.List[string]
    $args.Add("-hide_banner")
    $args.Add("-y")
    $args.Add("-ss")
    $args.Add((Num $clip.Start))
    $args.Add("-i")
    $args.Add($Video)
    $args.Add("-t")
    $args.Add((Num $duration))
    $args.Add("-filter_complex")
    $args.Add(($filters -join ";"))
    $args.Add("-map")
    $args.Add("[$vout]")
    $args.Add("-map")
    $args.Add("0:a?")
    $args.Add("-af")
    $args.Add("highpass=f=75,lowpass=f=17000,afftdn=nf=-25,dynaudnorm=f=150:g=7,alimiter=limit=0.95")

    if ($canNvenc) {
        $args.Add("-c:v")
        $args.Add("h264_nvenc")
        $args.Add("-preset")
        $args.Add($nvPreset)
        $args.Add("-rc")
        $args.Add("vbr")
        $args.Add("-cq")
        $args.Add($cq)
        $args.Add("-b:v")
        $args.Add("0")
    } else {
        $args.Add("-c:v")
        $args.Add("libx264")
        $args.Add("-preset")
        $args.Add($cpuPreset)
        $args.Add("-crf")
        $args.Add($crf)
        $args.Add("-threads")
        $args.Add("8")
    }

    $args.Add("-c:a")
    $args.Add("aac")
    $args.Add("-b:a")
    $args.Add("192k")
    $args.Add("-ar")
    $args.Add("48000")
    $args.Add("-pix_fmt")
    $args.Add("yuv420p")
    $args.Add("-movflags")
    $args.Add("+faststart")
    $args.Add($out)

    Write-Host ""
    Write-Host ("[Corte {0}/{1}] {2}" -f $index,$selected.Count,$title) -ForegroundColor Cyan
    Write-Host ("Trecho: {0}s -> {1}s" -f (Num $clip.Start),(Num ($clip.Start + $duration)))

    & $FFmpeg @args
    if ($LASTEXITCODE -ne 0) { throw "Falha ao renderizar corte $index." }

    Write-Host ("[OK] " + $out) -ForegroundColor Green
    $index++
}

Write-Host ""
Write-Host ("Koda Cut gerou {0} corte(s)." -f ($index - 1)) -ForegroundColor Green
