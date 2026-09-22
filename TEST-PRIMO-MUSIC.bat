@echo off
setlocal
cd /d "%~dp0"
echo Compilando core e desktop sem iniciar o app...
call gradlew.bat :core:compileKotlin :desktop:compileKotlin
if errorlevel 1 (
  echo.
  echo BUILD FAILED - veja o erro acima.
  pause
  exit /b 1
)
echo.
echo BUILD OK.
pause
endlocal
