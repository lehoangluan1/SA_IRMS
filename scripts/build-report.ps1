param(
    [string]$EntryPoint = "irms_architecture_report.tex",
    [int]$Passes = 2,
    [switch]$SkipPdf
)

$ErrorActionPreference = "Stop"

function Normalize-ForMatch {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ""
    }

    $decomposed = $Value.Normalize([Text.NormalizationForm]::FormD)
    $builder = [Text.StringBuilder]::new()
    foreach ($ch in $decomposed.ToCharArray()) {
        $category = [Globalization.CharUnicodeInfo]::GetUnicodeCategory($ch)
        if ($category -ne [Globalization.UnicodeCategory]::NonSpacingMark) {
            [void]$builder.Append($ch)
        }
    }

    return ($builder.ToString().Normalize([Text.NormalizationForm]::FormC).ToLowerInvariant() -replace '[^a-z0-9]+', '')
}

function Find-SimilarFiles {
    param(
        [string]$Requested,
        [string[]]$SearchDirs
    )

    $needle = Normalize-ForMatch ([IO.Path]::GetFileNameWithoutExtension($Requested))
    if (-not $needle) {
        return @()
    }

    $candidates = foreach ($dir in $SearchDirs) {
        if (Test-Path -LiteralPath $dir) {
            Get-ChildItem -LiteralPath $dir -File -ErrorAction SilentlyContinue |
                Where-Object { $_.Extension -in @(".png", ".pdf", ".dot") }
        }
    }

    $scored = foreach ($file in $candidates) {
        $candidate = Normalize-ForMatch $file.BaseName
        if (-not $candidate) {
            continue
        }

        $prefix = 0
        $limit = [Math]::Min($needle.Length, $candidate.Length)
        while ($prefix -lt $limit -and $needle[$prefix] -eq $candidate[$prefix]) {
            $prefix++
        }

        $contains = if ($candidate.Contains($needle) -or $needle.Contains($candidate)) { 1000 } else { 0 }
        [pscustomobject]@{
            Score = $contains + $prefix
            Path = $file.FullName
        }
    }

    return @($scored | Sort-Object Score -Descending | Where-Object Score -gt 3 | Select-Object -First 8 | ForEach-Object { $_.Path })
}

