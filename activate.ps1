# Activate Development Environment
# Run this script in your PowerShell session: . .\activate.ps1

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $ScriptDir) {
    $ScriptDir = Get-Location
}

$ToolsDir = Join-Path $ScriptDir "tools"

# Paths to tools
$JdkPath = Join-Path $ToolsDir "jdk-21"
$MavenPath = Join-Path $ToolsDir "apache-maven-3.9.6"
$NodePath = Join-Path $ToolsDir "node-v20.11.1-win-x64"
$PgPath = Join-Path $ToolsDir "pgsql"

# Set JAVA_HOME
$env:JAVA_HOME = $JdkPath
Write-Host "Set JAVA_HOME to $env:JAVA_HOME"

# Update PATH
$env:Path = "$JdkPath\bin;$MavenPath\bin;$NodePath;$PgPath\bin;$env:Path"
Write-Host "Added Java, Maven, Node.js, and PostgreSQL to PATH"

# Verify versions
Write-Host "Verifying versions..."
& "$JdkPath\bin\java.exe" -version
& "$MavenPath\bin\mvn.cmd" -version
& "$NodePath\node.exe" -v
& "$PgPath\bin\postgres.exe" --version

Write-Host "Environment activated!"
