Import-Module "$PSScriptRoot\..\api\imageAPI.psm1" -Force -DisableNameChecking
Import-Module "$PSScriptRoot\setname.psm1" -Force -DisableNameChecking

function Get-DefaultSkinPaths {
    param(
        [string]$DefaultSkinsRoot,
        [string[]]$SkinNames,
        [string]$Model
    )

    $paths = @()
    foreach ($skinName in $SkinNames) {
        $paths += (Join-Path $DefaultSkinsRoot "$Model/$skinName.png")
    }

    return $paths
}

function Restore-DefaultSkins {
    param(
        [string]$DefaultSkinsRoot,
        [string[]]$SkinNames,
        [string]$Model
    )

    foreach ($skin in (Get-DefaultSkinPaths -DefaultSkinsRoot $DefaultSkinsRoot -SkinNames $SkinNames -Model $Model)) {
        New-TransparentImage -Path $skin -Width 64 -Height 64
    }
}

function Apply-SkinProfile {
    param(
        [string]$ProfileName,
        [string]$Model,
        [string]$Username,
        [string]$SkinPacksRoot,
        [string]$DefaultSkinsRoot,
        [string[]]$SkinNames,
        [string]$BuildGradlePath,
        [string]$ModelMarker,
        [string]$UsernameMarker,
        [hashtable]$State
    )

    $profilePath = Join-Path $SkinPacksRoot $ProfileName
    $skinFile = Join-Path $profilePath "skin.png"

    if (!(Test-Path $skinFile -PathType Leaf)) {
        throw "Profile skin does not exist: $skinFile"
    }

    foreach ($skin in (Get-DefaultSkinPaths -DefaultSkinsRoot $DefaultSkinsRoot -SkinNames $SkinNames -Model $Model)) {
        Copy-Item -Force $skinFile $skin
    }

    Set-Model -BuildGradlePath $BuildGradlePath -ModelMarker $ModelMarker -Model $Model
    Set-Username -BuildGradlePath $BuildGradlePath -UsernameMarker $UsernameMarker -Username $Username
}

Export-ModuleMember -Function Get-DefaultSkinPaths, Restore-DefaultSkins, Apply-SkinProfile
