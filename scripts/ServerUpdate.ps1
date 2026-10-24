# Dot-source once, then call Invoke-ProminenceServerUpdate immediately before each Minecraft JVM.
# Requires Windows PowerShell 5.1+ and Java 17+. No Minecraft or Java flags are changed.

function Assert-ProminencePlainPath([string] $Path) {
    $item = Get-Item -LiteralPath $Path -Force -ErrorAction Stop
    if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw "Linked updater/mod path refused: $Path"
    }
}

function Initialize-ProminenceServerUpdate([string] $ServerRoot) {
    $script:ProminenceUpdateRoot = [IO.Path]::GetFullPath($ServerRoot)
    $script:ProminenceUpdateState = Join-Path $script:ProminenceUpdateRoot '.prominence-server-update'
    Assert-ProminencePlainPath $script:ProminenceUpdateRoot
    Assert-ProminencePlainPath (Join-Path $script:ProminenceUpdateRoot 'mods')
    Assert-ProminencePlainPath $script:ProminenceUpdateState
    $lockPath = Join-Path $script:ProminenceUpdateState 'server-launch.lock'
    if (Test-Path -LiteralPath $lockPath) { Assert-ProminencePlainPath $lockPath }
    # Held by this launcher process until exit, including the five-second restart delay.
    $script:ProminenceServerLifetimeLock = [IO.File]::Open($lockPath, 'OpenOrCreate', 'ReadWrite', 'None')
}

function Invoke-ProminenceServerUpdate([string] $Java) {
    Assert-ProminencePlainPath $script:ProminenceUpdateState
    Assert-ProminencePlainPath (Join-Path $script:ProminenceUpdateRoot 'mods')
    $propertiesPath = Join-Path $script:ProminenceUpdateRoot 'server.properties'
    $portLine = @(Get-Content -LiteralPath $propertiesPath -ErrorAction Stop | Where-Object { $_ -match '^server-port=\d+$' })
    if ($portLine.Count -ne 1) { throw 'Cannot determine the server port; refusing to update a possibly running server.' }
    $port = [int]($portLine[0].Split('=')[1])
    if (@([Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners() | Where-Object { $_.Port -eq $port }).Count -gt 0) {
        throw "Port $port is already listening. Stop the previous server and launcher before starting this launcher."
    }
    $bootstrap = Join-Path $script:ProminenceUpdateState 'bootstrap.jar'
    Assert-ProminencePlainPath $bootstrap
    $expectedBootstrap = '__BOOTSTRAP_SHA256__'
    if ((Get-FileHash -LiteralPath $bootstrap -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedBootstrap) {
        throw 'Rescue/bootstrap JAR hash mismatch; reinstall the reviewed updater bundle.'
    }
    $installed = Join-Path $script:ProminenceUpdateRoot 'mods/prominence-korean-compat-server-1.0.0.jar'
    $source = $bootstrap
    if (Test-Path -LiteralPath $installed) { Assert-ProminencePlainPath $installed; $source = $installed }
    $runner = Join-Path $script:ProminenceUpdateState ('runner-' + [guid]::NewGuid().ToString() + '.jar')
    try {
        Copy-Item -LiteralPath $source -Destination $runner -ErrorAction Stop
        & $Java '-Xms16m' '-Xmx96m' '-cp' $runner 'local.prominence.serverupdate.UpdateEngine' $script:ProminenceUpdateRoot | ForEach-Object { Write-Host $_ }
        $result = $LASTEXITCODE
        # A damaged installed JAR may not load its main class. The pinned first bundle can recover its journal.
        # Exit 20 means a deliberate safety stop, so never bypass it by trying a different updater.
        if ($result -ne 0 -and $result -ne 20 -and $source -ne $bootstrap) {
            Write-Host '[Server patch] Installed updater could not run; using verified rescue updater.'
            Copy-Item -LiteralPath $bootstrap -Destination $runner -Force -ErrorAction Stop
            & $Java '-Xms16m' '-Xmx96m' '-cp' $runner 'local.prominence.serverupdate.UpdateEngine' $script:ProminenceUpdateRoot | ForEach-Object { Write-Host $_ }
            $result = $LASTEXITCODE
        }
        if ($result -ne 0) { throw "Updater safety stop (exit $result). See .prominence-server-update/updater.log and pending.properties." }
    } finally {
        Remove-Item -LiteralPath $runner -Force -ErrorAction SilentlyContinue
    }
    $env:PROMINENCE_SERVER_UPDATE_TOKEN = ''
    $pending = Join-Path $script:ProminenceUpdateState 'pending.properties'
    if (Test-Path -LiteralPath $pending) {
        Assert-ProminencePlainPath $pending
        $data = Get-Content -Raw -LiteralPath $pending | ConvertFrom-StringData
        $env:PROMINENCE_SERVER_UPDATE_TOKEN = $data['token']
    }
}
