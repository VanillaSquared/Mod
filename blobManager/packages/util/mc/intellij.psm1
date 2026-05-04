Import-Module "$PSScriptRoot\..\api\errorAPI.psm1" -Force -DisableNameChecking
Import-Module "$PSScriptRoot\gradle.psm1" -Force -DisableNameChecking

function Set-IntelliJRunConfigurationProgramParameters {
    param(
        [string]$RunConfigurationPath,
        [string]$ProgramParameters
    )

    if ([string]::IsNullOrWhiteSpace($RunConfigurationPath) -or !(Test-Path $RunConfigurationPath -PathType Leaf)) {
        return @{
            Path = $RunConfigurationPath
            Found = $false
            Updated = $false
        }
    }

    $pattern = '^(?<Prefix>\s*<option\s+name="PROGRAM_PARAMETERS"\s+value=")(?<Value>[^"]*)(?<Suffix>".*)$'
    $escapedProgramParameters = [System.Security.SecurityElement]::Escape([string]$ProgramParameters)
    $lines = @(Get-Content $RunConfigurationPath)
    $updated = $false

    $newLines = foreach ($line in $lines) {
        if (-not $updated -and $line -match $pattern) {
            $updated = $true
            $Matches["Prefix"] + $escapedProgramParameters + $Matches["Suffix"]
        }
        else {
            $line
        }
    }

    if ($updated) {
        Set-Content $RunConfigurationPath $newLines
    }

    return @{
        Path = $RunConfigurationPath
        Found = $true
        Updated = $updated
    }
}

function Sync-IntelliJRunConfigurations {
    param(
        [hashtable]$RunConfigurations,
        [string]$Username,
        [string]$Model
    )

    $results = @()
    $clientProgramParameters = "--username `"$Username`" --model `"$Model`""

    foreach ($entryName in @("client", "server", "dataGeneration")) {
        $entry = $null
        if ($null -ne $RunConfigurations -and $RunConfigurations.ContainsKey($entryName)) {
            $entry = $RunConfigurations[$entryName]
        }

        $path = $null
        $programParameters = $null

        if ($entry -is [hashtable]) {
            $path = $entry["path"]
            if ($entry.ContainsKey("programParameters")) {
                $programParameters = $entry["programParameters"]
            }
        }

        if ($entryName -eq "client") {
            $programParameters = $clientProgramParameters
        }
        elseif ($null -eq $programParameters) {
            $programParameters = ""
        }

        $result = Set-IntelliJRunConfigurationProgramParameters -RunConfigurationPath $path -ProgramParameters $programParameters
        $results += [pscustomobject]@{
            Name = $entryName
            Path = $result.Path
            Found = $result.Found
            Updated = $result.Updated
        }
    }

    return ,$results
}

function Sync-IntelliJState {
    param(
        [hashtable]$GlobalConfig,
        [hashtable]$RunConfigurations,
        [string]$BuildGradlePath,
        [string]$UsernameMarker,
        [string]$ModelMarker,
        [string]$DefaultUsername,
        [string]$DefaultModel,
        [hashtable]$State
    )

    $features = $null
    if ($null -ne $GlobalConfig -and $GlobalConfig.ContainsKey("features")) {
        $features = $GlobalConfig["features"]
    }

    $intelliJEnabled = $false
    if ($features -is [hashtable] -and $features.ContainsKey("intelliJ")) {
        $intelliJEnabled = [bool]$features["intelliJ"]
    }
    elseif ($features -is [psobject] -and $null -ne $features.PSObject.Properties["intelliJ"]) {
        $intelliJEnabled = [bool]$features.PSObject.Properties["intelliJ"].Value
    }

    if (-not $intelliJEnabled) {
        return
    }

    $debugEnabled = $false
    if ($features -is [hashtable] -and $features.ContainsKey("debug")) {
        $debugEnabled = [bool]$features["debug"]
    }
    elseif ($features -is [psobject] -and $null -ne $features.PSObject.Properties["debug"]) {
        $debugEnabled = [bool]$features.PSObject.Properties["debug"].Value
    }

    $buildGradleArgs = Get-BuildGradleProgramArgs -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -ModelMarker $ModelMarker
    $username = $buildGradleArgs["Username"]
    $model = $buildGradleArgs["Model"]

    if ([string]::IsNullOrWhiteSpace($username)) {
        $username = $DefaultUsername
    }

    if ([string]::IsNullOrWhiteSpace($model)) {
        $model = $DefaultModel
    }

    $results = Sync-IntelliJRunConfigurations -RunConfigurations $RunConfigurations -Username $username -Model $model

    foreach ($result in $results) {
        if ($null -eq $result) {
            continue
        }

        if (-not $result.Found -and $null -ne $State) {
            Add-WarningMessage -State $State -Message "IntelliJ run configuration not found: $($result.Path)"
        }
        elseif (-not $result.Updated -and $null -ne $State) {
            Add-WarningMessage -State $State -Message "PROGRAM_PARAMETERS was not found in IntelliJ run configuration: $($result.Path)"
        }
    }

    if ($debugEnabled) {
        return $results
    }
}

Export-ModuleMember -Function Set-IntelliJRunConfigurationProgramParameters, Sync-IntelliJRunConfigurations, Sync-IntelliJState
