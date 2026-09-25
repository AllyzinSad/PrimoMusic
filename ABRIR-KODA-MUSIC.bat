@echo off
setlocal
cd /d "%~dp0"
echo Iniciando Koda Music 3.11.0 a partir do codigo-fonte...
echo Requer JDK 21 e internet no primeiro uso.
call gradlew.bat :desktop:run --console=plain
if errorlevel 1 (
  echo.
  echo Falha ao iniciar. Copie o trecho "What went wrong" para diagnostico.
  pause
)
endlocal
