@echo off
chcp 65001 >nul
title Teste automatico - Koda Connect

echo.
echo Testando Koda Connect...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
"$base='http://127.0.0.1:17777';" ^
"try {" ^
"  $h=Invoke-RestMethod ($base+'/health') -TimeoutSec 3;" ^
"  Write-Host '[OK] Servidor:' $h.app $h.version -ForegroundColor Green;" ^
"  $s=Invoke-RestMethod ($base+'/api/status') -TimeoutSec 3;" ^
"  Write-Host '[OK] Motor:' $s.engine -ForegroundColor Green;" ^
"  Write-Host '[OK] Workspace:' $s.workspace -ForegroundColor Green;" ^
"  $f=Invoke-RestMethod ($base+'/api/files') -TimeoutSec 3;" ^
"  $count=@($f.files).Count;" ^
"  Write-Host '[OK] Arquivos autorizados:' $count -ForegroundColor Green;" ^
"  if($count -eq 0){Write-Host '[DICA] Coloque um video na pasta KodaConnectWorkspace\videos' -ForegroundColor Yellow}" ^
"} catch {" ^
"  Write-Host '[ERRO] Nao foi possivel falar com o Koda Connect.' -ForegroundColor Red;" ^
"  Write-Host 'Abra primeiro INSTALAR-E-ABRIR-KODA-CONNECT.bat';" ^
"  exit 1" ^
"}"

echo.
pause
