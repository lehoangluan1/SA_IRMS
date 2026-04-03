$ErrorActionPreference = "Stop"

$dot = Get-Command dot -ErrorAction SilentlyContinue
if (-not $dot) {
    $graphvizCandidates = @(
        "C:\Program Files\Graphviz\bin\dot.exe",
        "C:\Program Files (x86)\Graphviz\bin\dot.exe"
    )

    $resolvedDot = $graphvizCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($resolvedDot) {
        $dot = [pscustomobject]@{ Source = $resolvedDot }
    } else {
        throw "The 'dot' command was not found. Install Graphviz and either add it to PATH or place it in the default Graphviz folder."
    }
}

$neato = Get-Command neato -ErrorAction SilentlyContinue
if (-not $neato) {
    $graphvizCandidates = @(
        "C:\Program Files\Graphviz\bin\neato.exe",
        "C:\Program Files (x86)\Graphviz\bin\neato.exe"
    )

    $resolvedNeato = $graphvizCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($resolvedNeato) {
        $neato = [pscustomobject]@{ Source = $resolvedNeato }
    }
}

$sourceRoot = Join-Path $PSScriptRoot "..\\assets\\diagrams\\source"
$renderRoot = Join-Path $PSScriptRoot "..\\assets\\diagrams\\rendered"

Get-ChildItem -Path $sourceRoot -Recurse -Filter *.dot | ForEach-Object {
    $relativeDir = $_.DirectoryName.Substring((Resolve-Path $sourceRoot).Path.Length).TrimStart('\')
    $targetDir = if ($relativeDir) { Join-Path $renderRoot $relativeDir } else { $renderRoot }
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

    $targetFile = Join-Path $targetDir ($_.BaseName + ".png")
    $sourceText = Get-Content -LiteralPath $_.FullName -Raw
    $usesNeatoLayout = $sourceText -match 'layout\s*=\s*neato'

    if ($usesNeatoLayout) {
        if (-not $neato) {
            throw "The 'neato' command was not found. Install Graphviz with neato support to render fixed-layout diagrams."
        }

        & $neato.Source -n2 -Tpng $_.FullName -o $targetFile
    } else {
        & $dot.Source -Tpng $_.FullName -o $targetFile
    }

    Write-Host "Rendered $($_.Name) -> $targetFile"
}
