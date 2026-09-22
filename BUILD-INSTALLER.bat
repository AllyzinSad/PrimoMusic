@echo off
setlocal
cd /d "%~dp0"
echo Gerando instaladores EXE e MSI do Primo Music 3.10.9...
echo O mpv sera incluido automaticamente no pacote.
call gradlew.bat :desktop:packageExe :desktop:packageMsi
if errorlevel 1 (
  echo.
  echo Falha ao gerar os instaladores. Veja o erro acima.
  pause
  exit /b 1
)
echo.
echo EXE: desktop\build\compose\binaries\main\exe\
echo MSI: desktop\build\compose\binaries\main\msi\
pause
endlocal
