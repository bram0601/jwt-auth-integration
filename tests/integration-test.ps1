# ============================================================
# Integration Test Suite - JWT Auth Integration
# Tests the full cross-application authentication flow.
# Requires both servers running:
#   PHP Auth App on localhost:8080
#   Java API App on localhost:8081
# ============================================================

$ErrorActionPreference = "Stop"
$phpBase = "http://localhost:8080"
$javaBase = "http://localhost:8081/java-api-app/api"
$passed = 0
$failed = 0
$total = 0
$testEmail = "integration-$(Get-Random)@test.com"
$testPassword = "IntTest_$(Get-Random)"
$testName = "Integration Tester"

function Test-Case {
    param(
        [string]$Name,
        [scriptblock]$Test
    )
    $script:total++
    try {
        & $Test
        Write-Host "  PASS: $Name" -ForegroundColor Green
        $script:passed++
    } catch {
        Write-Host "  FAIL: $Name" -ForegroundColor Red
        Write-Host "        $($_.Exception.Message)" -ForegroundColor DarkRed
        $script:failed++
    }
}

function Assert-Equal($expected, $actual, $message) {
    if ($expected -ne $actual) {
        throw "Expected '$expected' but got '$actual'. $message"
    }
}

function Assert-NotNull($value, $message) {
    if ($null -eq $value -or $value -eq "") {
        throw "Expected non-null value. $message"
    }
}

function Invoke-Api {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [string]$Body = $null,
        [hashtable]$Headers = @{},
        [switch]$ExpectError
    )
    $params = @{
        Uri = $Uri
        Method = $Method
        ContentType = "application/json"
        Headers = $Headers
    }
    if ($Body) { $params.Body = $Body }

    if ($ExpectError) {
        try {
            Invoke-RestMethod @params
            throw "Expected error but request succeeded"
        } catch {
            $ex = $_.Exception
            if ($ex -is [System.Net.WebException] -and $ex.Response) {
                $reader = New-Object System.IO.StreamReader($ex.Response.GetResponseStream())
                $errorBody = $reader.ReadToEnd()
                try { return ($errorBody | ConvertFrom-Json) } catch { return $null }
            }
            # Any other exception means the request failed, which is expected
            return $null
        }
    } else {
        return Invoke-RestMethod @params
    }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " JWT Auth Integration Tests" -ForegroundColor Cyan
Write-Host " PHP: $phpBase | Java: $javaBase" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ---- PHP AUTH APP TESTS ----
Write-Host "[PHP Auth App]" -ForegroundColor Yellow

Test-Case "Register - success" {
    $body = @{ email = $testEmail; password = $testPassword; name = $testName } | ConvertTo-Json
    $res = Invoke-Api -Uri "$phpBase/register" -Method POST -Body $body
    Assert-Equal "User registered successfully" $res.message
    Assert-NotNull $res.user.id "User ID should be set"
    Assert-Equal $testEmail $res.user.email
    Assert-Equal $testName $res.user.name
    $script:userId = $res.user.id
}

Test-Case "Register - duplicate email rejected (409)" {
    $body = @{ email = $testEmail; password = "x"; name = "x" } | ConvertTo-Json
    $err = Invoke-Api -Uri "$phpBase/register" -Method POST -Body $body -ExpectError
}

Test-Case "Register - missing fields rejected (400)" {
    $body = @{ email = "x@x.com" } | ConvertTo-Json
    $err = Invoke-Api -Uri "$phpBase/register" -Method POST -Body $body -ExpectError
}

Test-Case "Login - success" {
    $body = @{ email = $testEmail; password = $testPassword } | ConvertTo-Json
    $res = Invoke-Api -Uri "$phpBase/login" -Method POST -Body $body
    Assert-Equal "Login successful" $res.message
    Assert-NotNull $res.token "Token should be returned"
    Assert-Equal $testEmail $res.user.email
    $script:token = $res.token
}

Test-Case "Login - wrong password rejected (401)" {
    $body = @{ email = $testEmail; password = "wrongpassword" } | ConvertTo-Json
    $err = Invoke-Api -Uri "$phpBase/login" -Method POST -Body $body -ExpectError
}

Test-Case "Login - nonexistent email rejected (401)" {
    $body = @{ email = "nobody-$(Get-Random)@test.com"; password = "x" } | ConvertTo-Json
    $err = Invoke-Api -Uri "$phpBase/login" -Method POST -Body $body -ExpectError
}

Test-Case "Login - missing fields rejected (400)" {
    $body = @{ email = $testEmail } | ConvertTo-Json
    $err = Invoke-Api -Uri "$phpBase/login" -Method POST -Body $body -ExpectError
}

Test-Case "GET /me - valid token returns profile" {
    $res = Invoke-Api -Uri "$phpBase/me" -Headers @{ Authorization = "Bearer $script:token" }
    Assert-Equal $script:userId $res.user.id
    Assert-Equal $testEmail $res.user.email
    Assert-Equal $testName $res.user.name
    Assert-NotNull $res.user.created_at "created_at should be set"
}

