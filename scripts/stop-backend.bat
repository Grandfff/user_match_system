@echo off
title Stop Backend (:8080)
setlocal
set PORT=8080
set PID=
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do set PID=%%a
if not defined PID (
    echo Port %PORT% is not in use. Nothing to stop.
) else (
    echo Stopping backend on port %PORT% (PID %PID%) ...
    taskkill /F /PID %PID%
)
echo.
pause
