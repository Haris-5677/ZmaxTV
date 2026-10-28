# Restore script for "stable Zamax TV"
param(
    [string]$TvHost = "192.168.1.11"
)
$ErrorActionPreference = "Stop"
$scriptDir = $PSScriptRoot
$repoDir = (Get-Item $scriptDir).Parent.Parent.FullName
$py = Join-Path $repoDir "tools\venv\Scripts\python.exe"
$tv = Join-Path $repoDir "tools\tv.py"
$env:TV_HOST = $TvHost

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Restoring TV: stable Zamax TV            " -ForegroundColor Cyan
Write-Host " Target: $TvHost                          " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Check TV connectivity
Write-Host "Probing TV connection..."
& $py $tv --probe
if ($LASTEXITCODE -ne 0) {
    Write-Error "Cannot reach TV at $TvHost:5555. Please ensure TV is turned on and connected."
    exit 1
}

# 2. Re-install Zamax TV Launcher if missing or reinstalling
$homescreenApk = Join-Path $scriptDir "apks\HomeScreen.apk"
if (Test-Path $homescreenApk) {
    Write-Host "Verifying / installing Zamax TV Launcher (HomeScreen.apk)..."
    & $py (Join-Path $repoDir "tools\b64push.py") $homescreenApk "/data/local/tmp/HomeScreen.apk"
    & $py $tv "pm install -r -d /data/local/tmp/HomeScreen.apk"
    & $py $tv "rm /data/local/tmp/HomeScreen.apk"
}

# 3. Set Zamax TV Launcher as default HOME
Write-Host "Setting Zamax TV Launcher as default home activity..."
& $py $tv "pm enable com.gathin.tvhome"
& $py $tv "cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity"

# 4. Enforce Disabled Bloatware List
$disabledListFile = Join-Path $scriptDir "packages-disabled.txt"
if (Test-Path $disabledListFile) {
    Write-Host "Enforcing disabled package list (41 bloatware/telemetry packages)..."
    Get-Content $disabledListFile | ForEach-Object {
        $pkg = ($_ -replace '^package:', '').Trim()
        if ($pkg) {
            & $py $tv "pm disable-user --user 0 $pkg"
        }
    }
}

# 5. Launch Zamax Home
Write-Host "Launching Zamax TV Home..."
& $py $tv "am start -n com.gathin.tvhome/.MainActivity"

Write-Host "==========================================" -ForegroundColor Green
Write-Host " Restoration of 'stable Zamax TV' complete! " -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