Test-Case "GET /me - missing token rejected (401)" {
    $err = Invoke-Api -Uri "$phpBase/me" -ExpectError
}

Test-Case "GET /me - invalid token rejected (401)" {
    $err = Invoke-Api -Uri "$phpBase/me" -Headers @{ Authorization = "Bearer invalid.fake.token" } -ExpectError
}

Test-Case "404 for unknown route" {
    $err = Invoke-Api -Uri "$phpBase/unknown" -ExpectError
}

Write-Host ""

# ---- JAVA API APP TESTS ----
Write-Host "[Java API App - JWT Cross-App Validation]" -ForegroundColor Yellow

Test-Case "List products - no token rejected (401)" {
    $err = Invoke-Api -Uri "$javaBase/products" -ExpectError
}

Test-Case "List products - invalid token rejected (401)" {
    $err = Invoke-Api -Uri "$javaBase/products" -Headers @{ Authorization = "Bearer garbage.token.here" } -ExpectError
}

Test-Case "List products - valid PHP-issued token accepted" {
    $res = Invoke-Api -Uri "$javaBase/products" -Headers @{ Authorization = "Bearer $script:token" }
    Assert-NotNull $res.products "Should return products array"
}

$productName = "TestProduct-$(Get-Random)"
Test-Case "Create product - success" {
    $body = @{ name = $productName; description = "Test product"; price = 49.99 } | ConvertTo-Json
    $res = Invoke-Api -Uri "$javaBase/products-create" -Method POST -Body $body -Headers @{ Authorization = "Bearer $script:token" }
    Assert-Equal "Product created successfully" $res.message
    Assert-Equal $productName $res.product.name
    Assert-Equal "Test product" $res.product.description
    Assert-Equal $script:userId.ToString() $res.product.created_by "created_by should match JWT sub"
    $script:productId = $res.product.id
}

Test-Case "Create product - missing fields rejected (400)" {
    $body = @{ description = "no name or price" } | ConvertTo-Json
    $err = Invoke-Api -Uri "$javaBase/products-create" -Method POST -Body $body -Headers @{ Authorization = "Bearer $script:token" } -ExpectError
}

Test-Case "Create product - no token rejected (401)" {
    $body = @{ name = "x"; price = 1 } | ConvertTo-Json
    $err = Invoke-Api -Uri "$javaBase/products-create" -Method POST -Body $body -ExpectError
}

Test-Case "Get product by ID - success" {
    $res = Invoke-Api -Uri "$javaBase/products-get?id=$($script:productId)" -Headers @{ Authorization = "Bearer $script:token" }
    Assert-Equal $script:productId $res.product.id
    Assert-Equal $productName $res.product.name
}

Test-Case "Get product by ID - not found (404)" {
    $err = Invoke-Api -Uri "$javaBase/products-get?id=999999" -Headers @{ Authorization = "Bearer $script:token" } -ExpectError
}

Test-Case "Get product - invalid ID (404)" {
    $err = Invoke-Api -Uri "$javaBase/products-get?id=0" -Headers @{ Authorization = "Bearer $script:token" } -ExpectError
}

Test-Case "List products - contains newly created product" {
    $res = Invoke-Api -Uri "$javaBase/products" -Headers @{ Authorization = "Bearer $script:token" }
    $found = $res.products | Where-Object { $_.name -eq $productName }
    Assert-NotNull $found "Newly created product should appear in list"
}

Write-Host ""

# ---- CROSS-APP VERIFICATION ----
Write-Host "[Cross-App JWT Verification]" -ForegroundColor Yellow

Test-Case "Token sub claim matches user ID across apps" {
    $phpMe = Invoke-Api -Uri "$phpBase/me" -Headers @{ Authorization = "Bearer $script:token" }
    $javaProducts = Invoke-Api -Uri "$javaBase/products" -Headers @{ Authorization = "Bearer $script:token" }
    $myProduct = $javaProducts.products | Where-Object { $_.name -eq $productName }
    Assert-Equal $phpMe.user.id.ToString() $myProduct.created_by "PHP user.id should match Java created_by"
}

Test-Case "Token is reusable across multiple requests" {
    $r1 = Invoke-Api -Uri "$phpBase/me" -Headers @{ Authorization = "Bearer $script:token" }
    $r2 = Invoke-Api -Uri "$javaBase/products" -Headers @{ Authorization = "Bearer $script:token" }
    $r3 = Invoke-Api -Uri "$phpBase/me" -Headers @{ Authorization = "Bearer $script:token" }
    Assert-Equal $r1.user.id $r3.user.id "Same token should return same user"
}

# ---- SUMMARY ----
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Results: $passed passed, $failed failed, $total total" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "============================================" -ForegroundColor Cyan

exit $failed
