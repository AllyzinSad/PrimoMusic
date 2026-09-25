@echo off
chcp 65001 >nul
title Configurar Koda Cut
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\configurar.ps1"
echo.
pause
