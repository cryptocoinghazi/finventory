# Test Authentication Endpoints
# Usage: .\scripts\test_auth.ps1

$BaseUrl = "http://localhost:8080/api"

# 1. Health Check
Write-Host "Checking Health..."
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/health" -Method Get
    Write-Host "Health: $($health | ConvertTo-Json)" -ForegroundColor Green
} catch {
    Write-Error "Health check failed: $_"
    exit
}

# 2. Register User
Write-Host "`nRegistering User..."
$User = "user_$(Get-Random)"
$Email = "$User@example.com"
$Password = "password123"

$RegisterBody = @{
    username = $User
    email = $Email
    password = $Password
} | ConvertTo-Json

try {
    $RegisterResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post -Body $RegisterBody -ContentType "application/json"
    $Token = $RegisterResponse.token
    Write-Host "Registered successfully. Token: $Token" -ForegroundColor Green
} catch {
    Write-Error "Registration failed: $_"
    exit
}

# 3. Access Protected Endpoint
Write-Host "`nAccessing Protected Endpoint..."
try {
    $TestResponse = Invoke-RestMethod -Uri "$BaseUrl/test" -Method Get -Headers @{ Authorization = "Bearer $Token" }
    Write-Host "Response: $TestResponse" -ForegroundColor Green
} catch {
    Write-Error "Access failed: $_"
}

# 4. Access Protected Endpoint without Token
Write-Host "`nAccessing Protected Endpoint without Token (Should Fail)..."
try {
    Invoke-RestMethod -Uri "$BaseUrl/test" -Method Get
    Write-Error "Access succeeded but should have failed!"
} catch {
    Write-Host "Access failed as expected: $($_.Exception.Message)" -ForegroundColor Green
}
