@echo off
chcp 65001 >nul
title Koda Connect Beta - Instalacao automatica
cd /d "%~dp0"

echo.
echo ==========================================
echo        KODA CONNECT - BETA
echo ==========================================
echo.

where javac >nul 2>&1
if not errorlevel 1 goto :jdk_ok

echo JDK 17 nao encontrado.
where winget >nul 2>&1
if errorlevel 1 (
  echo.
  echo [ERRO] O Windows Package Manager ^(winget^) nao foi encontrado.
  echo Instale o JDK 17 e execute este arquivo novamente.
  pause
  exit /b 1
)

echo.
echo O Koda Connect pode instalar automaticamente o Java 17.
choice /C SN /N /M "Deseja instalar agora? [S/N]: "
if errorlevel 2 exit /b 1

echo.
echo Instalando Eclipse Temurin JDK 17...
winget install EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements
if errorlevel 1 (
  echo.
  echo [ERRO] Nao foi possivel instalar o JDK automaticamente.
  pause
  exit /b 1
)

echo.
echo Atualizando variaveis de ambiente...
for /f "delims=" %%J in ('where javac 2^>nul') do set "JAVAC=%%J"
if not defined JAVAC (
  echo O Java foi instalado. Feche esta janela e abra novamente este arquivo.
  pause
  exit /b 0
)

:jdk_ok
echo [OK] Java encontrado.

if not exist out-connect mkdir out-connect

echo [1/3] Compilando Koda Connect...
javac -encoding UTF-8 --release 17 --add-modules jdk.httpserver -d out-connect src\kodaconnect\*.java
if errorlevel 1 goto :erro

echo [2/3] Preparando workspace...
if not exist KodaConnectWorkspace mkdir KodaConnectWorkspace
for %%D in (videos images audio music broll projects outputs temp logs) do (
  if not exist "KodaConnectWorkspace\%%D" mkdir "KodaConnectWorkspace\%%D"
)

echo [3/3] Abrindo Koda Connect...
start "" javaw --add-modules jdk.httpserver -cp out-connect kodaconnect.KodaConnect

echo.
echo Aguardando o servidor iniciar...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ok=$false; 1..20 | %% { try { $r=Invoke-RestMethod 'http://127.0.0.1:17777/health' -TimeoutSec 1; if($r.ok){$ok=$true; break} } catch {}; Start-Sleep -Milliseconds 500 }; if($ok){exit 0}else{exit 1}"
if errorlevel 1 (
  echo [AVISO] O app abriu, mas o teste automatico ainda nao respondeu.
  echo Veja a janela do Koda Connect para detalhes.
) else (
  echo [OK] Servidor local conectado em http://127.0.0.1:17777
  echo [OK] Koda Connect pronto para os testes locais.
)

echo.
echo Dica: coloque um video em KodaConnectWorkspace\videos
echo e use o app para acompanhar os proximos testes.
pause
exit /b 0

:erro
echo.
echo [ERRO] Falha ao compilar o Koda Connect.
pause
exit /b 1
