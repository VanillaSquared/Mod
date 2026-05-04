Import-Module "$PSScriptRoot\gradle.psm1" -Force -DisableNameChecking

function Set-Username {
    param(
        [string]$BuildGradlePath,
        [string]$UsernameMarker,
        [string]$Username
    )

    Set-BuildGradleProgramArg -BuildGradlePath $BuildGradlePath -Marker $UsernameMarker -Value $Username
}

function Set-Model {
    param(
        [string]$BuildGradlePath,
        [string]$ModelMarker,
        [string]$Model
    )

    Set-BuildGradleProgramArg -BuildGradlePath $BuildGradlePath -Marker $ModelMarker -Value $Model
}

Export-ModuleMember -Function Set-Username, Set-Model