function Resolve-GraphicReference {
    param(
        [string]$Request,
        [string[]]$SearchDirs,
        [string[]]$Extensions
    )

    $requestPath = $Request -replace '/', [IO.Path]::DirectorySeparatorChar
    $requestedExtension = [IO.Path]::GetExtension($requestPath)
    $extensionsToTry = if ($requestedExtension) { @($requestedExtension) } else { $Extensions }
    $baseRequest = if ($requestedExtension) {
        $requestPath.Substring(0, $requestPath.Length - $requestedExtension.Length)
    } else {
        $requestPath
    }

    $dirsToTry = if ([IO.Path]::IsPathRooted($baseRequest) -or $baseRequest.Contains([IO.Path]::DirectorySeparatorChar)) {
        @($repoRoot)
    } else {
        $SearchDirs
    }

    $tried = New-Object System.Collections.Generic.List[string]
    foreach ($dir in $dirsToTry) {
        foreach ($extension in $extensionsToTry) {
            $candidate = if ([IO.Path]::IsPathRooted($baseRequest)) {
                "$baseRequest$extension"
            } else {
                Join-Path $dir "$baseRequest$extension"
            }

            $absoluteCandidate = [IO.Path]::GetFullPath($candidate)
            [void]$tried.Add($absoluteCandidate)
            if (Test-Path -LiteralPath $absoluteCandidate -PathType Leaf) {
                return [pscustomobject]@{
                    Found = $true
                    Path = (Resolve-Path -LiteralPath $absoluteCandidate).Path
                    Tried = @($tried)
                }
            }
        }
    }

    return [pscustomobject]@{
        Found = $false
        Path = $null
        Tried = @($tried)
    }
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Push-Location $repoRoot
try {
    $renderScript = Join-Path $PSScriptRoot "render-diagrams.ps1"
    Write-Host "Rendering Graphviz diagrams to PNG..."
    & $renderScript -Format png

    $searchDirs = @(
        (Join-Path $repoRoot "assets/diagrams/rendered/modeling"),
        (Join-Path $repoRoot "assets/diagrams/rendered/architecture"),
        (Join-Path $repoRoot "assets/diagrams/rendered/design"),
        (Join-Path $repoRoot "assets/diagrams/rendered/activity"),
        (Join-Path $repoRoot "assets/images/branding"),
        (Join-Path $repoRoot "assets/images/misc"),
        $repoRoot
    )
    $sourceDirs = @(
        (Join-Path $repoRoot "assets/diagrams/source/modeling"),
        (Join-Path $repoRoot "assets/diagrams/source/architecture"),
        (Join-Path $repoRoot "assets/diagrams/source/design"),
        (Join-Path $repoRoot "assets/diagrams/source/activity")
    )
    $extensions = @(".png", ".pdf")

    $texFiles = @((Join-Path $repoRoot "main.tex")) + @(Get-ChildItem -LiteralPath (Join-Path $repoRoot "sections") -Filter "*.tex" | ForEach-Object { $_.FullName })
    $requests = [ordered]@{}
    foreach ($file in $texFiles) {
        $text = Get-Content -LiteralPath $file -Raw -Encoding UTF8

        foreach ($match in [regex]::Matches($text, '\\activitydiagramfigure\{([^{}#]+)\}')) {
            $requests[$match.Groups[1].Value] = $true
        }

        foreach ($match in [regex]::Matches($text, '\\includegraphics(?:\[[^\]]*\])?\{([^{}#]+)\}')) {
            $value = $match.Groups[1].Value
            $extension = [IO.Path]::GetExtension($value)
            if ($extension -in @(".png", ".pdf")) {
                $value = $value.Substring(0, $value.Length - $extension.Length)
            }
            $requests[$value] = $true
        }

        foreach ($match in [regex]::Matches($text, '\\IfFileExists\{([^{}#]+\.(?:png|pdf))\}')) {
            $value = $match.Groups[1].Value
            $extension = [IO.Path]::GetExtension($value)
            if ($extension -in @(".png", ".pdf")) {
                $value = $value.Substring(0, $value.Length - $extension.Length)
            }
            $requests[$value] = $true
        }
    }

    $missing = @()
    foreach ($request in $requests.Keys) {
        $resolved = Resolve-GraphicReference -Request $request -SearchDirs $searchDirs -Extensions $extensions
        if ($resolved.Found) {
            continue
        }

        $sourceResolved = Resolve-GraphicReference -Request $request -SearchDirs $sourceDirs -Extensions @(".dot")
        $near = Find-SimilarFiles -Requested $request -SearchDirs ($searchDirs + $sourceDirs)

        Write-Warning "Missing graphic request: $request"
        Write-Warning "  searched dirs: $($searchDirs -join '; ')"
        Write-Warning "  extensions tried: $($extensions -join ', ')"
        Write-Warning "  absolute paths tried: $($resolved.Tried -join '; ')"
        if ($sourceResolved.Found) {
            Write-Warning "  matching DOT source exists: $($sourceResolved.Path)"
        }
        if ($near.Count -gt 0) {
            Write-Warning "  similar files: $($near -join '; ')"
        } else {
            Write-Warning "  similar files: none"
        }

        $missing += $request
    }

    if ($missing.Count -gt 0) {
        throw "Missing $($missing.Count) graphic reference(s). See warnings above."
    }

    if (-not $SkipPdf) {
        $xelatex = Get-Command xelatex -ErrorAction SilentlyContinue
        if (-not $xelatex) {
            $xelatexCandidates = @(
                "C:\Program Files\MiKTeX\miktex\bin\x64\xelatex.exe",
                "C:\Program Files\MiKTeX 2.9\miktex\bin\x64\xelatex.exe",
                "C:\texlive\2026\bin\windows\xelatex.exe",
                "C:\texlive\2025\bin\windows\xelatex.exe",
                "C:\texlive\2024\bin\windows\xelatex.exe"
            )

            $resolvedXeLaTeX = $xelatexCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
            if ($resolvedXeLaTeX) {
                $xelatex = [pscustomobject]@{ Source = $resolvedXeLaTeX }
            } else {
                throw "The 'xelatex' command was not found. Install MiKTeX/TeX Live with XeLaTeX."
            }
        }

        for ($i = 1; $i -le $Passes; $i++) {
            Write-Host "Running XeLaTeX pass $i/$Passes..."
            & $xelatex.Source -interaction=nonstopmode -halt-on-error $EntryPoint
            if ($LASTEXITCODE -ne 0) {
                throw "XeLaTeX failed on pass $i with exit code $LASTEXITCODE."
            }
        }
    }
} finally {
    Pop-Location
}
