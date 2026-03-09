
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
        Write-Error "No warehouses found."
        exit
    }
} catch {
    Write-Error "Failed to get warehouses: $_"
    exit
}

# 3. Create Item
$itemBody = @{
    name = "Test Item $random"
    code = "ITM$random"
    sku = "SKU$random"
    description = "Test Item Description"
    unitPrice = 100.00
    taxRate = 18.00
    hsnCode = "123456"
    uom = "PCS"
} | ConvertTo-Json

try {
    $itemResponse = Invoke-RestMethod -Uri "$baseUrl/items" -Method Post -Headers $headers -Body $itemBody -ContentType "application/json"
    $itemId = $itemResponse.id
    Write-Host "Item created. Using Item ID: $itemId"
} catch {
    Write-Error "Failed to create item: $_"
    exit
}

# 4. Create Parties
$customerBody = @{
    name = "Test Customer $random"
    type = "CUSTOMER"
    gstin = "27ABCDE$((Get-Random -Minimum 1000 -Maximum 9999))F1Z5"
    stateCode = "27"
} | ConvertTo-Json

$vendorBody = @{
    name = "Test Vendor $random"
    type = "VENDOR"
    gstin = "27ABCDE$((Get-Random -Minimum 1000 -Maximum 9999))F1Z5"
    stateCode = "27"
} | ConvertTo-Json

try {
    $customerResponse = Invoke-RestMethod -Uri "$baseUrl/parties" -Method Post -Headers $headers -Body $customerBody -ContentType "application/json"
    $customerId = $customerResponse.id
    Write-Host "Customer created. ID: $customerId"

    $vendorResponse = Invoke-RestMethod -Uri "$baseUrl/parties" -Method Post -Headers $headers -Body $vendorBody -ContentType "application/json"
    $vendorId = $vendorResponse.id
    Write-Host "Vendor created. ID: $vendorId"
} catch {
    Write-Error "Failed to create parties: $_"
    exit
}

# 5. Create Sales Invoice (Qty 10)
$salesInvoiceBody = @{
    invoiceDate = (Get-Date).ToString("yyyy-MM-dd")
    partyId = $customerId
    warehouseId = $warehouseId
    lines = @(
        @{
            itemId = $itemId
            quantity = 10
            unitPrice = 100.00
        }
    )
} | ConvertTo-Json -Depth 5

try {
    $salesInvoiceResponse = Invoke-RestMethod -Uri "$baseUrl/sales-invoices" -Method Post -Headers $headers -Body $salesInvoiceBody -ContentType "application/json"
    $salesInvoiceId = $salesInvoiceResponse.id
    Write-Host "Sales Invoice created. ID: $salesInvoiceId"
} catch {
    Write-Error "Failed to create sales invoice: $_"
    exit
}

# 6. Create Sales Return (Qty 5) -> Expect Success
$salesReturnBody1 = @{
    returnDate = (Get-Date).ToString("yyyy-MM-dd")
    salesInvoiceId = $salesInvoiceId
    partyId = $customerId
    warehouseId = $warehouseId
    lines = @(
        @{
            itemId = $itemId
            quantity = 5
            unitPrice = 100.00
        }
    )
} | ConvertTo-Json -Depth 5

try {
    $salesReturnResponse1 = Invoke-RestMethod -Uri "$baseUrl/sales-returns" -Method Post -Headers $headers -Body $salesReturnBody1 -ContentType "application/json"
    Write-Host "Sales Return (Qty 5) created successfully. ID: $($salesReturnResponse1.id)"
} catch {
    Write-Error "Failed to create sales return 1: $_"
    exit
}

# 7. Create Sales Return (Qty 6) -> Expect Failure (5+6 > 10)
$salesReturnBody2 = @{
    returnDate = (Get-Date).ToString("yyyy-MM-dd")
    salesInvoiceId = $salesInvoiceId
    partyId = $customerId
    warehouseId = $warehouseId
    lines = @(
        @{
            itemId = $itemId
            quantity = 6
            unitPrice = 100.00
        }
    )
} | ConvertTo-Json -Depth 5

try {
    Invoke-RestMethod -Uri "$baseUrl/sales-returns" -Method Post -Headers $headers -Body $salesReturnBody2 -ContentType "application/json"
    Write-Error "Sales Return (Qty 6) SHOULD HAVE FAILED but succeeded!"
} catch {
    Write-Host "Sales Return (Qty 6) failed as expected: $($_.Exception.Message)"
}

# 8. Create Purchase Invoice (Qty 10)
$purchaseInvoiceBody = @{
    invoiceDate = (Get-Date).ToString("yyyy-MM-dd")
    partyId = $vendorId
    warehouseId = $warehouseId
    lines = @(
        @{
            itemId = $itemId
            quantity = 10
            unitPrice = 80.00
        }
    )
} | ConvertTo-Json -Depth 5

try {
    $purchaseInvoiceResponse = Invoke-RestMethod -Uri "$baseUrl/purchase-invoices" -Method Post -Headers $headers -Body $purchaseInvoiceBody -ContentType "application/json"
    $purchaseInvoiceId = $purchaseInvoiceResponse.id
    Write-Host "Purchase Invoice created. ID: $purchaseInvoiceId"
} catch {
    Write-Error "Failed to create purchase invoice: $_"
    exit
}

# 9. Create Purchase Return (Qty 5) -> Expect Success
$purchaseReturnBody1 = @{
    returnDate = (Get-Date).ToString("yyyy-MM-dd")
    purchaseInvoiceId = $purchaseInvoiceId
    partyId = $vendorId
    warehouseId = $warehouseId
    lines = @(
        @{
            itemId = $itemId
            quantity = 5
            unitPrice = 80.00
        }
    )
} | ConvertTo-Json -Depth 5

try {
    $purchaseReturnResponse1 = Invoke-RestMethod -Uri "$baseUrl/purchase-returns" -Method Post -Headers $headers -Body $purchaseReturnBody1 -ContentType "application/json"
    Write-Host "Purchase Return (Qty 5) created successfully. ID: $($purchaseReturnResponse1.id)"
} catch {
    Write-Error "Failed to create purchase return 1: $_"
    exit
}

# 10. Create Purchase Return (Qty 6) -> Expect Failure (5+6 > 10)
$purchaseReturnBody2 = @{
    returnDate = (Get-Date).ToString("yyyy-MM-dd")
    purchaseInvoiceId = $purchaseInvoiceId
    partyId = $vendorId
    warehouseId = $warehouseId
    lines = @(
        @{
            itemId = $itemId
            quantity = 6
            unitPrice = 80.00
        }
    )
} | ConvertTo-Json -Depth 5

try {
    Invoke-RestMethod -Uri "$baseUrl/purchase-returns" -Method Post -Headers $headers -Body $purchaseReturnBody2 -ContentType "application/json"
    Write-Error "Purchase Return (Qty 6) SHOULD HAVE FAILED but succeeded!"
} catch {
    Write-Host "Purchase Return (Qty 6) failed as expected: $($_.Exception.Message)"
}
