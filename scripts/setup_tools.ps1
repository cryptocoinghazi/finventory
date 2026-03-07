# Finventory Development Environment Setup Script

$ToolsDir = "tools"
$ErrorActionPreference = "Stop"

# URLs for tools
# Using Adoptium API for latest Java 21 LTS
$JdkUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse"
$MavenUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
$NodeUrl = "https://nodejs.org/dist/v20.11.1/node-v20.11.1-win-x64.zip"

if (-not (Test-Path $ToolsDir)) {
    New-Item -ItemType Directory -Path $ToolsDir | Out-Null
}

function Download-And-Extract ($url, $name, $destFolder) {
    $zipFile = Join-Path $ToolsDir "$name.zip"
    $targetDir = Join-Path $ToolsDir $destFolder
    
    # Check if target directory already exists (simple check)
    if (Test-Path $targetDir) {
        Write-Host "$name directory found at $targetDir. Skipping download."
        return
    }

    # Check if zip already exists
    if (-not (Test-Path $zipFile)) {
        Write-Host "Downloading $name from $url..."
        try {
            Invoke-WebRequest -Uri $url -OutFile $zipFile -UserAgent "PowerShell"
        } catch {
            Write-Error "Failed to download $name. Error: $_"
            return
        }
    } else {
        Write-Host "$name zip already exists."
    }
    
    Write-Host "Extracting $name..."
    try {
        Expand-Archive -Path $zipFile -DestinationPath $ToolsDir -Force
    } catch {
        Write-Error "Failed to extract $name. Error: $_"
        return
    }
    
    # Clean up zip
    Remove-Item $zipFile
    
    # Handle folder naming differences
    # Find the extracted folder
    $extracted = Get-ChildItem -Path $ToolsDir -Directory | Where-Object { $_.Name -like "*$name*" -or $_.Name -like "*jdk*" -or $_.Name -like "*maven*" -or $_.Name -like "*node*" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    
    if ($extracted) {
        Write-Host "Extracted to: $($extracted.Name)"
        # If we expected a specific folder name and it's different, we might want to rename it
        # But for now, let's just log it. The activate script might need adjustment if names vary wildly.
        # For Java, Adoptium extracts to something like 'jdk-21.0.2+13'.
        # We can rename it to a standard name to make activate.ps1 reliable.
        
        if ($name -eq "jdk-21" -and $extracted.Name -ne "jdk-21") {
            if (Test-Path (Join-Path $ToolsDir "jdk-21")) { Remove-Item (Join-Path $ToolsDir "jdk-21") -Recurse -Force }
            Rename-Item -Path $extracted.FullName -NewName "jdk-21"
        }
        elseif ($name -eq "maven" -and $extracted.Name -ne "apache-maven-3.9.6") {
             # Maven usually extracts correctly, but just in case
        }
    }
    
    Write-Host "$name setup complete."
}

# 1. Install Java 21
Download-And-Extract $JdkUrl "jdk-21" "jdk-21"

# 2. Install Maven
Download-And-Extract $MavenUrl "maven" "apache-maven-3.9.6"

# 3. Install Node.js
Download-And-Extract $NodeUrl "node" "node-v20.11.1-win-x64"

# 4. Install PostgreSQL (Portable)
$PgUrl = "https://get.enterprisedb.com/postgresql/postgresql-15.6-1-windows-x64-binaries.zip"
Download-And-Extract $PgUrl "pgsql" "pgsql"

Write-Host "All tools installed in $ToolsDir."
Write-Host "Run '.\activate.ps1' to set up your environment variables."
