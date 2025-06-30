$token = "eyJhbGciOiJIUzUxMiJ9.eyJ0b2tlblR5cGUiOiJTVVBFUl9BRE1JTl9BQ0NFU1MiLCJhdXRob3JpdGllcyI6W3siYXV0aG9yaXR5IjoiUk9MRV9TVVBFUl9BRE1JTiJ9XSwic3ViIjoic3VwZXJhZG1pbiIsImlhdCI6MTc1MTE1Njg5MiwiZXhwIjoxNzUxMTY0MDkyfQ.UmshGqiRhBMFOgr8lgO3NXdz9kJLrhwXvT8YLpxSCXSoB_HgrSvTb2PVfDJcWzUJFLN5eRU5ZacenvbdfQq6jg"

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

Write-Output "Testing GET /auth/super/organizations..."
$response = Invoke-WebRequest -Uri "http://localhost:8081/auth/super/organizations" -Method GET -Headers $headers -UseBasicParsing

Write-Output "Response Status: $($response.StatusCode)"
Write-Output "Response Content:"
Write-Output $response.Content
