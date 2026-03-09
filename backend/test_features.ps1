
$baseUrl = "http://localhost:8080/api/v1"

# Generate random user
$random = Get-Random -Minimum 1000 -Maximum 9999
$username = "testuser_$random"
$email = "test_$random@example.com"
$password = "testpass123"

Write-Host "Registering user: $username"

# 1. Register
$registerBody = @{
    username = $username
    email = $email
    password = $password
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -Body $registerBody -ContentType "application/json"
    $token = $registerResponse.token
    Write-Host "Registration successful. Token: $token"
} catch {
    Write-Error "Registration failed: $_"
    # Fallback to login if registration fails (e.g., if we were re-using a user, but here it is random)
    exit
}

$headers = @{
    Authorization = "Bearer $token"
}

# 2. Get Warehouses
try {
    $warehouses = Invoke-RestMethod -Uri "$baseUrl/warehouses" -Method Get -Headers $headers
    if ($warehouses.Count -gt 0) {
        $warehouseId = $warehouses[0].id
        Write-Host "Using Warehouse ID: $warehouseId"
    } else {
        # Create a warehouse if none exist (might need admin rights, skipping for now as seed data should exist)
        Write-Error "No warehouses found."
        exit
    }
} catch {
    Write-Error "Failed to get warehouses: $_"
    exit
}

# 3. Upload Items
$csvPath = "c:\Users\gsyed\Documents\trae_projects\Finventory\backend\test_items.csv"
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

try {
    $fileBytes = [System.IO.File]::ReadAllBytes($csvPath)
    $fileContent = [System.Text.Encoding]::GetEncoding('iso-8859-1').GetString($fileBytes)
    
    $bodyLines = (
        "--$boundary",
        "Content-Disposition: form-data; name=`"file`"; filename=`"test_items.csv`"",
        "Content-Type: text/csv",
        "",
        $fileContent,
        "--$boundary--"
    ) -join $LF

    $uploadResponse = Invoke-RestMethod -Uri "$baseUrl/items/upload" -Method Post -Headers @{Authorization="Bearer $token"; "Content-Type"="multipart/form-data; boundary=$boundary"} -Body $bodyLines
    
    $itemId = $uploadResponse[0].id
    Write-Host "Items uploaded. Using Item ID: $itemId"
} catch {
    Write-Error "Failed to upload items: $_"
    # Proceeding if upload fails might be tricky, let's try to list items instead
    try {
        $items = Invoke-RestMethod -Uri "$baseUrl/items" -Method Get -Headers $headers
        if ($items.Count -gt 0) {
            $itemId = $items[0].id
            Write-Host "Fallback: Using existing Item ID: $itemId"
        } else {
             Write-Error "No items found."
             exit
        }
    } catch {
        Write-Error "Failed to list items: $_"
        exit
    }
}

# 4. Create Stock Adjustment
$adjustmentBody = @{
    warehouseId = $warehouseId
    itemId = $itemId
    quantity = 10
    adjustmentDate = (Get-Date).ToString("yyyy-MM-dd")
    reason = "Test Adjustment"
} | ConvertTo-Json

try {
    $adjustmentResponse = Invoke-RestMethod -Uri "$baseUrl/stock-adjustments" -Method Post -Headers $headers -Body $adjustmentBody -ContentType "application/json"
    Write-Host "Stock Adjustment created: $($adjustmentResponse.adjustmentNumber)"
} catch {
    Write-Error "Failed to create stock adjustment: $_"
    exit
}
