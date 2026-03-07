# Initialize and Start Local PostgreSQL
# Usage: .\scripts\start_db.ps1 [-ShowLogs]

param (
    [switch]$ShowLogs
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path "$ScriptDir\..").Path
$ToolsDir = Join-Path $ProjectRoot "tools"
$PgPath = Join-Path $ToolsDir "pgsql"
$DataDir = Join-Path $ToolsDir "pgdata"

$ErrorActionPreference = "Stop"

if (-not (Test-Path "$PgPath\bin\initdb.exe")) {
    Write-Error "PostgreSQL not found at $PgPath. Please run setup_tools.ps1 first."
}

# Initialize DB if not exists
if (-not (Test-Path $DataDir)) {
    Write-Host "Initializing PostgreSQL data directory at $DataDir..."
    & "$PgPath\bin\initdb.exe" -D "$DataDir" -U postgres -A password -E UTF8 --pwfile "$ScriptDir\pgpass.txt"
} else {
    Write-Host "PostgreSQL data directory exists at $DataDir."
}

# Start DB
Write-Host "Starting PostgreSQL on port 5432..."
$LogFile = Join-Path $ToolsDir "pg.log"

# Check if already running
if (Get-Process "postgres" -ErrorAction SilentlyContinue) {
    Write-Host "Postgres is already running."
} else {
    Start-Process -FilePath "$PgPath\bin\pg_ctl.exe" -ArgumentList "start -D `"$DataDir`" -l `"$LogFile`"" -Wait
    Write-Host "PostgreSQL started. Logs at $LogFile"
}

# Read password
$Password = Get-Content (Join-Path $ScriptDir "pgpass.txt") -Raw
$env:PGPASSWORD = $Password.Trim()

# Create Database if not exists
Start-Sleep -Seconds 2
& "$PgPath\bin\createdb.exe" -U postgres -h localhost finventory
if ($LASTEXITCODE -eq 0) {
    Write-Host "Database 'finventory' created."
} else {
    Write-Host "Database 'finventory' might already exist."
}

if ($ShowLogs) {
    Write-Host "Tailing PostgreSQL logs..." -ForegroundColor Cyan
    Get-Content $LogFile -Wait -Tail 10
}
