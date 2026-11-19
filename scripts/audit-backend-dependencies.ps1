param()

$ErrorActionPreference = 'Stop'
$backendPath = Join-Path $PSScriptRoot '../backend'
$treePath = Join-Path $backendPath 'target/dependency-runtime-audit.txt'
$reportPath = Join-Path $backendPath 'target/dependency-audit-osv.json'

Push-Location $backendPath
try {
    $wrapper = if ($IsWindows -or $env:OS -eq 'Windows_NT') { '.\mvnw.cmd' } else { './mvnw' }
    & $wrapper dependency:tree '-Dscope=runtime' '-DoutputFile=target/dependency-runtime-audit.txt'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo obtener el árbol de dependencias.' }

    $coordinates = @(Get-Content -LiteralPath $treePath | ForEach-Object {
        if ($_ -match '\s([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):jar:(.+):(compile|runtime)$') {
            [pscustomobject]@{
                Name = $Matches[1] + ':' + $Matches[2]
                Version = ($Matches[3] -split ':')[-1]
            }
        }
    } | Sort-Object Name, Version -Unique)
    if ($coordinates.Count -eq 0) { throw 'El árbol no contiene dependencias para consultar.' }

    # Only public dependency coordinates and versions are sent, never application source or credentials.
    $queries = @($coordinates | ForEach-Object {
        @{ package = @{ ecosystem = 'Maven'; name = $_.Name }; version = $_.Version }
    })
    $body = @{ queries = $queries } | ConvertTo-Json -Depth 5 -Compress
    $result = Invoke-RestMethod -Uri 'https://api.osv.dev/v1/querybatch' -Method Post `
        -ContentType 'application/json' -Body $body -TimeoutSec 40
    if (@($result.results).Count -ne $coordinates.Count) { throw 'OSV devolvió una respuesta incompleta.' }

    $findings = @(for ($index = 0; $index -lt $coordinates.Count; $index++) {
        if ($result.results[$index].vulns) {
            [pscustomobject]@{
                Name = $coordinates[$index].Name
                Version = $coordinates[$index].Version
                Advisories = @($result.results[$index].vulns.id)
            }
        }
    })
    [pscustomobject]@{
        CheckedAt = [DateTimeOffset]::UtcNow.ToString('o')
        Scope = 'Maven compile/runtime JAR dependencies'
        Checked = $coordinates.Count
        Findings = $findings
    } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $reportPath -Encoding utf8

    Write-Output ("Dependencias consultadas: {0}; paquetes con alertas: {1}" -f $coordinates.Count, $findings.Count)
    Write-Output "Informe: $reportPath"
    if ($findings.Count -gt 0) { exit 1 }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 2
} finally {
    Pop-Location
}
