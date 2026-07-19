[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('deploy', 'rollback', 'status', 'cleanup')]
    [string]$Action,

    [string]$Artifact = 'target/reserva-lab.jar',
    [string]$Version = 'dev',
    [string]$RuntimeDirectory = '.runtime',
    [int]$BluePort = 8081,
    [int]$GreenPort = 8082
)

$ErrorActionPreference = 'Stop'
$runtimePath = [System.IO.Path]::GetFullPath((Join-Path (Get-Location) $RuntimeDirectory))

function Get-StateValue {
    param([string]$Name, [string]$Default = 'none')
    $path = Join-Path $runtimePath $Name
    if (Test-Path -LiteralPath $path) {
        return (Get-Content -LiteralPath $path -Raw).Trim()
    }
    return $Default
}

function Set-StateValue {
    param([string]$Name, [string]$Value)
    Set-Content -LiteralPath (Join-Path $runtimePath $Name) -Value $Value -Encoding utf8NoBOM
}

function Get-SlotPort {
    param([ValidateSet('blue', 'green')][string]$Color)
    if ($Color -eq 'blue') { return $BluePort }
    return $GreenPort
}

function Get-SlotUrl {
    param([ValidateSet('blue', 'green')][string]$Color)
    return "http://localhost:$(Get-SlotPort $Color)"
}

function Get-SlotProcess {
    param([ValidateSet('blue', 'green')][string]$Color)
    $pidFile = Join-Path $runtimePath "$Color/app.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { return $null }
    $processId = (Get-Content -LiteralPath $pidFile -Raw).Trim()
    if (-not $processId) { return $null }
    return Get-Process -Id ([int]$processId) -ErrorAction SilentlyContinue
}

function Stop-ProcessTree {
    param([int]$ProcessId)
    if ($IsWindows) {
        $children = Get-CimInstance Win32_Process -Filter "ParentProcessId=$ProcessId" -ErrorAction SilentlyContinue
        foreach ($child in $children) {
            Stop-ProcessTree ([int]$child.ProcessId)
        }
    }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

function Stop-Slot {
    param([ValidateSet('blue', 'green')][string]$Color)
    $process = Get-SlotProcess $Color
    if ($null -ne $process) {
        Stop-ProcessTree $process.Id
        $process.WaitForExit(5000) | Out-Null
    }
    $pidFile = Join-Path $runtimePath "$Color/app.pid"
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

function Wait-Healthy {
    param([string]$Url)
    for ($attempt = 1; $attempt -le 40; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri "$Url/api/health" -TimeoutSec 2
            if ($health.status -eq 'UP') { return $health }
        } catch {
            Start-Sleep -Milliseconds 250
        }
    }
    throw "El slot no quedo saludable en $Url"
}

function Add-History {
    param([string]$Event)
    $timestamp = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
    Add-Content -LiteralPath (Join-Path $runtimePath 'deployment-history.log') -Value "$timestamp $Event" -Encoding utf8NoBOM
}

New-Item -ItemType Directory -Path $runtimePath -Force | Out-Null

switch ($Action) {
    'deploy' {
        $artifactPath = (Resolve-Path -LiteralPath $Artifact).Path
        $active = Get-StateValue 'active-color.txt'
        $candidate = if ($active -eq 'blue') { 'green' } else { 'blue' }
        $candidateDirectory = Join-Path $runtimePath $candidate
        New-Item -ItemType Directory -Path $candidateDirectory -Force | Out-Null
        Stop-Slot $candidate

        $candidateJar = Join-Path $candidateDirectory 'reserva-lab.jar'
        Copy-Item -LiteralPath $artifactPath -Destination $candidateJar -Force
        $port = Get-SlotPort $candidate
        $stdout = Join-Path $candidateDirectory 'application.log'
        $stderr = Join-Path $candidateDirectory 'application-error.log'
        $arguments = "-jar `"$candidateJar`" --port=$port --environment=test --color=$candidate --version=$Version"
        $startParameters = @{
            FilePath = 'java'
            ArgumentList = $arguments
            RedirectStandardOutput = $stdout
            RedirectStandardError = $stderr
            PassThru = $true
        }
        if ($IsWindows) { $startParameters.WindowStyle = 'Hidden' }
        $process = Start-Process @startParameters
        Set-Content -LiteralPath (Join-Path $candidateDirectory 'app.pid') -Value $process.Id -Encoding ascii

        try {
            $health = Wait-Healthy (Get-SlotUrl $candidate)
        } catch {
            Stop-Slot $candidate
            Add-History "DEPLOY_FAILED candidate=$candidate version=$Version"
            throw
        }

        Set-StateValue 'previous-color.txt' $active
        Set-StateValue 'active-color.txt' $candidate
        Set-StateValue 'current-url.txt' (Get-SlotUrl $candidate)
        Set-StateValue 'active-version.txt' $Version
        Add-History "DEPLOY_SUCCESS active=$candidate previous=$active version=$Version port=$port"
        Write-Output "DEPLOY_SUCCESS active=$candidate previous=$active version=$Version port=$port status=$($health.status)"
    }

    'rollback' {
        $active = Get-StateValue 'active-color.txt'
        $previous = Get-StateValue 'previous-color.txt'
        if ($active -notin @('blue', 'green') -or $previous -notin @('blue', 'green')) {
            throw 'No existe un slot anterior valido para realizar rollback.'
        }

        $health = Wait-Healthy (Get-SlotUrl $previous)
        Set-StateValue 'active-color.txt' $previous
        Set-StateValue 'previous-color.txt' $active
        Set-StateValue 'current-url.txt' (Get-SlotUrl $previous)
        Add-History "ROLLBACK_SUCCESS active=$previous previous=$active port=$(Get-SlotPort $previous)"
        Write-Output "ROLLBACK_SUCCESS active=$previous previous=$active status=$($health.status)"
    }

    'status' {
        $active = Get-StateValue 'active-color.txt'
        $previous = Get-StateValue 'previous-color.txt'
        Write-Output "active=$active previous=$previous currentUrl=$(Get-StateValue 'current-url.txt' '')"
        foreach ($color in @('blue', 'green')) {
            $process = Get-SlotProcess $color
            $state = if ($null -ne $process) { 'running' } else { 'stopped' }
            $healthState = 'DOWN'
            if ($state -eq 'running') {
                try { $healthState = (Invoke-RestMethod -Uri "$(Get-SlotUrl $color)/api/health" -TimeoutSec 2).status } catch { }
            }
            Write-Output "slot=$color port=$(Get-SlotPort $color) process=$state health=$healthState"
        }
    }

    'cleanup' {
        Stop-Slot 'blue'
        Stop-Slot 'green'
        Add-History 'CLEANUP_COMPLETE'
        Write-Output 'CLEANUP_COMPLETE'
    }
}
