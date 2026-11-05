@echo off
title Start Frontend (Vite :5173)
echo ============================================
echo   Starting frontend project (Vue 3 + Vite)
echo   Folder: %~dp0..\user_match_frontend
echo ============================================
cd /d "%~dp0..\user_match_frontend"
call npm run dev
echo.
echo Frontend stopped.
pause
