@echo off
chcp 65001 >nul
title Build Koda Connect Beta
cd /d "%~dp0"

where javac >nul 2>&1
if errorlevel 1 (
  echo [ERRO] JDK 17+ nao encontrado.
  pause
  exit /b 1
)

where jpackage >nul 2>&1
if errorlevel 1 (
  echo [ERRO] jpackage nao encontrado.
  pause
  exit /b 1
)

if exist out-connect rmdir /s /q out-connect
if exist build-connect rmdir /s /q build-connect
if exist "dist\Koda Connect" rmdir /s /q "dist\Koda Connect"
mkdir out-connect
mkdir build-connect
mkdir dist 2>nul

echo Compilando...
javac -encoding UTF-8 --release 17 --add-modules jdk.httpserver -d out-connect src\kodaconnect\*.java
if errorlevel 1 goto :erro

echo Gerando JAR...
jar --create --file build-connect\KodaConnect.jar -C out-connect .
if errorlevel 1 goto :erro

echo Gerando app portatil...
jpackage --type app-image --dest dist --name "Koda Connect" --input build-connect --main-jar KodaConnect.jar --main-class kodaconnect.KodaConnect --vendor "Koda" --app-version 0.1 --java-options "--add-modules=jdk.httpserver"
if errorlevel 1 goto :erro

if exist scripts xcopy /E /I /Y scripts "dist\Koda Connect\app\scripts" >nul
if exist projetos xcopy /E /I /Y projetos "dist\Koda Connect\app\projetos" >nul

echo.
echo [OK] Gerado em:
echo dist\Koda Connect\Koda Connect.exe
pause
exit /b 0

:erro
echo.
echo [ERRO] Build falhou.
pause
exit /b 1
