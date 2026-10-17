<#
.SYNOPSIS
    Загружает образы приложения и запускает контейнеры Docker Compose,
    ожидая, пока каждый из них перейдёт в состояние "healthy".

    Коды выхода (синхронизированы с installer.iss):
        0   успех
        10  docker CLI не найден
        11  демон Docker не запущен
        12  плагин docker compose недоступен
        13  images.tar не найден
        14  не удалось загрузить образы
        20  docker compose up завершился с ошибкой
        21  контейнеры не стали healthy за отведённое время
        99  непредвиденная ошибка
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$InstallDir,
    [string]$BackendHost = '127.0.0.1',
    [ValidateRange(1, 65535)][int]$BackendPort = 8496,
    [string]$FrontendHost = '127.0.0.1',
    [ValidateRange(1, 65535)][int]$FrontendPort = 4963,
    [ValidateRange(1, 65535)][int]$DbPort = 5496,
    [ValidateRange(30, 3600)][int]$TimeoutSeconds = 300
)

$ErrorActionPreference = 'Stop'
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

# --- Коды выхода (единый источник истины) -----------------------------------
$script:EXIT_OK              = 0
$script:EXIT_UNKNOWN         = 99
$script:EXIT_DOCKER_MISSING  = 10
$script:EXIT_DOCKER_DOWN     = 11
$script:EXIT_COMPOSE_MISSING = 12
$script:EXIT_TAR_MISSING     = 13
$script:EXIT_IMAGE_LOAD      = 14
$script:EXIT_COMPOSE_UP      = 20
$script:EXIT_NOT_HEALTHY     = 21

# --- Пути -------------------------------------------------------------------
$script:LogFile      = Join-Path $InstallDir 'install.log'
$script:MarkerFile   = Join-Path $InstallDir 'install.exit'
$script:PidFile      = Join-Path $InstallDir 'install.pid'
$script:ComposeFile  = Join-Path $InstallDir 'docker-compose.yml'
$script:ImagesTar    = Join-Path $InstallDir 'images.tar'
$script:ComposeOut   = Join-Path $InstallDir 'docker-compose.out.log'
$script:ComposeErr   = Join-Path $InstallDir 'docker-compose.err.log'

$script:Services = @('db', 'backend', 'frontend')

# Предупреждения Docker, которые не являются ошибкой.
$script:BenignPatterns = @(
    'No blkio throttle\.',
    'No cpu cfs (quota|period) support',
    'No cpu shares support',
    'No memory limit support',
    'No memory swap limit support',
    'daemon is not using the default seccomp profile',
    '^(Creating|Starting|Recreating|Stopping|Removing|Network|Volume|Container)\s'
)

# --- Логирование ------------------------------------------------------------
function Write-Log {
    param([string]$Message, [string]$Level = 'INFO')
    $ts   = Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff'
    $line = "[$ts] [$Level] $Message"
    Write-Host $line
    try { Add-Content -LiteralPath $script:LogFile -Value $line -Encoding UTF8 } catch {}
}

function Test-IsBenign {
    param([string]$Line)
    foreach ($p in $script:BenignPatterns) {
        if ($Line -match $p) { return $true }
    }
    return $false
}

# --- Завершение -------------------------------------------------------------
function Set-ExitCode {
    param([int]$Code)
    try { Set-Content -LiteralPath $script:MarkerFile -Value $Code -Encoding ASCII -Force } catch {}
}

function Stop-WithError {
    param([string]$Message, [int]$Code = $script:EXIT_UNKNOWN)
    Write-Log $Message 'ERROR'
    Set-ExitCode $Code
    exit $Code
}

# --- Обёртка над внешними командами -----------------------------------------
function Invoke-Docker {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )

    $result = @{ Output = @(); ExitCode = -1; NotFound = $false }

    if (-not (Get-Command -Name $FilePath -ErrorAction SilentlyContinue)) {
        $result.NotFound = $true
        return $result
    }

    Write-Log ("Запуск: {0} {1}" -f $FilePath, ($Arguments -join ' '))

    $raw = @()
    try {
        $raw = & $FilePath @Arguments 2>&1
    }
    catch [System.Management.Automation.CommandNotFoundException] {
        $result.NotFound = $true
        $result.ExitCode = -1
        return $result
    }
    catch {
        Write-Log ("Команда завершилась исключением: {0}" -f $_.Exception.Message) 'ERROR'
        $result.ExitCode = -1
        return $result
    }

    $clean = New-Object System.Collections.Generic.List[string]
    foreach ($line in $raw) {
        $text = [string]$line
        if (Test-IsBenign $text) {
            Write-Log $text 'WARN'
        }
        else {
            Write-Log $text
            $clean.Add($text)
        }
    }

    $result.ExitCode = $LASTEXITCODE
    $result.Output   = $clean.ToArray()
    return $result
}

