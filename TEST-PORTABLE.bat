@echo off
setlocal
cd /d "%~dp0"
echo.
echo ==========================================
echo   Koda Music 3.11.0 - Teste Portatil
echo ==========================================
echo.
echo Este comando monta a mesma imagem autocontida usada na versao portable
echo e a executa com o runtime reduzido/empacotado, nao com o JDK do Gradle.
echo.
call gradlew.bat --stop
if errorlevel 1 goto :fail
call gradlew.bat :desktop:runDistributable
if errorlevel 1 goto :fail
exit /b 0

:fail
echo.
echo O teste portatil falhou. Veja o erro acima.
echo.
pause
exit /b 1
