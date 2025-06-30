# Get fresh JWT token
$response = Invoke-WebRequest -Uri "http://localhost:8081/auth/super/auth/login" -Method POST -ContentType "application/json" -InFile "test-login.json" -UseBasicParsing
$json = $response.Content | ConvertFrom-Json
Write-Output "Fresh JWT Token:"
Write-Output $json.jwt
