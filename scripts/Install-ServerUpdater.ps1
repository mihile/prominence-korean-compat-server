param([string] $ServerRoot = $PSScriptRoot)
$ErrorActionPreference = 'Stop'
$ServerRoot = (Resolve-Path -LiteralPath $ServerRoot).Path
$start = Join-Path $ServerRoot 'start.ps1'
$hook = Join-Path $ServerRoot 'maintenance/ServerUpdate.ps1'
$raw = [IO.File]::ReadAllText($start)
$marker = '# Prominence dedicated-server signed updater (before every Minecraft JVM).'
if ($raw.Contains($marker)) { Write-Host 'Server updater hook already installed.'; exit 0 }
if (!(Test-Path -LiteralPath $hook)) { throw 'Extract the full bootstrap ZIP into the server folder first.' }
$portLine = @(Get-Content -LiteralPath (Join-Path $ServerRoot 'server.properties') | Where-Object { $_ -match '^server-port=\d+$' })
if ($portLine.Count -ne 1) { throw 'Cannot determine server port.' }
$port = [int]$portLine[0].Split('=')[1]
if (@([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object { $_.Port -eq $port }).Count -gt 0) {
    throw 'Stop Minecraft and close the old automatic-restart launcher first.'
}
$anchor = 'while ($true)'
if ([regex]::Matches($raw, [regex]::Escape($anchor)).Count -ne 1) { throw 'Unknown start.ps1 loop; manual integration required.' }
$launch = '    RunJavaCommand "${AdditionalArgs} ${ServerRunCommand}"'
if ([regex]::Matches($raw, [regex]::Escape($launch)).Count -ne 1) { throw 'Unknown Java launch; manual integration required.' }
$init = @'
# Prominence dedicated-server signed updater (before every Minecraft JVM).
try {
    . (Join-Path $BaseDir 'maintenance/ServerUpdate.ps1')
    Initialize-ProminenceServerUpdate $BaseDir
} catch {
    Write-Host "Server updater initialization stopped: $_" -ForegroundColor Red
    exit 20
}

'@
$preflight = @'
    try { Invoke-ProminenceServerUpdate $Java } catch {
        Write-Host "Server updater stopped launch: $_" -ForegroundColor Red
        exit 20
    }
'@
$updated = $raw.Replace($anchor, $init + "`r`n" + $anchor).Replace($launch, $preflight + "`r`n" + $launch)
$errors = $null; $tokens = $null
[void][Management.Automation.Language.Parser]::ParseInput($updated, [ref]$tokens, [ref]$errors)
if ($errors.Count -gt 0) { throw "Generated start.ps1 has syntax errors: $errors" }
$backup = Join-Path $ServerRoot ('maintenance-backups/server-updater-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $backup -ErrorAction Stop | Out-Null
Copy-Item -LiteralPath $start -Destination (Join-Path $backup 'start.ps1')
[IO.File]::WriteAllText($start, $updated, (New-Object Text.UTF8Encoding($true)))
Write-Host "Installed start hook. Backup: $backup. Start the server using start.bat."
