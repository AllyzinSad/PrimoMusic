@echo off
setlocal
cd /d "%~dp0"
echo.
echo ==========================================
echo       Primo Music - Windows Desktop
echo ==========================================
echo.
call gradlew.bat :desktop:run
if errorlevel 1 (
  echo.
  echo O Primo Music nao iniciou. Veja o erro acima.
  pause
)
endlocal
