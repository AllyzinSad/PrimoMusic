param(
    [Parameter(Mandatory=$true)][string]$Video,
    [Parameter(Mandatory=$true)][string]$Project,
    [ValidateSet("horizontal","vertical","ambos")][string]$OutputMode = "horizontal",
    [ValidateSet("blur","crop")][string]$VerticalMode = "blur",
    [ValidateSet("eco","balanceado","qualidade")][string]$Quality = "balanceado",
    [Parameter(Mandatory=$true)][string]$OutputDir
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version 2

$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$FFmpeg = Join-Path $Root "ffmpeg\bin\ffmpeg.exe"
$FFprobe = Join-Path $Root "ffmpeg\bin\ffprobe.exe"
$TempDir = Join-Path $Root "temp"

function GP($Obj, [string]$Name, $Default = $null) {
    if ($null -ne $Obj -and $Obj.PSObject.Properties.Name -contains $Name) { return $Obj.$Name }
    return $Default
}

function Num([double]$n) {
    return $n.ToString("0.###",[Globalization.CultureInfo]::InvariantCulture)
}

function Resolve-KodaPath([string]$p) {
    if ([string]::IsNullOrWhiteSpace($p)) { return $null }
    if ([IO.Path]::IsPathRooted($p)) { return $p }
    return Join-Path $Root $p
}

function Escape-FilterPath([string]$p) {
    return $p.Replace("\","/").Replace(":","\:").Replace("'","\'")
}

if (-not (Test-Path $FFmpeg) -or -not (Test-Path $FFprobe)) {
    throw "FFmpeg nao configurado. Abra o Koda Cut e clique CONFIGURAR."
}
if (-not (Test-Path $Video)) { throw "Video nao encontrado: $Video" }
if (-not (Test-Path $Project)) { throw "JSON nao encontrado: $Project" }

New-Item -ItemType Directory -Force -Path $TempDir,$OutputDir | Out-Null
Get-ChildItem $TempDir -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

$cfg = Get-Content $Project -Raw -Encoding UTF8 | ConvertFrom-Json
$events = @(GP $cfg "eventos" @())
$audioCfg = GP $cfg "audio" $null
$musicCfg = GP $cfg "musica" $null
$fps = [int](GP $cfg "fps" 60)
$limit = [double](GP $cfg "duracao_saida" 0)

$probeAudio = (& $FFprobe -v error -select_streams a:0 -show_entries stream=index -of csv=p=0 $Video 2>$null | Out-String).Trim()
$hasAudio = -not [string]::IsNullOrWhiteSpace($probeAudio)
$encoders = (& $FFmpeg -hide_banner -encoders 2>&1 | Out-String)
$canNvenc = $encoders -match "h264_nvenc"

function Render-One([string]$Kind) {
    if ($Kind -eq "vertical") { $W=1080; $H=1920 } else { $W=1920; $H=1080 }

    $args = New-Object Collections.Generic.List[string]
    $args.Add("-hide_banner")
    $args.Add("-y")
    $args.Add("-i")
    $args.Add($Video)

    $nextInput = 1
    $musicIndex = $null

    if ($null -ne $musicCfg) {
        $mf = Resolve-KodaPath ([string](GP $musicCfg "arquivo" ""))
        if ($mf -and (Test-Path $mf)) {
            $args.Add("-stream_loop")
            $args.Add("-1")
            $args.Add("-i")
            $args.Add($mf)
            $musicIndex = $nextInput
            $nextInput++
        }
    }

    $eventInfo = @()

    for ($i=0; $i -lt $events.Count; $i++) {
        $ev = $events[$i]
        $pngIndex = $null
        $sfxIndex = $null

        $pngName = [string](GP $ev "png" "")
        if (-not [string]::IsNullOrWhiteSpace($pngName)) {
            $pngPath = Resolve-KodaPath $pngName
            if (Test-Path $pngPath) {
                $args.Add("-loop")
                $args.Add("1")
                $args.Add("-framerate")
                $args.Add($fps.ToString())
                $args.Add("-i")
                $args.Add($pngPath)
                $pngIndex = $nextInput
                $nextInput++
            } else {
                Write-Host "[AVISO] PNG nao encontrado: $pngPath" -ForegroundColor Yellow
            }
        }

        $sfxName = [string](GP $ev "sfx" "")
        if (-not [string]::IsNullOrWhiteSpace($sfxName)) {
            if ([IO.Path]::GetExtension($sfxName) -eq "") {
                $sfxName = "efeitos\$sfxName.wav"
            }
            $sfxPath = Resolve-KodaPath $sfxName
            if (Test-Path $sfxPath) {
                $args.Add("-i")
                $args.Add($sfxPath)
                $sfxIndex = $nextInput
                $nextInput++
            } else {
                Write-Host "[AVISO] SFX nao encontrado: $sfxPath" -ForegroundColor Yellow
            }
        }

        $eventInfo += [pscustomobject]@{
            ev = $ev
            png = $pngIndex
            sfx = $sfxIndex
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

        $freeze = [bool](GP $ev "freeze" $false)
        if ($freeze) {
            $frameEnd = Num ($start + (1.0 / [Math]::Max(30,$fps)))
            $freezeDur = Num (($end - $start) + 0.1)
            $filters.Add(("[{0}]split=2[fk{1}][fs{1}]" -f $vcur,$n))
            $filters.Add(("[fs{0}]trim=start={1}:end={2},setpts=PTS-STARTPTS+{1}/TB,tpad=stop_mode=clone:stop_duration={3}[fr{0}]" -f $n,$s,$frameEnd,$freezeDur))
            $filters.Add(("[fk{0}][fr{0}]overlay=0:0:eof_action=pass:enable='between(t,{1},{2})'[vf{0}]" -f $n,$s,$e))
            $vcur = "vf$n"
        }

        if ($null -ne $info.png) {
            if ($Kind -eq "vertical") { $defaultWidth = 430 } else { $defaultWidth = 360 }
            $pw = [int](GP $ev "png_largura" $defaultWidth)

            if ($Kind -eq "vertical") { $defaultPos = "bottom-center" } else { $defaultPos = "bottom-left" }
            $pos = ([string](GP $ev "png_posicao" $defaultPos)).ToLowerInvariant()

            if ($Kind -eq "vertical") { $bottomY = "H-h-260" } else { $bottomY = "H-h-40" }

            switch ($pos) {
                "bottom-right"  { $x="W-w-40"; $y=$bottomY }
                "bottom-center" { $x="(W-w)/2"; $y=$bottomY }
                "top-left"      { $x="40"; $y="80" }
                "top-right"     { $x="W-w-40"; $y="80" }
                "center"        { $x="(W-w)/2"; $y="(H-h)/2" }
                default         { $x="40"; $y=$bottomY }
            }

            $filters.Add(("[{0}:v]scale={1}:-1,format=rgba[png{2}]" -f $info.png,$pw,$n))
            $filters.Add(("[{0}][png{1}]overlay=x={2}:y={3}:eof_action=pass:enable='between(t,{4},{5})'[vp{1}]" -f $vcur,$n,$x,$y,$s,$e))
            $vcur = "vp$n"
        }

        $textValue = [string](GP $ev "texto" "")
        if (-not [string]::IsNullOrWhiteSpace($textValue)) {
            $captionCounter++
            $txt = Join-Path $TempDir ("caption_" + $Kind + "_" + $captionCounter + ".txt")
            [IO.File]::WriteAllText($txt,$textValue,(New-Object Text.UTF8Encoding($false)))
            $txtEsc = Escape-FilterPath $txt

            if ($Kind -eq "vertical") { $defaultSize = 68 } else { $defaultSize = 58 }
            $size = [int](GP $ev "font_size" $defaultSize)
            $pos = ([string](GP $ev "texto_posicao" "top")).ToLowerInvariant()

            switch ($pos) {
                "center" { $ty="(h-text_h)/2" }
                "bottom" {
                    if ($Kind -eq "vertical") { $ty="h-text_h-h*0.22" } else { $ty="h-text_h-h*0.10" }
                }
                default {
                    if ($Kind -eq "vertical") { $ty="h*0.10" } else { $ty="h*0.07" }
                }
            }

            $font = "C\:/Windows/Fonts/arialbd.ttf"
            $filters.Add(("[{0}]drawtext=fontfile='{1}':textfile='{2}':fontcolor=white:fontsize={3}:borderw=5:bordercolor=black:box=1:boxcolor=black@0.22:boxborderw=14:x=(w-text_w)/2:y={4}:enable='between(t,{5},{6})'[vt{7}]" -f $vcur,$font,$txtEsc,$size,$ty,$s,$e,$n))
            $vcur = "vt$n"
        }
    }

    $reduceNoise = [bool](GP $audioCfg "reduzir_ruido" $true)
    $normalize = [bool](GP $audioCfg "normalizar" $true)
    $voiceVol = Num ([double](GP $audioCfg "volume" 1.0))

    if ($hasAudio) {
        $af = @("aresample=48000","aformat=channel_layouts=stereo","highpass=f=75","lowpass=f=17000")
        if ($reduceNoise) { $af += "afftdn=nf=-25" }
        if ($normalize) { $af += "dynaudnorm=f=150:g=7" }
        $af += "volume=$voiceVol"
        $filters.Add("[0:a]" + ($af -join ",") + "[a0]")
    } else {
        if ($limit -gt 0) {
            $dur = $limit
        } else {
            $durationText = (& $FFprobe -v error -show_entries format=duration -of default=nw=1:nk=1 $Video | Select-Object -First 1)
            $dur = [double]::Parse($durationText,[Globalization.CultureInfo]::InvariantCulture)
        }
        $filters.Add(("anullsrc=r=48000:cl=stereo,atrim=duration={0}[a0]" -f (Num $dur)))
    }

    $audioLabels = New-Object Collections.Generic.List[string]
    $audioLabels.Add("[a0]")

    if ($null -ne $musicIndex) {
        $mv = Num ([double](GP $musicCfg "volume" 0.08))
        $filters.Add(("[{0}:a]aresample=48000,aformat=channel_layouts=stereo,volume={1}[music0]" -f $musicIndex,$mv))
        $audioLabels.Add("[music0]")
    }

    $sfxNo = 0
    foreach ($info in $eventInfo) {
        if ($null -ne $info.sfx) {
            $sfxNo++
            $ev = $info.ev
            $start = [double](GP $ev "inicio" 0)
            $delay = [int][Math]::Round($start * 1000)
            $sv = Num ([double](GP $ev "sfx_volume" 0.65))
            $filters.Add(("[{0}:a]aresample=48000,aformat=channel_layouts=stereo,volume={1},adelay={2}|{2}[sfx{3}]" -f $info.sfx,$sv,$delay,$sfxNo))
            $audioLabels.Add("[sfx$sfxNo]")
        }
    }

    if ($audioLabels.Count -gt 1) {
        $filters.Add(($audioLabels -join "") + ("amix=inputs={0}:duration=first:dropout_transition=0,alimiter=limit=0.95[aout]" -f $audioLabels.Count))
    } else {
        $filters.Add("[a0]alimiter=limit=0.95[aout]")
    }

    $stem = [IO.Path]::GetFileNameWithoutExtension($Video)
    if ($Kind -eq "vertical") {
        $out = Join-Path $OutputDir ($stem + "_VERTICAL_1080x1920_60p.mp4")
    } else {
        $out = Join-Path $OutputDir ($stem + "_YOUTUBE_1080p60.mp4")
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

if ($OutputMode -eq "ambos") {
    Render-One "horizontal"
    Render-One "vertical"
} else {
    Render-One $OutputMode
}

Get-ChildItem $TempDir -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "Koda Cut terminou." -ForegroundColor Green
