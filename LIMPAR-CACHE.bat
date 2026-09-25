@echo off
chcp 65001 >nul
title Limpar Cache - Koda Cut
cd /d "%~dp0"
if exist temp rmdir /s /q temp
mkdir temp
echo Cache limpo.
pause
