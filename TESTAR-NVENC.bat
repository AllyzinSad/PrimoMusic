@echo off
chcp 65001 >nul
title Teste NVENC - Koda Cut
cd /d "%~dp0"

if not exist "ffmpeg\bin\ffmpeg.exe" (
  echo Execute CONFIGURAR.bat primeiro.
  pause
  exit /b 1
)

"ffmpeg\bin\ffmpeg.exe" -hide_banner -f lavfi -i color=c=black:s=1280x720:r=60:d=2 -c:v h264_nvenc -preset p5 -cq 23 -b:v 0 -f null NUL

if errorlevel 1 (
  echo.
  echo [AVISO] O NVENC falhou. Atualize o driver NVIDIA ou use CPU.
) else (
  echo.
  echo [OK] GTX / NVENC funcionando.
)
echo.
pause
