param([string] $BuildDir, [string] $Java, [string] $StartTemplate)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$script:Checks = 0
function Check($Condition, [string] $Message) {
    if (!$Condition) { throw "FAIL: $Message" }; $script:Checks++
}
function Fixture {
    $root = Join-Path ([IO.Path]::GetTempPath()) ('prominence-hook-test-' + [guid]::NewGuid().ToString())
    foreach ($dir in @('mods', 'maintenance', '.prominence-server-update')) { [void](New-Item -ItemType Directory -Path (Join-Path $root $dir) -Force) }
    Copy-Item -LiteralPath (Join-Path $BuildDir 'prominence-korean-compat-server-1.0.0.jar') -Destination (Join-Path $root '.prominence-server-update/bootstrap.jar')
    Copy-Item -LiteralPath (Join-Path $BuildDir 'release/server-update.properties') -Destination (Join-Path $root '.prominence-server-update/bootstrap.properties')
    Copy-Item -LiteralPath (Join-Path $BuildDir 'ServerUpdate.ps1') -Destination (Join-Path $root 'maintenance/ServerUpdate.ps1')
    Copy-Item -LiteralPath (Join-Path $repo 'base/server-compat-before-updater.jar') -Destination (Join-Path $root 'mods/prominence-korean-compat-1.0.0.jar')
    Copy-Item -LiteralPath $StartTemplate -Destination (Join-Path $root 'start.ps1')
    Set-Content -LiteralPath (Join-Path $root '.prominence-server-update/config.properties') -Value 'enabled=false' -Encoding ascii
    Set-Content -LiteralPath (Join-Path $root 'server.properties') -Value 'server-port=65501' -Encoding ascii
    return $root
}

# Parse the full real start script without executing it, install twice and compare.
$root = Fixture
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repo 'scripts/Install-ServerUpdater.ps1') -ServerRoot $root
Check ($LASTEXITCODE -eq 0) 'installer success'
$start = Join-Path $root 'start.ps1'; $first = (Get-FileHash -LiteralPath $start).Hash
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repo 'scripts/Install-ServerUpdater.ps1') -ServerRoot $root
Check ($LASTEXITCODE -eq 0 -and (Get-FileHash -LiteralPath $start).Hash -eq $first) 'idempotent installer'
$text = [IO.File]::ReadAllText($start)
Check ([regex]::Matches($text, [regex]::Escape('Invoke-ProminenceServerUpdate $Java')).Count -eq 1) 'exactly one preflight per JVM'

. (Join-Path $root 'maintenance/ServerUpdate.ps1')
Initialize-ProminenceServerUpdate $root
Invoke-ProminenceServerUpdate $Java
Check (Test-Path -LiteralPath (Join-Path $root 'mods/prominence-korean-compat-server-1.0.0.jar')) 'offline initial rename'
Check (!(Test-Path -LiteralPath (Join-Path $root 'mods/prominence-korean-compat-1.0.0.jar'))) 'old name removed'
Check ($env:PROMINENCE_SERVER_UPDATE_TOKEN -match '^[a-f0-9-]{36}$') 'random health token passed to child JVM'
& $Java '-cp' ((Join-Path $BuildDir 'tests') + ';' + (Join-Path $BuildDir 'prominence-korean-compat-server-1.0.0.jar')) HealthProbe $root
Check ($LASTEXITCODE -eq 0) 'fixture health receipt'
Invoke-ProminenceServerUpdate $Java
Check (!(Test-Path -LiteralPath (Join-Path $root '.prominence-server-update/pending.properties'))) 'healthy finalized through actual PowerShell hook'
Check ([string]::IsNullOrEmpty($env:PROMINENCE_SERVER_UPDATE_TOKEN)) 'stale token cleared'
Check (@(Get-ChildItem -LiteralPath (Join-Path $root '.prominence-server-update') -Filter 'runner-*.jar').Count -eq 0) 'runner files cleaned'

# Another launcher cannot acquire the whole-lifetime lock.
$lockTest = Join-Path $root 'lock-test.ps1'
@'
param($Root)
. (Join-Path $Root 'maintenance/ServerUpdate.ps1')
try { Initialize-ProminenceServerUpdate $Root; exit 1 } catch { exit 0 }
'@ | Set-Content -LiteralPath $lockTest -Encoding utf8
& powershell -NoProfile -ExecutionPolicy Bypass -File $lockTest $root
Check ($LASTEXITCODE -eq 0) 'concurrent launcher refused'

# Refuse to touch mods when a process is listening on the configured port.
$listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
$listener.Start()
try {
    Set-Content -LiteralPath (Join-Path $root 'server.properties') -Value ('server-port=' + $listener.LocalEndpoint.Port) -Encoding ascii
    $rejected = $false
    try { Invoke-ProminenceServerUpdate $Java } catch { $rejected = $true }
    Check $rejected 'live port guard'
} finally { $listener.Stop(); $script:ProminenceServerLifetimeLock.Dispose() }

# A genuine Windows delete-sharing lock causes a safety stop, followed by exact recovery after release.
$lockedRoot = Fixture
. (Join-Path $lockedRoot 'maintenance/ServerUpdate.ps1')
Initialize-ProminenceServerUpdate $lockedRoot
$old = Join-Path $lockedRoot 'mods/prominence-korean-compat-1.0.0.jar'
$oldHash = (Get-FileHash -LiteralPath $old).Hash
$hold = [IO.File]::Open($old, 'Open', 'Read', 'Read')
try {
    $rejected = $false
    try { Invoke-ProminenceServerUpdate $Java } catch { $rejected = $true }
    Check $rejected 'locked old JAR stops launch instead of duplicate mods'
    Check (Test-Path -LiteralPath (Join-Path $lockedRoot '.prominence-server-update/pending.properties')) 'locked migration retains recovery journal'
} finally { $hold.Dispose() }
Invoke-ProminenceServerUpdate $Java
Check ((Get-FileHash -LiteralPath $old).Hash -eq $oldHash) 'recovery restores exact old bytes'
Check (!(Test-Path -LiteralPath (Join-Path $lockedRoot 'mods/prominence-korean-compat-server-1.0.0.jar'))) 'recovery removes duplicate new filename'
Invoke-ProminenceServerUpdate $Java
Check ((Get-FileHash -LiteralPath $old).Hash -eq $oldHash) 'blocked initial build does not loop'
$script:ProminenceServerLifetimeLock.Dispose()
Write-Host "PASS: $script:Checks Windows PowerShell/installer/real sharing-lock checks; no Minecraft/server/world launched."
