@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ==========================================
echo   Primo Music 3.10.9 - Portable Clean Build
echo ==========================================
echo.
echo Uma unica pipeline: Gradle ^> Compose/jpackage ^> validacao ^> ZIP.
echo Sem reparos de classpath depois do build.
echo.

REM Clean only generated outputs. The verified mpv download cache lives in
REM .primo-cache and is intentionally preserved between builds.
call gradlew.bat :core:clean :desktop:clean :desktop:portableZip --console=plain
if errorlevel 1 goto :fail

echo.
echo ==========================================
echo   PORTABLE GERADO COM SUCESSO
echo ==========================================
echo.
echo ZIP:
echo   %CD%\release\PrimoMusic-3.10.9-Portable.zip
echo.
echo Extraia o ZIP em outra pasta e abra PrimoMusic.exe.
echo.
start "" explorer.exe "%CD%\release"
pause
exit /b 0

:fail
echo.
echo ==========================================
echo   O BUILD PORTABLE FALHOU
echo ==========================================
echo.
echo A validacao interrompeu o build antes de gerar um ZIP quebrado.
echo Envie o trecho "What went wrong" acima para diagnostico.
echo.
pause
exit /b 1
