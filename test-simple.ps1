# Get token
$loginBody = @{
    username = "superadmin"
    password = "admin123"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri 'http://localhost:8081/auth/super/auth/login' -Method POST -Body $loginBody -ContentType 'application/json' -UseBasicParsing
$data = $response.Content | ConvertFrom-Json
$token = $data.jwt

Write-Host "Token obtained: $($token.Substring(0, 50))..."

# Test organization endpoint
$headers = @{ "Authorization" = "Bearer $token" }

Write-Host "Testing organization endpoint..."
try {
    $orgResponse = Invoke-WebRequest -Uri 'http://localhost:8082/organization/super/organizations' -Headers $headers -UseBasicParsing
    Write-Host "SUCCESS: $($orgResponse.StatusCode)"
    Write-Host "Response: $($orgResponse.Content)"
} catch {
    Write-Host "FAILED: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $errorContent = $reader.ReadToEnd()
        Write-Host "Error details: $errorContent"
    }
}
