@echo off
setlocal enabledelayedexpansion

echo =======================================================
echo          Zamax TV Home - Push to GitHub
echo =======================================================
echo.

where git >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Git is not installed or not in your PATH.
    echo Please install Git from https://git-scm.com/
    pause
    exit /b 1
)

git remote get-url origin >nul 2>&1
if %ERRORLEVEL% equ 0 (
    for /f "delims=" %%i in ('git remote get-url origin') do set CURRENT_REMOTE=%%i
    echo Current remote origin: !CURRENT_REMOTE!
    set /p CHANGE_REMOTE="Do you want to change this remote? (y/N): "
    if /i "!CHANGE_REMOTE!"=="y" (
        set /p REPO_URL="Enter your new GitHub repository URL: "
        git remote set-url origin !REPO_URL!
    )
) else (
    echo No remote repository configured yet.
    echo.
    echo Step 1: Create a new empty repository on https://github.com/new
    echo         (Do NOT initialize with README, .gitignore, or license)
    echo.
    set /p REPO_URL="Enter your GitHub repository URL (e.g. https://github.com/USERNAME/zamax-tv-home.git): "
    if "!REPO_URL!"=="" (
        echo [ERROR] No URL provided. Aborting.
        pause
        exit /b 1
    )
    git remote add origin !REPO_URL!
)

echo.
echo Setting branch to main...
git branch -M main

echo Pushing commits and tags to GitHub...
git push -u origin main

if %ERRORLEVEL% equ 0 (
    echo.
    echo =======================================================
    echo [SUCCESS] Zamax TV Home successfully pushed to GitHub!
    echo =======================================================
) else (
    echo.
    echo [NOTICE] If Git prompted for credentials, ensure you authenticate via browser or GitHub Personal Access Token (PAT).
)

echo.
pause
