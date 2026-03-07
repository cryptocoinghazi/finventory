# Stops Finventory Local Environment

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path "$ScriptDir\..").Path
$ToolsDir = Join-Path $ProjectRoot "tools"
$PgPath = Join-Path $ToolsDir "pgsql"
$DataDir = Join-Path $ToolsDir "pgdata"

# Activate Environment
Write-Host "Activating Local Environment..." -ForegroundColor Cyan
. "$ProjectRoot\activate.ps1"

# Stop Postgres
Write-Host "Stopping PostgreSQL..." -ForegroundColor Cyan
if (Get-Process "postgres" -ErrorAction SilentlyContinue) {
    & "$PgPath\bin\pg_ctl.exe" stop -D "$DataDir"
    Write-Host "PostgreSQL stopped." -ForegroundColor Green
} else {
    Write-Host "PostgreSQL is not running." -ForegroundColor Yellow
}

# Stop Backend
Write-Host "Stopping Backend..." -ForegroundColor Cyan

if ($env:JAVA_HOME) {
    $JpsExe = Join-Path $env:JAVA_HOME "bin\jps.exe"
    if (Test-Path $JpsExe) {
        $JpsOutput = & $JpsExe -l
        $BackendInfos = $JpsOutput | Where-Object { $_ -match "com.finventory.FinventoryApplication" }
        
        if ($BackendInfos) {
            foreach ($Info in $BackendInfos) {
                $Parts = $Info.Trim() -split "\s+"
                $BackendPid = $Parts[0]
                if ($BackendPid) {
                    Stop-Process -Id $BackendPid -Force -ErrorAction SilentlyContinue
                    Write-Host "Backend (PID $BackendPid) stopped." -ForegroundColor Green
                }
            }
        } else {
            Write-Host "Backend application not found running." -ForegroundColor Yellow
        }
    } else {
         Write-Host "jps.exe not found. Cannot automatically stop backend." -ForegroundColor Yellow
    }
} else {
    Write-Host "JAVA_HOME not set. Cannot automatically stop backend." -ForegroundColor Yellow
}

Write-Host "Please close the Backend PowerShell window if it is still open." -ForegroundColor Yellow
