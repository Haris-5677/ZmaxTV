# Verification script for "stable Zamax TV" restore point
param(
    [string]$TvHost = "192.168.1.11"
)
$ErrorActionPreference = "Continue"
$scriptDir = $PSScriptRoot
$repoDir = (Get-Item $scriptDir).Parent.Parent.FullName
$py = Join-Path $repoDir "tools\venv\Scripts\python.exe"
$tv = Join-Path $repoDir "tools\tv.py"
$env:TV_HOST = $TvHost

Write-Host "Verifying TV against restore point 'stable Zamax TV'..." -ForegroundColor Cyan

# Check default launcher
$launcherInfo = & $py $tv "cmd package resolve-activity -a android.intent.action.MAIN -c android.intent.category.HOME"
if ($launcherInfo -match "com\.gathin\.tvhome") {
    Write-Host "[OK] Zamax TV (com.gathin.tvhome) is default HOME launcher." -ForegroundColor Green
} else {
    Write-Host "[WARN] Zamax TV is NOT currently default launcher!" -ForegroundColor Yellow
}

# Check disabled packages
$liveDisabled = & $py $tv "pm list packages -d"
$expectedDisabled = Get-Content (Join-Path $scriptDir "packages-disabled.txt")
$diff = Compare-Object $expectedDisabled $liveDisabled
if (-not $diff) {
    Write-Host "[OK] All 41 bloatware/telemetry packages match expected disabled status." -ForegroundColor Green
} else {
    Write-Host "[INFO] Some package states differ:" -ForegroundColor Yellow
    $diff | Format-Table -AutoSize
}
