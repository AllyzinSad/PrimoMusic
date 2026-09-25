@echo off
chcp 65001 >nul
title Build Portable - Koda Cut
cd /d "%~dp0"

where javac >nul 2>&1
if errorlevel 1 (
  echo [ERRO] JDK 17+ nao encontrado.
  pause
  exit /b 1
)

where jpackage >nul 2>&1
if errorlevel 1 (
  echo [ERRO] jpackage nao encontrado. Use um JDK completo 17+.
  pause
  exit /b 1
)

if not exist "ffmpeg\bin\ffmpeg.exe" (
  echo FFmpeg ainda nao foi configurado.
  call CONFIGURAR.bat
)

if exist out rmdir /s /q out
if exist build rmdir /s /q build
if exist "dist\Koda Cut" rmdir /s /q "dist\Koda Cut"
mkdir out
mkdir build
mkdir dist 2>nul

echo Compilando...
javac -encoding UTF-8 --release 17 -d out src\kodacut\KodaCut.java
if errorlevel 1 goto :erro

echo Gerando JAR...
jar --create --file build\KodaCut.jar -C out .
if errorlevel 1 goto :erro

echo Gerando app portatil...
jpackage --type app-image --dest dist --name "Koda Cut" --input build --main-jar KodaCut.jar --main-class kodacut.KodaCut --vendor "Koda" --app-version 0.2
if errorlevel 1 goto :erro

echo Copiando motor e recursos...
xcopy /E /I /Y scripts "dist\Koda Cut\app\scripts" >nul
xcopy /E /I /Y ffmpeg "dist\Koda Cut\app\ffmpeg" >nul
xcopy /E /I /Y assets "dist\Koda Cut\app\assets" >nul
xcopy /E /I /Y efeitos "dist\Koda Cut\app\efeitos" >nul
xcopy /E /I /Y projetos "dist\Koda Cut\app\projetos" >nul

if not exist "dist\Koda Cut\app\biblioteca\videos" mkdir "dist\Koda Cut\app\biblioteca\videos"
if not exist "dist\Koda Cut\app\biblioteca\imagens" mkdir "dist\Koda Cut\app\biblioteca\imagens"
if not exist "dist\Koda Cut\app\biblioteca\audios" mkdir "dist\Koda Cut\app\biblioteca\audios"
if not exist "dist\Koda Cut\app\final" mkdir "dist\Koda Cut\app\final"
if not exist "dist\Koda Cut\app\temp" mkdir "dist\Koda Cut\app\temp"

echo.
echo [OK] PORTATIL GERADO:
echo dist\Koda Cut\Koda Cut.exe
echo.
pause
exit /b 0

:erro
echo.
echo [ERRO] Build falhou.
pause
exit /b 1
