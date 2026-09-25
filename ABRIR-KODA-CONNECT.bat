@echo off
chcp 65001 >nul
title Koda Connect Beta
cd /d "%~dp0"

where javac >nul 2>&1
if errorlevel 1 (
  echo [ERRO] JDK 17+ nao encontrado.
  echo Instale um JDK 17 ou superior e tente novamente.
  pause
  exit /b 1
)

if not exist out mkdir out

echo Compilando Koda Connect...
javac -encoding UTF-8 --release 17 --add-modules jdk.httpserver -d out src\kodaconnect\*.java
if errorlevel 1 (
  echo.
  echo [ERRO] Falha ao compilar.
  pause
  exit /b 1
)

echo Abrindo Koda Connect...
java --add-modules jdk.httpserver -cp out kodaconnect.KodaConnect

pause
