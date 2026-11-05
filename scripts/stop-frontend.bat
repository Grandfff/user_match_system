@echo off
title Stop Frontend (:5173)
setlocal
set PORT=5173
set PID=
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do set PID=%%a
if not defined PID (
    echo Port %PORT% is not in use. Nothing to stop.
) else (
    echo Stopping frontend on port %PORT% (PID %PID%) ...
    taskkill /F /PID %PID%
)
echo.
pause
