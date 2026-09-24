@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ==========================================
echo   Koda Music 3.11.0 - GitHub Release
echo ==========================================
echo.
echo Etapa 1/2: gerando Portable validado...
call gradlew.bat :core:clean :desktop:clean :desktop:portableZip --console=plain
if errorlevel 1 goto :fail

echo.
echo Etapa 2/2: gerando instaladores EXE e MSI...
call gradlew.bat :desktop:packageExe :desktop:packageMsi --console=plain
if errorlevel 1 goto :fail

if not exist "release" mkdir "release"
for %%F in ("desktop\build\compose\binaries\main\exe\*.exe") do copy /Y "%%~fF" "release\" >nul
for %%F in ("desktop\build\compose\binaries\main\msi\*.msi") do copy /Y "%%~fF" "release\" >nul

echo.
echo ==========================================
echo   ARQUIVOS PARA O GITHUB PRONTOS
echo ==========================================
echo.
echo Pasta:
echo   %CD%\release
echo.
start "" explorer.exe "%CD%\release"
pause
exit /b 0

:fail
echo.
echo ==========================================
echo   RELEASE BUILD FALHOU
echo ==========================================
echo.
echo Nada deve ser publicado ate o erro acima ser corrigido.
echo.
pause
exit /b 1
