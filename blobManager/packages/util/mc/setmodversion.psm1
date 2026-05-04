function Set-ModVersion {
    param(
        [string]$GradlePropertiesPath,
        [string]$FabricModJsonPath,
        [string]$GradleVersionMarker,
        [string]$Version,
        [string]$VersionArg,
        [string]$ReleaseType
    )

    switch ($ReleaseType) {
        "alpha" {
            $releaseTypePrefix = "-2."
            $releaseTypeGradleProperties = "2."
        }
        "beta" {
            $releaseTypePrefix = "-1."
            $releaseTypeGradleProperties = "1."
        }
        "release" {
            $releaseTypePrefix = "0."
            $releaseTypeGradleProperties = "0."
        }
        default {
            throw "Release type must be alpha, beta, or release."
        }
    }

    $gradleProperties = Get-Content $GradlePropertiesPath
    $fabricModJson = Get-Content $FabricModJsonPath

    $newVersion = "$releaseTypePrefix$Version-$VersionArg"
    $newVersionGradleProperties = "$releaseTypeGradleProperties$Version-$VersionArg"

    $content = $gradleProperties | ForEach-Object {
        if ($_ -match [regex]::Escape($GradleVersionMarker)) {
            "mod_version=$newVersionGradleProperties"
        }
        else {
            $_
        }
    }

    Set-Content $GradlePropertiesPath $content

    $content = $fabricModJson | ForEach-Object {
        if ($_ -match '^\s*"version"\s*:') {
            '    "version": "' + $newVersion + '",'
        }
        else {
            $_
        }
    }

    Set-Content $FabricModJsonPath $content
    return $newVersion
}

Export-ModuleMember -Function Set-ModVersion