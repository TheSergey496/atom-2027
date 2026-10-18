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
        [string[]]$Arguments,
        [switch]$SuppressOutput
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
        $result.NotFound = $true; $result.ExitCode = -1; return $result
    }
    catch {
        Write-Log ("Команда завершилась исключением: {0}" -f $_.Exception.Message) 'ERROR'
        $result.ExitCode = -1; return $result
    }

    $clean = New-Object System.Collections.Generic.List[string]
    foreach ($line in $raw) {
        $text = [string]$line
        if (Test-IsBenign $text) {
            if (-not $SuppressOutput) { Write-Log $text 'WARN' }
        }
        else {
            if (-not $SuppressOutput) { Write-Log $text }
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

# --- Разбор health-JSON -----------------------------------------------------

<#
Возвращает @{ Lines = string[]; Summary = string[] }.
Lines  — читаемое представление ошибки для лога;
Summary — короткие пары "component=STATUS (detail=..., ...)" для итоговой сводки.
#>
function Get-HealthComponentErrors {
    param([string]$Output)

    $lines   = New-Object System.Collections.Generic.List[string]
    $summary = New-Object System.Collections.Generic.List[string]
    $empty   = @{ Lines = $lines; Summary = $summary }

    if ([string]::IsNullOrWhiteSpace($Output)) { return $empty }

    # Health-выход может содержать префикс/суффикс от curl — берём «{...}».
    $start = $Output.IndexOf('{')
    $end   = $Output.LastIndexOf('}')
    if ($start -lt 0 -or $end -le $start) { return $empty }
    $jsonText = $Output.Substring($start, $end - $start + 1)

    $obj = $null
    try { $obj = $jsonText | ConvertFrom-Json -ErrorAction Stop } catch {
        return $empty
    }
    if ($obj -isnot [System.Management.Automation.PSCustomObject]) { return $empty }

    $topStatus = $null
    if ($obj.PSObject.Properties.Name -contains 'status') { $topStatus = [string]$obj.status }

    $components = $null
    if ($obj.PSObject.Properties.Name -contains 'components') { $components = $obj.components }

    if ($null -ne $components -and $components -is [System.Management.Automation.PSCustomObject]) {
        $bad = @()
        foreach ($prop in $components.PSObject.Properties) {
            $comp   = $prop.Value
            $status = $null
            if ($comp -and ($comp.PSObject.Properties.Name -contains 'status')) {
                $status = [string]$comp.status
            }
            if ($status -and $status -ne 'UP') {
                $bad += ,[pscustomobject]@{ Name = $prop.Name; Component = $comp; Status = $status }
            }
        }

        if ($bad.Count -gt 0) {
            $overallText = if ($topStatus) { $topStatus } else { 'UNKNOWN' }
            $lines.Add("Health: компоненты с проблемами (overall=$overallText):")

            foreach ($b in $bad) {
                $lines.Add("- $($b.Name): status=$($b.Status)")

                $detailPairs = @()
                if (($b.Component.PSObject.Properties.Name -contains 'details') -and $b.Component.details) {
                    foreach ($dl in (Format-DetailsObject -Details $b.Component.details -Indent '    ')) {
                        $lines.Add($dl)
                    }
                    foreach ($dp in $b.Component.details.PSObject.Properties) {
                        $v = $dp.Value
                        if ($null -eq $v) {
                            $detailPairs += "$($dp.Name)=null"
                        }
                        elseif ($v -is [string] -or $v -is [int] -or $v -is [long] -or
                                $v -is [double] -or $v -is [bool]) {
                            $detailPairs += "$($dp.Name)=$v"
                        }
                        else {
                            $detailPairs += "$($dp.Name)=<json>"
                        }
                    }
                }

                $summaryText = "$($b.Name)=$($b.Status)"
                if ($detailPairs.Count -gt 0) {
                    $summaryText += ' (' + ($detailPairs -join ', ') + ')'
                }
                $summary.Add($summaryText)
            }
        }
    }
    elseif ($topStatus -and $topStatus -ne 'UP') {
        $lines.Add("Health status: $topStatus")
        $summary.Add("status=$topStatus")
    }

    return @{ Lines = $lines; Summary = $summary }
}

function Format-DetailsObject {
    param($Details, [string]$Indent = '  ')
    $lines = New-Object System.Collections.Generic.List[string]
    if ($null -eq $Details) { return ,$lines }
    foreach ($p in $Details.PSObject.Properties) {
        $v = $p.Value
        if ($null -eq $v) {
            $lines.Add("$Indent$($p.Name): (null)")
        }
        elseif ($v -is [string] -or $v -is [int] -or $v -is [long] -or
                $v -is [double] -or $v -is [bool]) {
            $lines.Add("$Indent$($p.Name): $v")
        }
        else {
            $ser = $null
            try { $ser = ConvertTo-Json -InputObject $v -Compress -Depth 5 -ErrorAction Stop }
            catch { $ser = [string]$v }
            $lines.Add("$Indent$($p.Name): $ser")
        }
    }
    return ,$lines
}

<#
Полный разбор одного контейнера через docker inspect.
Возвращает @{ Lines = string[]; Problems = string[] }.
#>
function Get-ContainerDiagnostic {
    param([string]$ContainerId, [string]$Service)

    $lines    = New-Object System.Collections.Generic.List[string]
    $problems = New-Object System.Collections.Generic.List[string]

    $r = Invoke-Docker -FilePath 'docker' -SuppressOutput `
        -Arguments @('inspect', '--format', '{{json .State}}', $ContainerId)
    if ($r.ExitCode -ne 0 -or $r.Output.Count -eq 0) {
        $lines.Add("${Service}: не удалось выполнить docker inspect для $ContainerId")
        $problems.Add('inspect failed')
        return @{ Lines = $lines; Problems = $problems }
    }

    $jsonLine = $null
    foreach ($l in $r.Output) {
        $t = ([string]$l).Trim()
        if ($t.StartsWith('{') -and $t.EndsWith('}')) { $jsonLine = $t; break }
    }
    if (-not $jsonLine) {
        $lines.Add("${Service}: JSON не найден в выводе docker inspect")
        foreach ($l in $r.Output) { $lines.Add("  raw: $l") }
        $problems.Add('inspect JSON not found')
        return @{ Lines = $lines; Problems = $problems }
    }

    $state = $null
    try { $state = $jsonLine | ConvertFrom-Json -ErrorAction Stop }
    catch {
        $lines.Add("${Service}: ошибка разбора docker inspect: $($_.Exception.Message)")
        $problems.Add('inspect parse error')
        return @{ Lines = $lines; Problems = $problems }
    }

    $status = [string]$state.Status
    $lines.Add("${Service}: Status=$status ExitCode=$($state.ExitCode)")

    if ($status -ne 'running') {
        $problems.Add("state=$status")
    }

    if ($state.Error) {
        $lines.Add("  Container error: $($state.Error)")
        $problems.Add("container error: $($state.Error)")
    }

    $health = $state.Health
    if ($null -eq $health) {
        $lines.Add('  Healthcheck не задан')
        return @{ Lines = $lines; Problems = $problems }
    }

    $healthStatus  = [string]$health.Status
    $failingStreak = $health.FailingStreak
    $lines.Add("  Health=$healthStatus FailingStreak=$failingStreak")
    if ($healthStatus -and $healthStatus -ne 'healthy') {
        $problems.Add("health=$healthStatus (failingStreak=$failingStreak)")
    }

    $logEntries = @()
    if ($health.Log) { $logEntries = @($health.Log | Where-Object { $_ }) }
    if ($logEntries.Count -eq 0) {
        $lines.Add('  Результатов health-проб пока нет')
        return @{ Lines = $lines; Problems = $problems }
    }

    $failedEntries = @($logEntries | Where-Object { [int]$_.ExitCode -ne 0 })
    $entriesToShow = @()
    if ($failedEntries.Count -gt 0) {
        $candidates     = @($failedEntries | Select-Object -Last 5)
        $distinctByError = [ordered]@{}
        foreach ($e in $candidates) {
            $out = [string]$e.Output
            $cr  = Get-HealthComponentErrors -Output $out
            $key = if ($cr.Summary.Count -gt 0) {
                $cr.Summary -join '; '
            } else {
                $t = $out.Trim()
                if ($t.Length -gt 200) { $t = $t.Substring(0, 200) }
                $t
            }
            $distinctByError[$key] = @{ Entry = $e; Result = $cr }
        }
        $entriesToShow = @($distinctByError.Values | Select-Object -Last 2)
    } else {
        $last = $logEntries[-1]
        $entriesToShow = @(
            @{ Entry  = $last
               Result = (Get-HealthComponentErrors -Output ([string]$last.Output)) }
        )
    }

    $summaryCaptured = $false
    foreach ($item in $entriesToShow) {
        $entry           = $item.Entry
        $componentResult = $item.Result
        $out             = [string]$entry.Output

        $lines.Add("  Probe: exit=$($entry.ExitCode) end=$($entry.End)")

        if ($componentResult.Lines.Count -gt 0) {
            foreach ($cl in $componentResult.Lines) { $lines.Add("  $cl") }
        }
        if (-not $summaryCaptured -and $componentResult.Summary.Count -gt 0) {
            $problems.Add($componentResult.Summary -join '; ')
            $summaryCaptured = $true
        }

        if ($componentResult.Lines.Count -eq 0 -and -not [string]::IsNullOrWhiteSpace($out)) {
            $raw = $out.Trim()
            if ($raw.Length -gt 800) { $raw = $raw.Substring(0, 800) + '...' }
            foreach ($rl in ($raw -split "`r?`n")) {
                if ($rl.Trim()) { $lines.Add("    $rl") }
            }
            if (-not $summaryCaptured -and $healthStatus -ne 'healthy') {
                $problems.Add("probe exit=$($entry.ExitCode)")
                $summaryCaptured = $true
            }
        }
    }

    return @{ Lines = $lines; Problems = $problems }
}

function Write-ServiceDiagnostics {
    param([string[]]$Services)

    Write-Log '=== Диагностика контейнеров ===' 'ERROR'

    $r = Invoke-Docker -FilePath 'docker' -SuppressOutput `
        -Arguments @('compose', '-f', $script:ComposeFile, 'ps', '-a')
    foreach ($l in $r.Output) { Write-Log $l 'ERROR' }

    $allProblems = New-Object System.Collections.Generic.List[string]

    foreach ($svc in $Services) {
        $ids = @(& docker compose -f $script:ComposeFile ps -a -q $svc 2>$null | Where-Object { $_ })
        if ($ids.Count -eq 0) {
            Write-Log "${svc}: контейнер не найден" 'ERROR'
            $allProblems.Add("${svc}: not found")
            continue
        }

        foreach ($id in $ids) {
            $diag = Get-ContainerDiagnostic -ContainerId $id -Service $svc
            foreach ($line in $diag.Lines) { Write-Log $line 'ERROR' }
            foreach ($p in $diag.Problems) { $allProblems.Add("${svc}: $p") }
        }
    }

    Write-Log '=== Конец диагностики ===' 'ERROR'

    $unique = @($allProblems | Select-Object -Unique)
    if ($unique.Count -gt 0) {
        # Сводка идёт последней — её и увидит Inno Setup через GetTail().
        Write-Log '=== СВОДКА ПРОБЛЕМ ===' 'ERROR'
        foreach ($u in $unique) { Write-Log "  $u" 'ERROR' }
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

    foreach ($logPath in @($script:ComposeOut, $script:ComposeErr)) {
        if (Test-Path -LiteralPath $logPath) {
            Get-Content -LiteralPath $logPath -ErrorAction SilentlyContinue | ForEach-Object {
                $line = [string]$_
                if ($line -and -not (Test-IsBenign $line)) { Write-Log $line }
            }
        }
    }

    $composeFailed = ($proc.ExitCode -ne 0)
    $healthy       = $false

    if (-not $composeFailed) {
        # 9. Ожидание здоровья — только если compose up не упал сам.
        Write-Log 'Ожидание перехода контейнеров в состояние healthy ...'
        $healthy = Wait-ForHealthy -Services $script:Services -TimeoutSeconds $TimeoutSeconds
        Write-Log ("Wait-ForHealthy завершился: healthy=$healthy")
    }

    # 10. Единая точка диагностики — срабатывает на ОБОИХ путях отказа.
    if ($composeFailed -or -not $healthy) {
        Write-ServiceDiagnostics -Services $script:Services
    }

    if ($composeFailed) {
        Stop-WithError "docker compose up завершился с ошибкой (код $($proc.ExitCode))." $script:EXIT_COMPOSE_UP
    }
    if (-not $healthy) {
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