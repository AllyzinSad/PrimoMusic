@echo off
setlocal
cd /d "%~dp0"

echo.
echo ==========================================
echo      Koda Music 3.11.0 - Release Build
echo ==========================================
echo.
echo O mpv sera preparado DURANTE a compilacao e embutido
echo nos instaladores. O usuario final nao baixa dependencias.
echo.

call gradlew.bat --stop
if errorlevel 1 goto :fail

call gradlew.bat :core:compileKotlin :desktop:compileKotlin
if errorlevel 1 goto :fail

call gradlew.bat :desktop:packageExe :desktop:packageMsi
if errorlevel 1 goto :fail

if not exist "release" mkdir "release"
for %%F in ("desktop\build\compose\binaries\main\exe\*.exe") do copy /Y "%%~fF" "release\" >nul
for %%F in ("desktop\build\compose\binaries\main\msi\*.msi") do copy /Y "%%~fF" "release\" >nul

echo.
echo Build concluido.
echo Instaladores EXE e MSI copiados para:
echo   %CD%\release\
echo.
pause
exit /b 0

:fail
echo.
echo A compilacao falhou. Veja o erro acima antes de publicar no GitHub.
echo.
pause
exit /b 1
