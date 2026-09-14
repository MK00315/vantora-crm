[CmdletBinding()]
param(
    [switch]$Force
)

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$templatePath = Join-Path $repositoryRoot '.env.example'
$destinationPath = Join-Path $repositoryRoot '.env'

if ((Test-Path -LiteralPath $destinationPath) -and -not $Force) {
    throw '.env already exists. Use -Force only when you intentionally want to replace it.'
}

$randomBytes = [byte[]]::new(48)
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $generator.GetBytes($randomBytes)
}
finally {
    $generator.Dispose()
}

$jwtSecret = [Convert]::ToBase64String($randomBytes)
$contents = Get-Content -LiteralPath $templatePath -Raw
$contents = [regex]::Replace($contents, '(?m)^JWT_SECRET=.*$', "JWT_SECRET=$jwtSecret")
[System.IO.File]::WriteAllText($destinationPath, $contents, [System.Text.UTF8Encoding]::new($false))

Write-Host "Created $destinationPath with a unique signing key."