# --- Валидация --------------------------------------------------------------
function Test-HostOrIp {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) { return $false }
    if ($Value.Length -gt 253) { return $false }
    if ($Value -match '[\s"''`$;|&<>()\[\]{}\\#]') { return $false }

    # IPv4
    if ($Value -match '^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$') {
        foreach ($g in 1..4) {
            $n = [int]$Matches[$g]
            if ($n -lt 0 -or $n -gt 255) { return $false }
        }
        return $true
    }

    # IPv6
    if ($Value.Contains(':') -and $Value -match '^[0-9a-fA-F:.]+(%[0-9a-zA-Z._-]+)?$') {
        return $true
    }

    # DNS
    if ($Value -match '^[a-zA-Z0-9]([a-zA-Z0-9\-\.]{0,251}[a-zA-Z0-9])?$' -and -not $Value.Contains('..')) {
        return $true
    }

    return $false
}

# --- Проверка готовности контейнеров ----------------------------------------
function Test-ServiceReady {
    param([string]$Service)

    $ids = @(& docker compose -f $script:ComposeFile ps -a -q $Service 2>$null | Where-Object { $_ })
    if ($ids.Count -eq 0) {
        return @{ Ready = $false; Fatal = $false; State = 'missing'; Health = 'missing'; ContainerId = $null }
    }

    $fmt = '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}'
    $allReady = $true
    $fatal    = $false
    $lastState = 'unknown'; $lastHealth = 'none'; $lastId = $null

    foreach ($id in $ids) {
        $info = (& docker inspect --format $fmt $id 2>$null)
        if ($LASTEXITCODE -ne 0 -or -not $info) {
            $allReady = $false
            continue
        }
        $parts  = ([string]$info).Trim() -split '\|', 2
        $state  = if ($parts.Count -ge 1) { $parts[0] } else { 'unknown' }
        $health = if ($parts.Count -ge 2) { $parts[1] } else { 'none' }
        $lastState = $state; $lastHealth = $health; $lastId = $id

        if ($state -eq 'exited' -or $state -eq 'dead') {
            $fatal = $true; $allReady = $false
        }
        elseif ($state -eq 'running' -and ($health -eq 'healthy' -or $health -eq 'none')) {
            # ok
        }
        else {
            $allReady = $false
        }
    }

    return @{ Ready = $allReady; Fatal = $fatal; State = $lastState; Health = $lastHealth; ContainerId = $lastId }
}

function Wait-ForHealthy {
    param([string[]]$Services, [int]$TimeoutSeconds)

    $deadline   = (Get-Date).AddSeconds($TimeoutSeconds)
    $pollSecs   = 2
    while ((Get-Date) -lt $deadline) {
        $allReady = $true
        $fatal    = $false
        foreach ($svc in $Services) {
            $r = Test-ServiceReady -Service $svc
            if ($r.Fatal) { $fatal = $true }
            if (-not $r.Ready) { $allReady = $false }
        }
        if ($allReady) { return $true }
        if ($fatal)    { return $false }
        Start-Sleep -Seconds $pollSecs
    }
    return $false
}

function Write-ServiceDiagnostics {
    param([string[]]$Services)
    foreach ($svc in $Services) {
        $r = Test-ServiceReady -Service $svc
        Write-Log ("{0}: state={1}, health={2}, id={3}" -f $svc, $r.State, $r.Health, $r.ContainerId) 'ERROR'
    }
}

