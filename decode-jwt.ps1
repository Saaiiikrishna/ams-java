$token = "eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlblR5cGUiOiJTVVBFUl9BRE1JTl9BQ0NFU1MiLCJhdXRob3JpdGllcyI6W3siYXV0aG9yaXR5IjoiUk9MRV9TVVBFUl9BRE1JTiJ9XSwic3ViIjoic3VwZXJhZG1pbiIsImlhdCI6MTc1MTE1Njg5MiwiZXhwIjoxNzUxMTY0MDkyfQ.UmshGqiRhBMFOgr8lgO3NXdz9kJLrhwXvT8YLpxSCXSoB_HgrSvTb2PVfDJcWzUJFLN5eRU5ZacenvbdfQq6jg"

# Split the JWT token
$parts = $token.Split('.')

# Decode the payload (second part)
$payload = $parts[1]

# Add padding if needed
while ($payload.Length % 4 -ne 0) {
    $payload += "="
}

# Decode from Base64
$decodedBytes = [System.Convert]::FromBase64String($payload)
$decodedText = [System.Text.Encoding]::UTF8.GetString($decodedBytes)

Write-Output "JWT Payload:"
Write-Output $decodedText

# Parse as JSON for better formatting
$jsonPayload = $decodedText | ConvertFrom-Json
Write-Output "`nParsed JWT Payload:"
Write-Output "Subject: $($jsonPayload.sub)"
Write-Output "Token Type: $($jsonPayload.tokenType)"
Write-Output "Issued At: $($jsonPayload.iat)"
Write-Output "Expires At: $($jsonPayload.exp)"
Write-Output "Authorities: $($jsonPayload.authorities | ConvertTo-Json)"
