@echo off
chcp 65001 >nul
title Koda Cut v0.4
cd /d "%~dp0"

where javac >nul 2>&1
if errorlevel 1 (
  echo.
  echo [ERRO] JDK 17 ou superior nao encontrado.
  echo Instale um JDK 17+ completo e tente novamente.
  echo.
  pause
  exit /b 1
)

if not exist "out" mkdir "out"

echo Compilando Koda Cut v0.4...
javac -encoding UTF-8 --release 17 -d out src\kodacut\*.java
if errorlevel 1 (
  echo.
  echo [ERRO] Falha na compilacao.
  pause
  exit /b 1
)

echo Abrindo Koda Cut...
java -cp out kodacut.KodaCut
