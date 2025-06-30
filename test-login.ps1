$loginBody = @{
    username = "superadmin"
    password = "admin123"
} | ConvertTo-Json

Write-Host "Testing SuperAdmin login..."
Write-Host "Request body: $loginBody"

try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8081/auth/super/auth/login' -Method POST -Body $loginBody -ContentType 'application/json' -UseBasicParsing
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
    
    $data = $response.Content | ConvertFrom-Json
    Write-Host "Access Token: $($data.accessToken)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $errorContent = $reader.ReadToEnd()
        Write-Host "Error details: $errorContent"
    }
}
