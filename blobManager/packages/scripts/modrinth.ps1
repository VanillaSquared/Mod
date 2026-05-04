$ErrorActionPreference = "Stop"
$Version = "1.0.0"
$PackageName = "modrinth"

$errorApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\errorAPI.psm1") -Force -DisableNameChecking -PassThru
$configApiModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\configAPI.psm1") -Force -DisableNameChecking -PassThru
$versionModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\version.psm1") -Force -DisableNameChecking -PassThru
$helpModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\help.psm1") -Force -DisableNameChecking -PassThru
$pingModule = Import-Module (Join-Path $PSScriptRoot "..\util\api\commands\ping.psm1") -Force -DisableNameChecking -PassThru

$Cmd = @{
    NewMessageState    = $errorApiModule.ExportedCommands["New-MessageState"]
    AddErrorMessage    = $errorApiModule.ExportedCommands["Add-ErrorMessage"]
    ThrowIfErrors      = $errorApiModule.ExportedCommands["Throw-IfErrors"]
    WriteWarnings      = $errorApiModule.ExportedCommands["Write-Warnings"]
    GetJsonConfig      = $configApiModule.ExportedCommands["Get-JsonConfig"]
    GetConfigValue     = $configApiModule.ExportedCommands["Get-ConfigValue"]
    TestConfigVersion  = $configApiModule.ExportedCommands["Test-ConfigVersion"]
    CommandVersion     = $versionModule.ExportedCommands["Command-Version"]
    CommandHelp        = $helpModule.ExportedCommands["Command-Help"]
    CommandPing        = $pingModule.ExportedCommands["Command-Ping"]
}

foreach ($entry in $Cmd.GetEnumerator()) {
    if ($null -eq $entry.Value) {
        throw "Required command handle missing: $($entry.Key)"
    }
}

$configPath = Join-Path $PSScriptRoot "..\config\modrinth.json"
$config = & $Cmd.GetJsonConfig -Path $configPath -Fallback @{}
$networkConfig = & $Cmd.GetConfigValue -Config $config -Key "network" -DefaultValue @{}
$headers = & $Cmd.GetConfigValue -Config $networkConfig -Key "headers" -DefaultValue @{}
$baseUrl = & $Cmd.GetConfigValue -Config $networkConfig -Key "baseUrl" -DefaultValue "https://api.modrinth.com"
$testEndpoint = & $Cmd.GetConfigValue -Config $networkConfig -Key "testEndpoint" -DefaultValue "/"
$timeoutSeconds = & $Cmd.GetConfigValue -Config $networkConfig -Key "timeoutSeconds" -DefaultValue 30

$state = & $Cmd.NewMessageState
$versionStatus = & $Cmd.TestConfigVersion -PackageName $PackageName -ScriptVersion $Version -Config $config -State $state

$Command = if ($args.Count -gt 0) { $args[0] } else { $null }

switch ($Command) {
    "-p" {
        if ([string]::IsNullOrWhiteSpace([string]$baseUrl)) {
            & $Cmd.AddErrorMessage -State $state -Message "modrinth network.baseUrl is missing in blobManager\\packages\\config\\modrinth.json"
        }

        if ([string]::IsNullOrWhiteSpace([string]$testEndpoint)) {
            & $Cmd.AddErrorMessage -State $state -Message "modrinth network.testEndpoint is missing in blobManager\\packages\\config\\modrinth.json"
        }

        & $Cmd.ThrowIfErrors -State $state

        $baseUri = [System.Uri]$baseUrl
        $requestUri = [System.Uri]::new($baseUri, $testEndpoint).AbsoluteUri
        $result = & $Cmd.CommandPing -PackageName $PackageName -ApiName "Modrinth API" -Uri $requestUri -Headers $headers -TimeoutSeconds $timeoutSeconds
        & $Cmd.WriteWarnings -State $state

        if ($result.Success) {
            exit 0
        }

        exit 1
    }

    "-v" {
        & $Cmd.CommandVersion -PackageName $PackageName -Version $Version -ConfigVersion $versionStatus.ConfigVersion -WarningMessage $versionStatus.WarningMessage
        exit 0
    }

    "-?" {
        & $Cmd.WriteWarnings -State $state
        & $Cmd.CommandHelp -PackageName $PackageName -Commands @("modrinth -p", "modrinth -v", "modrinth -?")
        exit 0
    }

    default {
        & $Cmd.WriteWarnings -State $state
        Write-Host "Unknown modrinth command: $Command"
        Write-Host "Run: .\blob.ps1 modrinth -?"
        exit 1
    }
}
