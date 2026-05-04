function Set-BuildGradleProgramArg {
    param(
        [string]$BuildGradlePath,
        [string]$Marker,
        [string]$Value
    )

    $buildGradle = Get-Content $BuildGradlePath
    $newLine = "            programArg(`"$Value`") // $Marker"
    $pattern = '^\s*programArg\(".*"\)\s*//\s*' + [regex]::Escape($Marker) + '\s*$'

    $content = $buildGradle | ForEach-Object {
        if ($_ -match $pattern) {
            $newLine
        }
        else {
            $_
        }
    }

    Set-Content $BuildGradlePath $content
}

function Get-BuildGradleProgramArg {
    param(
        [string]$BuildGradlePath,
        [string]$Marker
    )

    if (!(Test-Path $BuildGradlePath -PathType Leaf)) {
        return $null
    }

    $pattern = '^\s*programArg\("(?<Value>[^"]*)"\)\s*//\s*' + [regex]::Escape($Marker) + '\s*$'
    foreach ($line in (Get-Content $BuildGradlePath)) {
        if ($line -match $pattern) {
            return $Matches["Value"]
        }
    }

    return $null
}

function Get-BuildGradleProgramArgs {
    param(
        [string]$BuildGradlePath,
        [string]$UsernameMarker,
        [string]$ModelMarker
    )

    return @{
        Username = Get-BuildGradleProgramArg -BuildGradlePath $BuildGradlePath -Marker $UsernameMarker
        Model = Get-BuildGradleProgramArg -BuildGradlePath $BuildGradlePath -Marker $ModelMarker
    }
}

Export-ModuleMember -Function Set-BuildGradleProgramArg, Get-BuildGradleProgramArg, Get-BuildGradleProgramArgs
