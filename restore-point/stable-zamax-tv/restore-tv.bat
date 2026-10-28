@echo off
setlocal
set TV_HOST=192.168.1.11
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0restore-tv.ps1" -TvHost "%TV_HOST%"
pause
