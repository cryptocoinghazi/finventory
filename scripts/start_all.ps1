# Finventory Local Startup Script
# Starts Database, Backend, and (future) Frontend using local tools

$StartupScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path "$StartupScriptDir\..").Path

# 1. Activate Environment (for this script)
Write-Host "Activating Local Environment..." -ForegroundColor Cyan
. "$ProjectRoot\activate.ps1"

$ActivateScript = Join-Path $ProjectRoot "activate.ps1"

# 2. Start Database
Write-Host "Starting Database (New Window)..." -ForegroundColor Cyan
$DbScript = Join-Path $StartupScriptDir "start_db.ps1"
$DbCommand = "-NoExit -Command `". '$ActivateScript'; & '$DbScript' -ShowLogs`""
Start-Process powershell -ArgumentList $DbCommand -WorkingDirectory $StartupScriptDir

# Wait for DB to likely be up
Write-Host "Waiting 5 seconds for Database initialization..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

# 3. Start Backend
Write-Host "Starting Backend..." -ForegroundColor Cyan
$BackendDir = Join-Path $ProjectRoot "backend"

# Launch in a new window to keep this script clean and allow parallel frontend start later
# We explicitly source activate.ps1 in the new window to ensure maven/java are on PATH
$BackendCommand = "-NoExit -Command `". '$ActivateScript'; cd '$BackendDir'; mvn spring-boot:run`""
Start-Process powershell -ArgumentList $BackendCommand -WorkingDirectory $BackendDir

# 4. Start Frontend (Future)
# Write-Host "Starting Frontend..." -ForegroundColor Cyan
# $FrontendDir = Join-Path $ProjectRoot "web"
# $FrontendCommand = "-NoExit -Command `". '$ActivateScript'; cd '$FrontendDir'; npm run dev`""
# Start-Process powershell -ArgumentList $FrontendCommand -WorkingDirectory $FrontendDir

Write-Host "--------------------------------------------------------" -ForegroundColor Green
Write-Host "Finventory Backend is starting in a new window." -ForegroundColor Green
Write-Host "Database is running in a separate window." -ForegroundColor Green
Write-Host "Logs are visible in their respective windows." -ForegroundColor Green
Write-Host "--------------------------------------------------------" -ForegroundColor Green
Write-Host "To stop the application:" -ForegroundColor Yellow
Write-Host "1. Close the Backend PowerShell window." -ForegroundColor Yellow
Write-Host "2. Run 'scripts\stop_all.ps1' to stop the database." -ForegroundColor Yellow
Write-Host "   (Closing the DB window will NOT stop the database service)" -ForegroundColor DarkYellow
