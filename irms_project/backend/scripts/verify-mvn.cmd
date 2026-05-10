@echo off
setlocal
bash "%~dp0verify-mvn.sh"
exit /b %ERRORLEVEL%
