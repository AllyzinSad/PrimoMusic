param(
    [Parameter(Mandatory=$true)][string]$Video,
    [Parameter(Mandatory=$true)][string]$Project,
    [Parameter(Mandatory=$true)][string]$Manifest,
    [ValidateSet("horizontal","vertical","quadrado","horizontal_vertical","todos")][string]$OutputMode = "horizontal",
    [ValidateSet("blur","crop")][string]$VerticalMode = "blur",
    [ValidateSet("eco","balanceado","qualidade")][string]$Quality = "balanceado",
    [Parameter(Mandatory=$true)][string]$OutputDir,
    [ValidateSet("off","completa","destaques","palavra")][string]$CaptionMode = "off",
    [string]$AutoTranscribe = "true",
    [ValidateSet("pt","auto","en","es")][string]$Language = "pt",
    [string]$SafeZone = "true",
    [string]$NoiseReduction = "true",
    [string]$NormalizeAudio = "true",
    [string]$Ducking = "true",
    [double]$VoiceVolume = 1.0,
    [double]$DefaultMusicVolume = 0.08,
    [ValidateSet("personalizado","gameplay","dark","podcast","shorts","clean","cinematico")][string]$Style = "personalizado"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$FFmpeg = Join-Path $Root "ffmpeg\bin\ffmpeg.exe"
$FFprobe = Join-Path $Root "ffmpeg\bin\ffprobe.exe"
$TempDir = Join-Path $Root "temp"
$Transcriptions = Join-Path $Root "transcricoes"

function GP($Obj, [string]$Name, $Default = $null) {
    if ($null -ne $Obj -and $Obj.PSObject.Properties.Name -contains $Name) { return $Obj.$Name }
    return $Default
}

function Num([double]$n) {
    return $n.ToString("0.###",[Globalization.CultureInfo]::InvariantCulture)
}

function To-Bool([string]$value, [bool]$default = $false) {
    if ([string]::IsNullOrWhiteSpace($value)) { return $default }
    return $value.Trim().ToLowerInvariant() -in @("true","1","yes","sim","on")
}

function Escape-FilterPath([string]$p) {
    return $p.Replace("\","/").Replace(":","\:").Replace("'","\'")
}

function Get-Asset([string]$id) {
    if ([string]::IsNullOrWhiteSpace($id)) { return $null }
    $prop = $assets.PSObject.Properties[$id]
    if ($null -eq $prop) { return $null }
    return $prop.Value
}

function Get-Position([string]$pos, [string]$kind) {
    if ($kind -eq "vertical") {
        if ($UseSafeZone) { $bottomY = "H-h-300" } else { $bottomY = "H-h-70" }
    } else {
        $bottomY = "H-h-40"
    }

    switch ($pos.ToLowerInvariant()) {
        "bottom-right"  { return @("W-w-40",$bottomY) }
        "bottom-center" { return @("(W-w)/2",$bottomY) }
        "top-left"      { return @("40","80") }
        "top-right"     { return @("W-w-40","80") }
        "center"        { return @("(W-w)/2","(H-h)/2") }
        default         { return @("40",$bottomY) }
    }
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

function Score-Subtitle([string]$text) {
    $score = 0.0
    $clean = $text.Trim()
    if ($clean -match '[!?]') { $score += 2.5 }
    if ($clean -match '(?i)\b(nossa|caraca|mano|meu deus|impossivel|absurdo|olha|agora|pera|espera|como|por que|porque|serio|mentira|acertou|errou|ganhou|perdeu|cuidado)\b') { $score += 3.0 }
    if ($clean -match '(?i)(kkk|haha|rsrs)') { $score += 2.0 }
    if ($clean.Length -ge 18 -and $clean.Length -le 95) { $score += 1.0 }
    if ($clean -cmatch '[A-ZÁÉÍÓÚÃÕÇ]{4,}') { $score += 1.0 }
    return $score
}

function Select-Highlights($items) {
    if ($items.Count -eq 0) { return @() }
    $scored = @()
    for ($i=0; $i -lt $items.Count; $i++) {
        $it = $items[$i]
        $scored += [pscustomobject]@{ Index=$i; Score=(Score-Subtitle $it.Text); Item=$it }
    }
    $take = [Math]::Max(1,[Math]::Ceiling($items.Count * 0.35))
    $picked = $scored | Sort-Object Score -Descending | Select-Object -First $take
    return @($picked | Sort-Object Index | ForEach-Object { $_.Item })
}

function Write-Srt($items, [string]$path) {
    $sb = New-Object Text.StringBuilder
    $i = 1
    foreach ($it in $items) {
        [void]$sb.AppendLine($i.ToString())
        [void]$sb.AppendLine((Seconds-ToSrt $it.Start) + " --> " + (Seconds-ToSrt $it.End))
        [void]$sb.AppendLine($it.Text)
        [void]$sb.AppendLine("")
        $i++
    }
    [IO.File]::WriteAllText($path,$sb.ToString(),(New-Object Text.UTF8Encoding($false)))
}

function Ass-Time([double]$sec) {
    if ($sec -lt 0) { $sec = 0 }
    $ts = [TimeSpan]::FromSeconds($sec)
    $hours = [Math]::Floor($ts.TotalHours)
    $cs = [Math]::Floor($ts.Milliseconds / 10)
    return ("{0}:{1:00}:{2:00}.{3:00}" -f $hours,$ts.Minutes,$ts.Seconds,$cs)
}

function Escape-AssText([string]$text) {
    return $text.Replace("\","\\").Replace("{","\{").Replace("}","\}").Replace([char]13," ").Replace([char]10," ")
}

function Write-KaraokeAss($items, [string]$path, [int]$W, [int]$H, [string]$kind) {
    if ($kind -eq "vertical") {
        $fontSize = 66
        if ($UseSafeZone) { $marginV = 300 } else { $marginV = 120 }
    } elseif ($kind -eq "quadrado") {
        $fontSize = 48
        $marginV = 100
    } else {
        $fontSize = 46
        $marginV = 90
    }

    if ($Style -eq "dark") { $fontSize = [Math]::Max(38,$fontSize - 8) }
    if ($Style -eq "gameplay") { $fontSize += 5 }

    $header = @"
[Script Info]
ScriptType: v4.00+
PlayResX: $W
PlayResY: $H
WrapStyle: 2
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding
Style: Koda,Arial,$fontSize,&H00FFFFFF,&H006E6E6E,&H00000000,&H64000000,-1,0,0,0,100,100,0,0,1,4,1,2,70,70,$marginV,1

[Events]
Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
"@

    $sb = New-Object Text.StringBuilder
    [void]$sb.Append($header)

    foreach ($it in $items) {
        $words = @($it.Text -split '\s+' | Where-Object { $_ -ne "" })
        if ($words.Count -eq 0) { continue }
        $duration = [Math]::Max(0.2,$it.End - $it.Start)
        $centis = [Math]::Max(1,[Math]::Floor(($duration * 100) / $words.Count))
        $text = ""
        foreach ($word in $words) {
            $text += ("{\k" + $centis + "}" + (Escape-AssText $word) + " ")
        }
        $line = "Dialogue: 0," + (Ass-Time $it.Start) + "," + (Ass-Time $it.End) + ",Koda,,0,0,0,," + $text.Trim()
        [void]$sb.AppendLine($line)
    }

    [IO.File]::WriteAllText($path,$sb.ToString(),(New-Object Text.UTF8Encoding($false)))
}

if (-not (Test-Path $FFmpeg) -or -not (Test-Path $FFprobe)) {
    throw "FFmpeg nao configurado. Abra o Koda Cut e clique CONFIGURAR FERRAMENTAS."
}
if (-not (Test-Path $Video)) { throw "Video principal nao encontrado: $Video" }
if (-not (Test-Path $Project)) { throw "KodaScript nao encontrado: $Project" }
if (-not (Test-Path $Manifest)) { throw "Mapa de arquivos nao encontrado: $Manifest" }

$UseSafeZone = To-Bool $SafeZone $true
$UseNoiseReduction = To-Bool $NoiseReduction $true
$UseNormalize = To-Bool $NormalizeAudio $true
$UseDucking = To-Bool $Ducking $true
$UseAutoTranscribe = To-Bool $AutoTranscribe $true

New-Item -ItemType Directory -Force -Path $TempDir,$OutputDir,$Transcriptions | Out-Null
Get-ChildItem $TempDir -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

$cfg = Get-Content $Project -Raw -Encoding UTF8 | ConvertFrom-Json
$assets = Get-Content $Manifest -Raw -Encoding UTF8 | ConvertFrom-Json

if ($cfg.PSObject.Properties.Name -contains "timeline") {
    $events = @($cfg.timeline)
} elseif ($cfg.PSObject.Properties.Name -contains "eventos") {
    $events = @($cfg.eventos)
} else {
    $events = @()
}

$musicCfg = GP $cfg "musica" $null
$fps = [int](GP $cfg "fps" 60)
$limit = [double](GP $cfg "duracao_saida" 0)

$probeAudio = (& $FFprobe -v error -select_streams a:0 -show_entries stream=index -of csv=p=0 $Video 2>$null | Out-String).Trim()
$hasAudio = -not [string]::IsNullOrWhiteSpace($probeAudio)

$encoders = (& $FFmpeg -hide_banner -encoders 2>&1 | Out-String)
$canNvenc = $false
if ($encoders -match "h264_nvenc") {
    & $FFmpeg -hide_banner -loglevel error -f lavfi -i "color=black:s=64x64:r=30:d=0.1" -frames:v 1 -c:v h264_nvenc -f null NUL 2>$null | Out-Null
    $canNvenc = ($LASTEXITCODE -eq 0)
    if (-not $canNvenc) {
        Write-Host "[AVISO] NVENC existe no FFmpeg, mas nao iniciou neste PC. Usando CPU." -ForegroundColor Yellow
    }
}

$captionSrt = $null
if ($CaptionMode -ne "off" -and $UseAutoTranscribe) {
    $stem = [IO.Path]::GetFileNameWithoutExtension($Video)
    $safeStem = ($stem -replace '[^\p{L}\p{Nd}\-_ ]','').Trim()
    if ([string]::IsNullOrWhiteSpace($safeStem)) { $safeStem = "transcricao" }
    $expected = Join-Path $Transcriptions ($safeStem + ".srt")

    if (-not (Test-Path $expected)) {
        Write-Host "[Koda Cut] Gerando legenda local com Whisper..." -ForegroundColor Cyan
        & (Join-Path $Root "scripts\transcrever.ps1") -Video $Video -Language $Language -OutputDir $Transcriptions
    }
    if (Test-Path $expected) { $captionSrt = $expected }
}

function Render-One([string]$Kind) {
    if ($Kind -eq "vertical") {
        $W=1080; $H=1920
    } elseif ($Kind -eq "quadrado") {
        $W=1080; $H=1080
    } else {
        $W=1920; $H=1080
    }

    $args = New-Object Collections.Generic.List[string]
    $args.Add("-hide_banner")
    $args.Add("-y")
    $args.Add("-i")
    $args.Add($Video)

    $nextInput = 1
    $musicIndex = $null

    if ($null -ne $musicCfg) {
        $musicAssetId = [string](GP $musicCfg "asset" "")
        if (-not [string]::IsNullOrWhiteSpace($musicAssetId)) {
            $ma = Get-Asset $musicAssetId
            if ($null -ne $ma -and (Test-Path ([string]$ma.path))) {
                $args.Add("-stream_loop")
                $args.Add("-1")
                $args.Add("-i")
                $args.Add([string]$ma.path)
                $musicIndex = $nextInput
                $nextInput++
            } else {
                Write-Host "[AVISO] Musica nao encontrada no mapa: $musicAssetId" -ForegroundColor Yellow
            }
        }
    }

    $eventInfo = @()

    for ($i=0; $i -lt $events.Count; $i++) {
        $ev = $events[$i]
        $elementInfos = @()
        $soundInfos = @()

        $elements = @(GP $ev "elementos" @())
        foreach ($el in $elements) {
            $assetId = [string](GP $el "asset" "")
            $asset = Get-Asset $assetId
            if ($null -eq $asset) {
                Write-Host "[AVISO] Elemento nao encontrado: $assetId" -ForegroundColor Yellow
                continue
            }

            $assetPath = [string]$asset.path
            if (-not (Test-Path $assetPath)) {
                Write-Host "[AVISO] Arquivo nao existe: $assetPath" -ForegroundColor Yellow
                continue
            }

            $type = [string]$asset.type
            if ($type -ne "image" -and $type -ne "video") {
                Write-Host "[AVISO] $assetId nao e imagem/video para overlay." -ForegroundColor Yellow
                continue
            }

            if ($type -eq "image") {
                $args.Add("-loop")
                $args.Add("1")
                $args.Add("-framerate")
                $args.Add($fps.ToString())
                $args.Add("-i")
                $args.Add($assetPath)
            } else {
                $sourceStart = [double](GP $el "source_start" 0)
                if ($sourceStart -gt 0) {
                    $args.Add("-ss")
                    $args.Add((Num $sourceStart))
                }
                if ([bool](GP $el "loop" $false)) {
                    $args.Add("-stream_loop")
                    $args.Add("-1")
                }
                $args.Add("-i")
                $args.Add($assetPath)
            }

            $elementInfos += [pscustomobject]@{
                element = $el
                input = $nextInput
                type = $type
                asset = $assetId
            }
            $nextInput++
        }

        $sounds = @(GP $ev "audios" @())
        foreach ($snd in $sounds) {
            $assetId = [string](GP $snd "asset" "")
            $asset = Get-Asset $assetId
            if ($null -eq $asset) {
                Write-Host "[AVISO] Audio nao encontrado: $assetId" -ForegroundColor Yellow
                continue
            }

            $type = [string]$asset.type
            if ($type -ne "audio" -and $type -ne "video") {
                Write-Host "[AVISO] $assetId nao possui trilha de audio utilizavel." -ForegroundColor Yellow
                continue
            }

            $assetPath = [string]$asset.path
            if (-not (Test-Path $assetPath)) {
                Write-Host "[AVISO] Arquivo nao existe: $assetPath" -ForegroundColor Yellow
                continue
            }

            $sourceStart = [double](GP $snd "source_start" 0)
            if ($sourceStart -gt 0) {
                $args.Add("-ss")
                $args.Add((Num $sourceStart))
            }
            $args.Add("-i")
            $args.Add($assetPath)

            $soundInfos += [pscustomobject]@{
                sound = $snd
                input = $nextInput
                asset = $assetId
            }
            $nextInput++
        }

        $eventInfo += [pscustomobject]@{
            ev = $ev
            elements = $elementInfos
            sounds = $soundInfos
            n = $i
        }
    }

    $filters = New-Object Collections.Generic.List[string]

    if ($Kind -eq "vertical" -and $VerticalMode -eq "blur") {
        $filters.Add(("[0:v]fps={0},setpts=PTS-STARTPTS,split=2[bg0][fg0]" -f $fps))
        $filters.Add(("[bg0]scale={0}:{1}:force_original_aspect_ratio=increase,crop={0}:{1},gblur=sigma=28[bg1]" -f $W,$H))
        $filters.Add(("[fg0]scale={0}:{1}:force_original_aspect_ratio=decrease[fg1]" -f $W,$H))
        $filters.Add("[bg1][fg1]overlay=(W-w)/2:(H-h)/2,setsar=1[v0]")
    } elseif ($Kind -eq "vertical") {
        $filters.Add(("[0:v]fps={0},setpts=PTS-STARTPTS,scale={1}:{2}:force_original_aspect_ratio=increase,crop={1}:{2},setsar=1[v0]" -f $fps,$W,$H))
    } else {
        $filters.Add(("[0:v]fps={0},setpts=PTS-STARTPTS,scale={1}:{2}:force_original_aspect_ratio=decrease,pad={1}:{2}:(ow-iw)/2:(oh-ih)/2:black,setsar=1[v0]" -f $fps,$W,$H))
    }

    $vcur = "v0"
    $captionCounter = 0
    $elementCounter = 0
    $shakeCounter = 0

    foreach ($info in $eventInfo) {
        $ev = $info.ev
        $n = $info.n

        $start = [double](GP $ev "inicio" 0)
        $end = [double](GP $ev "fim" ($start + 1.0))
        if ($end -le $start) { $end = $start + 0.2 }

        $s = Num $start
        $e = Num $end

        $zoom = [double](GP $ev "zoom" 1.0)
        if ($zoom -gt 1.001) {
            $z = Num $zoom
            $filters.Add(("[{0}]split=2[zk{1}][zs{1}]" -f $vcur,$n))
            $filters.Add(("[zs{0}]crop=iw/{1}:ih/{1}:(iw-iw/{1})/2:(ih-ih/{1})/2,scale={2}:{3}[zo{0}]" -f $n,$z,$W,$H))
            $filters.Add(("[zk{0}][zo{0}]overlay=0:0:enable='between(t,{1},{2})'[vz{0}]" -f $n,$s,$e))
            $vcur = "vz$n"
        }

        $shake = [double](GP $ev "shake" 0)
        if ($shake -gt 0) {
            $shakeCounter++
            $amount = [Math]::Max(2,[Math]::Min(24,$shake))
            $filters.Add(("[{0}]split=2[shk{1}][shs{1}]" -f $vcur,$shakeCounter))
            $filters.Add(("[shs{0}]crop=iw/1.04:ih/1.04:x='(iw-ow)/2+{1}*sin(45*t)':y='(ih-oh)/2+{1}*cos(38*t)',scale={2}:{3}[sho{0}]" -f $shakeCounter,(Num $amount),$W,$H))
            $filters.Add(("[shk{0}][sho{0}]overlay=0:0:enable='between(t,{1},{2})'[shv{0}]" -f $shakeCounter,$s,$e))
            $vcur = "shv$shakeCounter"
        }

        $freeze = [bool](GP $ev "freeze" $false)
        if ($freeze) {
            $frameEnd = Num ($start + (1.0 / [Math]::Max(30,$fps)))
            $freezeDur = Num (($end - $start) + 0.1)
            $filters.Add(("[{0}]split=2[fk{1}][fs{1}]" -f $vcur,$n))
            $filters.Add(("[fs{0}]trim=start={1}:end={2},setpts=PTS-STARTPTS+{1}/TB,tpad=stop_mode=clone:stop_duration={3}[fr{0}]" -f $n,$s,$frameEnd,$freezeDur))
            $filters.Add(("[fk{0}][fr{0}]overlay=0:0:eof_action=pass:enable='between(t,{1},{2})'[vf{0}]" -f $n,$s,$e))
            $vcur = "vf$n"
        }

        foreach ($ei in $info.elements) {
            $elementCounter++
            $el = $ei.element
            $inputNo = $ei.input

            $mode = ([string](GP $el "modo" "fit")).ToLowerInvariant()
            $pos = ([string](GP $el "posicao" "bottom-left")).ToLowerInvariant()
            $opacity = [double](GP $el "opacity" 1.0)
            if ($opacity -lt 0) { $opacity = 0 }
            if ($opacity -gt 1) { $opacity = 1 }
            $opacityS = Num $opacity

            if ($Kind -eq "vertical") { $defaultWidth = 430 } elseif ($Kind -eq "quadrado") { $defaultWidth = 330 } else { $defaultWidth = 360 }
            $width = [int](GP $el "largura" $defaultWidth)

            $prepared = "elprep$elementCounter"
            if ($pos -eq "full" -or $mode -eq "cover") {
                $filters.Add(("[{0}:v]fps={1},setpts=PTS-STARTPTS+{2}/TB,scale={3}:{4}:force_original_aspect_ratio=increase,crop={3}:{4},format=rgba,colorchannelmixer=aa={5}[{6}]" -f $inputNo,$fps,$s,$W,$H,$opacityS,$prepared))
                $x = "0"
                $y = "0"
            } elseif ($mode -eq "fit-full") {
                $filters.Add(("[{0}:v]fps={1},setpts=PTS-STARTPTS+{2}/TB,scale={3}:{4}:force_original_aspect_ratio=decrease,format=rgba,colorchannelmixer=aa={5}[{6}]" -f $inputNo,$fps,$s,$W,$H,$opacityS,$prepared))
                $x = "(W-w)/2"
                $y = "(H-h)/2"
            } else {
                $filters.Add(("[{0}:v]fps={1},setpts=PTS-STARTPTS+{2}/TB,scale={3}:-1,format=rgba,colorchannelmixer=aa={4}[{5}]" -f $inputNo,$fps,$s,$width,$opacityS,$prepared))
                $xy = Get-Position $pos $Kind
                $x = $xy[0]
                $y = $xy[1]
            }

            $next = "ve$elementCounter"
            $filters.Add(("[{0}][{1}]overlay=x={2}:y={3}:eof_action=pass:enable='between(t,{4},{5})'[{6}]" -f $vcur,$prepared,$x,$y,$s,$e,$next))
            $vcur = $next
        }

        $textValue = [string](GP $ev "texto" "")
        if (-not [string]::IsNullOrWhiteSpace($textValue)) {
            $captionCounter++
            $txt = Join-Path $TempDir ("caption_" + $Kind + "_" + $captionCounter + ".txt")
            [IO.File]::WriteAllText($txt,$textValue,(New-Object Text.UTF8Encoding($false)))
            $txtEsc = Escape-FilterPath $txt

            if ($Kind -eq "vertical") { $defaultSize = 68 } elseif ($Kind -eq "quadrado") { $defaultSize = 54 } else { $defaultSize = 58 }
            $size = [int](GP $ev "font_size" $defaultSize)
            $pos = ([string](GP $ev "texto_posicao" "top")).ToLowerInvariant()

            switch ($pos) {
                "center" { $ty="(h-text_h)/2" }
                "bottom" {
                    if ($Kind -eq "vertical" -and $UseSafeZone) { $ty="h-text_h-h*0.24" }
                    elseif ($Kind -eq "vertical") { $ty="h-text_h-h*0.10" }
                    else { $ty="h-text_h-h*0.10" }
                }
                default {
                    if ($Kind -eq "vertical") { $ty="h*0.10" } else { $ty="h*0.07" }
                }
            }

            $font = "C\:/Windows/Fonts/arialbd.ttf"
            $filters.Add(("[{0}]drawtext=fontfile='{1}':textfile='{2}':fontcolor=white:fontsize={3}:borderw=5:bordercolor=black:box=1:boxcolor=black@0.20:boxborderw=14:x=(w-text_w)/2:y={4}:enable='between(t,{5},{6})'[vt{7}]" -f $vcur,$font,$txtEsc,$size,$ty,$s,$e,$n))
            $vcur = "vt$n"
        }
    }

    if ($CaptionMode -ne "off" -and $null -ne $captionSrt -and (Test-Path $captionSrt)) {
        $subtitleSource = $captionSrt

        if ($CaptionMode -eq "destaques") {
            $allItems = Parse-Srt $captionSrt
            $selected = Select-Highlights $allItems
            $subtitleSource = Join-Path $TempDir ("highlights_" + $Kind + ".srt")
            Write-Srt $selected $subtitleSource
        }

        if ($CaptionMode -eq "palavra") {
            $allItems = Parse-Srt $captionSrt
            $ass = Join-Path $TempDir ("karaoke_" + $Kind + ".ass")
            Write-KaraokeAss $allItems $ass $W $H $Kind
            $assEsc = Escape-FilterPath $ass
            $filters.Add(("[{0}]ass=filename='{1}'[vcaption]" -f $vcur,$assEsc))
            $vcur = "vcaption"
        } else {
            $subEsc = Escape-FilterPath $subtitleSource
            if ($Kind -eq "vertical") {
                if ($Style -eq "dark") { $fontSize = 20 } else { $fontSize = 24 }
                if ($UseSafeZone) { $marginV = 300 } else { $marginV = 120 }
            } elseif ($Kind -eq "quadrado") {
                $fontSize = 21
                $marginV = 95
            } else {
                if ($Style -eq "dark") { $fontSize = 18 } else { $fontSize = 21 }
                $marginV = 80
            }
            $force = "FontName=Arial,FontSize=$fontSize,Bold=1,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,BorderStyle=1,Outline=3,Shadow=1,Alignment=2,MarginV=$marginV"
            $filters.Add(("[{0}]subtitles=filename='{1}':force_style='{2}'[vcaption]" -f $vcur,$subEsc,$force))
            $vcur = "vcaption"
        }
    }

    $voiceVol = Num $VoiceVolume

    if ($hasAudio) {
        $af = @("aresample=48000","aformat=channel_layouts=stereo","highpass=f=75","lowpass=f=17000")
        if ($UseNoiseReduction) { $af += "afftdn=nf=-25" }
        if ($UseNormalize) { $af += "dynaudnorm=f=150:g=7" }
        $af += "volume=$voiceVol"
        $filters.Add("[0:a]" + ($af -join ",") + "[voice0]")
    } else {
        if ($limit -gt 0) {
            $dur = $limit
        } else {
            $durationText = (& $FFprobe -v error -show_entries format=duration -of default=nw=1:nk=1 $Video | Select-Object -First 1)
            $dur = [double]::Parse($durationText,[Globalization.CultureInfo]::InvariantCulture)
        }
        $filters.Add(("anullsrc=r=48000:cl=stereo,atrim=duration={0}[voice0]" -f (Num $dur)))
    }

    $audioLabels = New-Object Collections.Generic.List[string]

    if ($null -ne $musicIndex) {
        $mv = [double](GP $musicCfg "volume" $DefaultMusicVolume)
        $mvS = Num $mv
        $filters.Add(("[{0}:a]aresample=48000,aformat=channel_layouts=stereo,volume={1}[musicRaw]" -f $musicIndex,$mvS))

        if ($UseDucking -and $hasAudio) {
            $filters.Add("[voice0]asplit=2[voiceMix][voiceSide]")
            $filters.Add("[musicRaw][voiceSide]sidechaincompress=threshold=0.025:ratio=8:attack=20:release=300[musicDuck]")
            $audioLabels.Add("[voiceMix]")
            $audioLabels.Add("[musicDuck]")
        } else {
            $audioLabels.Add("[voice0]")
            $audioLabels.Add("[musicRaw]")
        }
    } else {
        $audioLabels.Add("[voice0]")
    }

    $soundCounter = 0
    foreach ($info in $eventInfo) {
        $ev = $info.ev
        $eventStart = [double](GP $ev "inicio" 0)

        foreach ($si in $info.sounds) {
            $soundCounter++
            $snd = $si.sound
            $at = [double](GP $snd "at" 0)
            $timelineStart = [Math]::Max(0, $eventStart + $at)
            $delay = [int][Math]::Round($timelineStart * 1000)
            $sv = Num ([double](GP $snd "volume" 0.70))
            $dur = [double](GP $snd "duracao" 0)

            if ($dur -gt 0) {
                $filters.Add(("[{0}:a]aresample=48000,aformat=channel_layouts=stereo,atrim=duration={1},asetpts=PTS-STARTPTS,volume={2},adelay={3}|{3}[snd{4}]" -f $si.input,(Num $dur),$sv,$delay,$soundCounter))
            } else {
                $filters.Add(("[{0}:a]aresample=48000,aformat=channel_layouts=stereo,asetpts=PTS-STARTPTS,volume={1},adelay={2}|{2}[snd{3}]" -f $si.input,$sv,$delay,$soundCounter))
            }
            $audioLabels.Add("[snd$soundCounter]")
        }
    }

    if ($audioLabels.Count -gt 1) {
        $filters.Add(($audioLabels -join "") + ("amix=inputs={0}:duration=first:dropout_transition=0,alimiter=limit=0.95[aout]" -f $audioLabels.Count))
    } else {
        $filters.Add("[voice0]alimiter=limit=0.95[aout]")
    }

    $stem = [IO.Path]::GetFileNameWithoutExtension($Video)
    if ($Kind -eq "vertical") {
        $out = Join-Path $OutputDir ($stem + "_VERTICAL_1080x1920_60p.mp4")
    } elseif ($Kind -eq "quadrado") {
        $out = Join-Path $OutputDir ($stem + "_QUADRADO_1080x1080_60p.mp4")
    } else {
        $out = Join-Path $OutputDir ($stem + "_HORIZONTAL_1080p60.mp4")
    }

    switch ($Quality) {
        "eco"       { $cq="25"; $nvPreset="p4"; $crf="24"; $cpuPreset="veryfast" }
        "qualidade" { $cq="18"; $nvPreset="p6"; $crf="18"; $cpuPreset="medium" }
        default     { $cq="21"; $nvPreset="p5"; $crf="21"; $cpuPreset="fast" }
    }

    $args.Add("-filter_complex")
    $args.Add(($filters -join ";"))
    $args.Add("-map")
    $args.Add("[$vcur]")
    $args.Add("-map")
    $args.Add("[aout]")

    if ($limit -gt 0) {
        $args.Add("-t")
        $args.Add((Num $limit))
    }

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
        $args.Add("-maxrate")
        $args.Add("20M")
        $args.Add("-bufsize")
        $args.Add("40M")
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
    $args.Add("-shortest")
    $args.Add($out)

    Write-Host ""
    Write-Host ("=== Render {0} ===" -f $Kind) -ForegroundColor Cyan
    Write-Host ("Saida: {0}" -f $out)
    if ($canNvenc) { Write-Host "Encoder: NVIDIA NVENC" } else { Write-Host "Encoder: CPU / libx264" }

    & $FFmpeg @args
    if ($LASTEXITCODE -ne 0) {
        throw ("FFmpeg falhou no render {0}." -f $Kind)
    }

    Write-Host ("[OK] {0}" -f $out) -ForegroundColor Green
}

switch ($OutputMode) {
    "horizontal_vertical" {
        Render-One "horizontal"
        Render-One "vertical"
    }
    "todos" {
        Render-One "horizontal"
        Render-One "vertical"
        Render-One "quadrado"
    }
    default {
        Render-One $OutputMode
    }
}

Get-ChildItem $TempDir -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "Koda Cut terminou." -ForegroundColor Green