# --- Main -------------------------------------------------------------------
try {
    Write-Log '=== Запуск инсталлятора It Just Works ==='
    Write-Log ("Каталог установки : {0}" -f $InstallDir)
    Write-Log ("BackendHost       : {0}" -f $BackendHost)
    Write-Log ("BackendPort       : {0}" -f $BackendPort)
    Write-Log ("FrontendHost      : {0}" -f $FrontendHost)
    Write-Log ("FrontendPort      : {0}" -f $FrontendPort)
    Write-Log ("DbPort            : {0}" -f $DbPort)
    Write-Log ("Таймаут           : {0}s" -f $TimeoutSeconds)
    Write-Log ("PowerShell        : {0}" -f $PSVersionTable.PSVersion)
    Write-Log ("OS                : {0}" -f [Environment]::OSVersion)

    # 0. Валидация параметров
    if (-not (Test-Path -LiteralPath $InstallDir -PathType Container)) {
        Stop-WithError "Каталог установки не существует или не является каталогом: $InstallDir" $script:EXIT_UNKNOWN
    }
    if (-not (Test-HostOrIp -Value $BackendHost)) {
        Stop-WithError "BackendHost не является корректным IPv4/IPv6/хостом: '$BackendHost'" $script:EXIT_UNKNOWN
    }
    if (-not (Test-HostOrIp -Value $FrontendHost)) {
        Stop-WithError "FrontendHost не является корректным IPv4/IPv6/хостом: '$FrontendHost'" $script:EXIT_UNKNOWN
    }

    Set-Location -LiteralPath $InstallDir

    # PID для возможности прибить зависший процесс снаружи.
    try { Set-Content -LiteralPath $script:PidFile -Value $PID -Encoding ASCII -Force } catch {}

    # 1. Наличие Docker CLI
    if (-not (Get-Command -Name 'docker' -ErrorAction SilentlyContinue)) {
        Stop-WithError ('Docker не установлен или отсутствует в PATH. ' +
                        'Установите Docker Desktop с https://www.docker.com/products/docker-desktop') `
                       $script:EXIT_DOCKER_MISSING
    }

    $r = Invoke-Docker -FilePath 'docker' -Arguments @('--version')
    if ($r.NotFound -or $r.ExitCode -ne 0) {
        Stop-WithError 'Docker CLI не найден или не работает (docker --version).' $script:EXIT_DOCKER_MISSING
    }

    # 2. Доступность демона
    $r = Invoke-Docker -FilePath 'docker' -Arguments @('info', '--format', '{{.ServerVersion}}')
    if ($r.ExitCode -ne 0) {
        Stop-WithError 'Демон Docker не запущен. Запустите Docker Desktop и повторите установку.' $script:EXIT_DOCKER_DOWN
    }

    # 3. Наличие плагина compose
    $r = Invoke-Docker -FilePath 'docker' -Arguments @('compose', 'version')
    if ($r.ExitCode -ne 0) {
        Stop-WithError 'Плагин docker compose недоступен. Обновите Docker Desktop.' $script:EXIT_COMPOSE_MISSING
    }

    # 4. Наличие docker-compose.yml
    if (-not (Test-Path -LiteralPath $script:ComposeFile -PathType Leaf)) {
        Stop-WithError "Файл docker-compose.yml не найден в $InstallDir" $script:EXIT_UNKNOWN
    }

    # 5. Наличие images.tar
    if (-not (Test-Path -LiteralPath $script:ImagesTar -PathType Leaf)) {
        Stop-WithError "Файл images.tar не найден в $InstallDir" $script:EXIT_TAR_MISSING
    }

    # 6. Загрузка образов
    Write-Log "Загрузка образов из $script:ImagesTar ..."
    $r = Invoke-Docker -FilePath 'docker' -Arguments @('load', '-i', $script:ImagesTar)
    if ($r.ExitCode -ne 0) {
        Stop-WithError "Не удалось загрузить образы (код $($r.ExitCode))." $script:EXIT_IMAGE_LOAD
    }

    # 7. Переменные окружения для интерполяции в compose
    $env:BACKEND_HOST = $BackendHost
    $env:BACKEND_PORT = $BackendPort
    $env:FRONTEND_PORT = $FrontendPort
    $env:DB_PORT = $DbPort

    # 8. Запуск стека
    Remove-Item -LiteralPath $script:ComposeOut -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $script:ComposeErr -Force -ErrorAction SilentlyContinue

    Write-Log 'Запуск: docker compose up -d --no-build'
    $proc = Start-Process -FilePath 'docker' `
        -ArgumentList @('compose', 'up', '-d', '--no-build') `
        -WorkingDirectory $InstallDir `
        -NoNewWindow -Wait -PassThru `
        -RedirectStandardOutput $script:ComposeOut `
        -RedirectStandardError  $script:ComposeErr

    if ($null -eq $proc) {
        Stop-WithError 'Не удалось запустить docker compose up.' $script:EXIT_COMPOSE_UP
    }

    # Копируем захваченный вывод в главный лог.
    foreach ($logPath in @($script:ComposeOut, $script:ComposeErr)) {
        if (Test-Path -LiteralPath $logPath) {
            Get-Content -LiteralPath $logPath -ErrorAction SilentlyContinue | ForEach-Object {
                $line = [string]$_
                if ($line -and -not (Test-IsBenign $line)) { Write-Log $line }
            }
        }
    }

    if ($proc.ExitCode -ne 0) {
        Invoke-Docker -FilePath 'docker' -Arguments @('compose', 'ps') | Out-Null
        Stop-WithError "docker compose up завершился с ошибкой (код $($proc.ExitCode))." $script:EXIT_COMPOSE_UP
    }

    # 9. Ожидание здоровья
    Write-Log 'Ожидание перехода контейнеров в состояние healthy ...'
    if (-not (Wait-ForHealthy -Services $script:Services -TimeoutSeconds $TimeoutSeconds)) {
        Invoke-Docker -FilePath 'docker' -Arguments @('compose', 'ps') | Out-Null
        Write-ServiceDiagnostics -Services $script:Services
        Stop-WithError 'Контейнеры не перешли в состояние «healthy» за отведённое время.' $script:EXIT_NOT_HEALTHY
    }

    Write-Log 'Все контейнеры здоровы. Установка успешно завершена.'
    Set-ExitCode $script:EXIT_OK
    exit $script:EXIT_OK
}
catch {
    Stop-WithError ("Непредвиденная ошибка: {0}" -f $_.Exception.Message) $script:EXIT_UNKNOWN
}
finally {
    try { Remove-Item -LiteralPath $script:PidFile -Force -ErrorAction SilentlyContinue } catch {}
}