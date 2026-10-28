# Zamax TV Home - Push to GitHub
param(
    [string]$RepoUrl
)

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "         Zamax TV Home - Push to GitHub" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host ""

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Write-Error "Git is not installed or not in your PATH. Please install Git from https://git-scm.com/"
    exit 1
}

$origin = git remote get-url origin 2>$null
if ($origin) {
    Write-Host "Current remote origin: $origin" -ForegroundColor Yellow
    if (-not $RepoUrl) {
        $change = Read-Host "Do you want to change this remote? (y/N)"
        if ($change -match "^[yY]") {
            $RepoUrl = Read-Host "Enter your new GitHub repository URL"
            git remote set-url origin $RepoUrl
        }
    } else {
        git remote set-url origin $RepoUrl
    }
} else {
    if (-not $RepoUrl) {
        Write-Host "No remote repository configured yet."
        Write-Host "Step 1: Create a new empty repository on https://github.com/new" -ForegroundColor Green
        Write-Host "        (Do NOT initialize with README, .gitignore, or license)`n"
        $RepoUrl = Read-Host "Enter your GitHub repository URL (e.g. https://github.com/USERNAME/zamax-tv-home.git)"
    }
    if ([string]::IsNullOrWhiteSpace($RepoUrl)) {
        Write-Error "No repository URL provided. Aborting."
        exit 1
    }
    git remote add origin $RepoUrl
}

Write-Host "`nSetting branch to main..." -ForegroundColor Gray
git branch -M main

Write-Host "Pushing to GitHub..." -ForegroundColor Cyan
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=======================================================" -ForegroundColor Green
    Write-Host "[SUCCESS] Zamax TV Home successfully pushed to GitHub!" -ForegroundColor Green
    Write-Host "=======================================================" -ForegroundColor Green
} else {
    Write-Host "`n[NOTICE] Push failed or was cancelled. Check your GitHub permissions or token." -ForegroundColor Yellow
}
