$scriptPath = Join-Path $PSScriptRoot "blobManager/manager.ps1"
& $scriptPath @args

if ($LASTEXITCODE -ne $null) {
    exit $LASTEXITCODE
}
