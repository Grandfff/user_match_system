@echo off
title Start Backend (Spring Boot :8080)
echo ============================================
echo   Starting backend project (Spring Boot)
echo   Folder: %~dp0..\user_match_backend
echo ============================================
cd /d "%~dp0..\user_match_backend"
call mvn spring-boot:run
echo.
echo Backend stopped.
pause
