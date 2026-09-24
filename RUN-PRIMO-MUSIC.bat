@echo off
setlocal
cd /d "%~dp0"
echo.
echo ==========================================
echo       Koda Music - Windows Desktop
echo ==========================================
echo.
call gradlew.bat :desktop:run
if errorlevel 1 (
  echo.
  echo O Koda Music nao iniciou. Veja o erro acima.
  pause
)
endlocal
