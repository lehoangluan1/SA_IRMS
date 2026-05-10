$ErrorActionPreference = "Stop"

# ============================================================
# Auto run as Administrator, but keep window open
# ============================================================

$currentIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal($currentIdentity)
$isAdmin = $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
  Write-Host "Restarting script as Administrator..."

  $argLine = "-NoExit -NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`""
  Start-Process powershell.exe -Verb RunAs -ArgumentList $argLine
  exit
}

# ============================================================
# Paths
# ============================================================

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackendDir = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend"
$LogDir = Join-Path $RootDir ".logs\local"
New-Item -ItemType Directory -Path $LogDir -Force | Out-Null

# ============================================================
# Default environment
# ============================================================

$env:DB_HOST = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$env:DB_PORT = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }
$env:DB_NAME = if ($env:DB_NAME) { $env:DB_NAME } else { "irms" }

if (-not $env:DB_USER -and -not $env:DB_USERNAME) {
  $env:DB_USER = "postgres"
}
elseif ($env:DB_USER) {
  $env:DB_USER = $env:DB_USER
}
elseif ($env:DB_USERNAME) {
  $env:DB_USER = $env:DB_USERNAME
}

$env:DB_USERNAME = $env:DB_USER
$env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }

$env:JDBC_DATABASE_URL = if ($env:JDBC_DATABASE_URL) {
  $env:JDBC_DATABASE_URL
} else {
  "jdbc:postgresql://$($env:DB_HOST):$($env:DB_PORT)/$($env:DB_NAME)"
}

$env:RABBITMQ_HOST = if ($env:RABBITMQ_HOST) { $env:RABBITMQ_HOST } else { "localhost" }
$env:RABBITMQ_PORT = if ($env:RABBITMQ_PORT) { $env:RABBITMQ_PORT } else { "5672" }
$env:RABBITMQ_USERNAME = if ($env:RABBITMQ_USERNAME) { $env:RABBITMQ_USERNAME } else { "guest" }
$env:RABBITMQ_PASSWORD = if ($env:RABBITMQ_PASSWORD) { $env:RABBITMQ_PASSWORD } else { "guest" }

$env:VITE_API_BASE_URL = if ($env:VITE_API_BASE_URL) {
  $env:VITE_API_BASE_URL
} else {
  "http://localhost:8080"
}

$env:IRMS_INTERNAL_SERVICE_TOKEN = if ($env:IRMS_INTERNAL_SERVICE_TOKEN) {
  $env:IRMS_INTERNAL_SERVICE_TOKEN
} else {
  "dev-internal-service-token-change-me"
}

$env:IRMS_ACCESS_TOKEN_SIGNING_SECRET = if ($env:IRMS_ACCESS_TOKEN_SIGNING_SECRET) {
  $env:IRMS_ACCESS_TOKEN_SIGNING_SECRET
} else {
  "dev-access-token-signing-secret-change-me"
}

# Giới hạn RAM cho mỗi Maven/Spring process.
# Nếu service bị chết do OutOfMemory, tăng dòng này lên -Xmx768m hoặc -Xmx1024m.
$env:IRMS_LOCAL_JVM_OPTS = if ($env:IRMS_LOCAL_JVM_OPTS) {
  $env:IRMS_LOCAL_JVM_OPTS
} else {
  "-Xms128m -Xmx512m -XX:MaxMetaspaceSize=256m"
}

$services = @(
  @{ Name = "api-gateway"; Port = 8080 },
  @{ Name = "ordering-service"; Port = 8081 },
  @{ Name = "kitchen-service"; Port = 8082 },
  @{ Name = "billing-service"; Port = 8083 },
  @{ Name = "reservation-service"; Port = 8084 },
  @{ Name = "inventory-service"; Port = 8085 },
  @{ Name = "notification-service"; Port = 8086 },
  @{ Name = "reporting-service"; Port = 8087 },
  @{ Name = "identity-audit-service"; Port = 8088 }
)

$localPorts = @()
foreach ($service in $services) {
  $localPorts += $service.Port
}
$localPorts += 5173

$started = New-Object System.Collections.Generic.List[object]
$writers = New-Object System.Collections.Generic.List[System.IO.StreamWriter]
$scriptFailed = $false

# ============================================================
# Helpers
# ============================================================

function Require-Command([string]$CommandName) {
  $cmd = Get-Command $CommandName -ErrorAction SilentlyContinue

  if (-not $cmd) {
    throw "Missing required command: $CommandName"
  }

  return $cmd
}

function Require-Path([string]$Path, [string]$Description) {
  if (-not (Test-Path $Path)) {
    throw "Missing $Description`: $Path"
  }
}

function Get-LogTail([string]$Path, [int]$Lines = 100) {
  if (Test-Path $Path) {
    return (Get-Content $Path -Tail $Lines -ErrorAction SilentlyContinue) -join "`n"
  }

  return ""
}

function Invoke-NativeCommand {
  param(
    [Parameter(Mandatory = $true)]
    [string]$FileName,

    [string]$Arguments = "",

    [string]$WorkingDirectory = "",

    [hashtable]$EnvironmentVars = @{},

    [string]$LogFile = "",

    [switch]$EchoOutput
  )

  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $FileName
  $psi.Arguments = $Arguments

  if ($WorkingDirectory) {
    $psi.WorkingDirectory = $WorkingDirectory
  }

  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true

  foreach ($key in $EnvironmentVars.Keys) {
    $psi.EnvironmentVariables[$key] = [string]$EnvironmentVars[$key]
  }

  $process = New-Object System.Diagnostics.Process
  $process.StartInfo = $psi

  $null = $process.Start()

  $stdout = $process.StandardOutput.ReadToEnd()
  $stderr = $process.StandardError.ReadToEnd()

  $process.WaitForExit()

  $combined = "$stdout`n$stderr".Trim()

  if ($EchoOutput -and $combined) {
    Write-Host $combined
  }

  if ($LogFile) {
    $combined | Out-File -FilePath $LogFile -Encoding UTF8
  }

  return @{
    ExitCode = $process.ExitCode
    Output = $combined
  }
}

function Get-CommonEnvironment {
  $map = @{}

  $map["DB_HOST"] = $env:DB_HOST
  $map["DB_PORT"] = $env:DB_PORT
  $map["DB_NAME"] = $env:DB_NAME
  $map["DB_USER"] = $env:DB_USER
  $map["DB_USERNAME"] = $env:DB_USERNAME
  $map["DB_PASSWORD"] = $env:DB_PASSWORD
  $map["JDBC_DATABASE_URL"] = $env:JDBC_DATABASE_URL

  $map["RABBITMQ_HOST"] = $env:RABBITMQ_HOST
  $map["RABBITMQ_PORT"] = $env:RABBITMQ_PORT
  $map["RABBITMQ_USERNAME"] = $env:RABBITMQ_USERNAME
  $map["RABBITMQ_PASSWORD"] = $env:RABBITMQ_PASSWORD

  $map["IRMS_INTERNAL_SERVICE_TOKEN"] = $env:IRMS_INTERNAL_SERVICE_TOKEN
  $map["IRMS_ACCESS_TOKEN_SIGNING_SECRET"] = $env:IRMS_ACCESS_TOKEN_SIGNING_SECRET

  $map["MAVEN_OPTS"] = $env:IRMS_LOCAL_JVM_OPTS
  $map["JAVA_TOOL_OPTIONS"] = $env:IRMS_LOCAL_JVM_OPTS

  return $map
}

function Add-EnvironmentValue {
  param(
    [Parameter(Mandatory = $true)]
    [hashtable]$Map,

    [Parameter(Mandatory = $true)]
    [string]$Name,

    [Parameter(Mandatory = $true)]
    [string]$Value
  )

  $Map[$Name] = $Value
}

# ============================================================
# Java
# ============================================================

function Assert-SupportedJava {
  $result = Invoke-NativeCommand -FileName "java" -Arguments "-version"

  if ($result.ExitCode -ne 0) {
    throw "Failed to run java -version. Output: $($result.Output)"
  }

  $versionOutput = $result.Output
  $major = $null

  if ($versionOutput -match 'version\s+"(\d+)\.') {
    $major = [int]$Matches[1]
  }
  elseif ($versionOutput -match 'version\s+"1\.(\d+)\.') {
    $major = [int]$Matches[1]
  }

  if ($null -eq $major) {
    throw "Could not detect Java version. Output: $versionOutput"
  }

  $supportedVersions = @(17, 21, 23)

  if ($supportedVersions -notcontains $major) {
    throw "Unsupported Java version: $major. Supported versions are: 17, 21, 23. Current output: $versionOutput"
  }

  Write-Host "Java version OK: $major"
}

# ============================================================
# Node / npm
# ============================================================

function Warn-NodeVersion {
  $nodeResult = Invoke-NativeCommand -FileName "node" -Arguments "-v"

  if ($nodeResult.ExitCode -ne 0) {
    Write-Warning "Could not check Node version."
    return
  }

  $nodeVersion = $nodeResult.Output.Trim()

  if ($nodeVersion -match '^v(\d+)\.(\d+)\.(\d+)') {
    $major = [int]$Matches[1]
    $minor = [int]$Matches[2]

    $supported =
      (($major -eq 20) -and ($minor -ge 19)) -or
      (($major -eq 22) -and ($minor -ge 13)) -or
      ($major -ge 24)

    if (-not $supported) {
      Write-Warning "Current Node version is $nodeVersion. Some frontend packages prefer Node ^20.19.0, ^22.13.0, or >=24."
      Write-Warning "The script will continue, but frontend may show npm EBADENGINE warnings."
    }
    else {
      Write-Host "Node version OK: $nodeVersion"
    }
  }
}

# ============================================================
# PostgreSQL / psql
# ============================================================

function Find-Psql {
  $cmd = Get-Command psql -ErrorAction SilentlyContinue

  if ($cmd) {
    return $cmd.Source
  }

  $candidateRoots = @(
    "$env:ProgramFiles\PostgreSQL",
    "${env:ProgramFiles(x86)}\PostgreSQL"
  )

  foreach ($root in $candidateRoots) {
    if (Test-Path $root) {
      $found = Get-ChildItem -Path $root -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object {
          Join-Path $_.FullName "bin\psql.exe"
        } |
        Where-Object {
          Test-Path $_
        } |
        Select-Object -First 1

      if ($found) {
        return $found
      }
    }
  }

  return $null
}

function Invoke-Psql {
  param(
    [Parameter(Mandatory = $true)]
    [string]$PsqlPath,

    [Parameter(Mandatory = $true)]
    [string]$Database,

    [Parameter(Mandatory = $true)]
    [string]$Sql
  )

  $pgEnv = @{}
  $pgEnv["PGPASSWORD"] = $env:DB_PASSWORD

  $args = "-h `"$($env:DB_HOST)`" -p `"$($env:DB_PORT)`" -U `"$($env:DB_USER)`" -d `"$Database`" -tAc `"$Sql`""

  return Invoke-NativeCommand `
    -FileName $PsqlPath `
    -Arguments $args `
    -EnvironmentVars $pgEnv
}

function Ensure-Database {
  $psqlPath = Find-Psql

  if (-not $psqlPath) {
    Write-Warning "psql was not found in PATH or PostgreSQL install folders."
    Write-Warning "Skipping automatic database creation."
    Write-Warning "Make sure database '$($env:DB_NAME)' already exists in pgAdmin."
    Write-Warning "Current DB config: host=$($env:DB_HOST), port=$($env:DB_PORT), user=$($env:DB_USER), database=$($env:DB_NAME)"
    return
  }

  Write-Host "Using psql: $psqlPath"

  $test = Invoke-Psql `
    -PsqlPath $psqlPath `
    -Database "postgres" `
    -Sql "select 1"

  if ($test.ExitCode -ne 0) {
    throw "Cannot connect to PostgreSQL. Check pgAdmin server/user/password. Output: $($test.Output)"
  }

  $exists = Invoke-Psql `
    -PsqlPath $psqlPath `
    -Database "postgres" `
    -Sql "select 1 from pg_database where datname='$($env:DB_NAME)'"

  if ($exists.ExitCode -ne 0) {
    throw "Could not check database '$($env:DB_NAME)'. Output: $($exists.Output)"
  }

  if (-not ($exists.Output -match "1")) {
    Write-Host "Creating database '$($env:DB_NAME)'..."

    $pgEnv = @{}
    $pgEnv["PGPASSWORD"] = $env:DB_PASSWORD

    $createArgs = "-h `"$($env:DB_HOST)`" -p `"$($env:DB_PORT)`" -U `"$($env:DB_USER)`" -d `"postgres`" -c `"create database $($env:DB_NAME)`""

    $create = Invoke-NativeCommand `
      -FileName $psqlPath `
      -Arguments $createArgs `
      -EnvironmentVars $pgEnv

    if ($create.ExitCode -ne 0) {
      throw "Could not create database '$($env:DB_NAME)'. Output: $($create.Output)"
    }
  }
  else {
    Write-Host "Database '$($env:DB_NAME)' already exists."
  }
}

# ============================================================
# Port cleanup
# ============================================================

function Ensure-PortFree([int]$Port) {
  for ($attempt = 1; $attempt -le 3; $attempt++) {
    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

    if (-not $connections) {
      Write-Host "Port $Port is free."
      return
    }

    $processIds = $connections |
      Select-Object -ExpandProperty OwningProcess -Unique |
      Where-Object {
        $_ -and $_ -gt 0 -and $_ -ne $PID
      }

    if (-not $processIds) {
      Start-Sleep -Seconds 1
      continue
    }

    foreach ($processId in $processIds) {
      $processName = "unknown"

      try {
        $processName = (Get-Process -Id $processId -ErrorAction Stop).ProcessName
      }
      catch {
      }

      Write-Warning "Port $Port is already in use by PID=$processId ($processName). Killing owner process only..."

      & taskkill.exe /PID $processId /F *> $null

      if ($LASTEXITCODE -ne 0) {
        try {
          Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        }
        catch {
        }
      }
    }

    Start-Sleep -Seconds 2
  }

  $stillUsed = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

  if ($stillUsed) {
    $stillPids = $stillUsed |
      Select-Object -ExpandProperty OwningProcess -Unique

    Write-Warning "Port $Port is still in use after cleanup. PID(s): $($stillPids -join ', ')"
    Write-Warning "Continuing anyway. This service may fail to start."
  }
  else {
    Write-Host "Port $Port is free."
  }
}

# ============================================================
# Process starters
# ============================================================

function Start-LoggedProcess {
  param(
    [Parameter(Mandatory = $true)]
    [string]$Name,

    [Parameter(Mandatory = $true)]
    [string]$FileName,

    [Parameter(Mandatory = $true)]
    [string]$Arguments,

    [Parameter(Mandatory = $true)]
    [string]$WorkingDirectory,

    [Parameter(Mandatory = $true)]
    [string]$LogFile,

    [hashtable]$EnvironmentVars = @{}
  )

  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $FileName
  $psi.Arguments = $Arguments
  $psi.WorkingDirectory = $WorkingDirectory
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true

  foreach ($key in $EnvironmentVars.Keys) {
    $psi.EnvironmentVariables[$key] = [string]$EnvironmentVars[$key]
  }

  $process = New-Object System.Diagnostics.Process
  $process.StartInfo = $psi

  $writer = [System.IO.StreamWriter]::new($LogFile, $false)
  $writers.Add($writer) | Out-Null

  $outHandler = {
    param($sender, $args)

    if ($null -ne $args.Data) {
      $writer.WriteLine($args.Data)
      $writer.Flush()
    }
  }.GetNewClosure()

  $errHandler = {
    param($sender, $args)

    if ($null -ne $args.Data) {
      $writer.WriteLine($args.Data)
      $writer.Flush()
    }
  }.GetNewClosure()

  $process.add_OutputDataReceived($outHandler)
  $process.add_ErrorDataReceived($errHandler)

  try {
    $null = $process.Start()
    $process.BeginOutputReadLine()
    $process.BeginErrorReadLine()
  }
  catch {
    Write-Warning "Could not start $Name. Error: $($_.Exception.Message)"
    return
  }

  $item = [PSCustomObject]@{
    Name = $Name
    Process = $process
    LogFile = $LogFile
    ReportedExit = $false
  }

  $started.Add($item) | Out-Null

  Start-Sleep -Milliseconds 2500

  if ($process.HasExited) {
    $item.ReportedExit = $true
    Write-Warning "$Name exited immediately with code $($process.ExitCode)."
    Write-Warning "Log file: $LogFile"

    $tail = Get-LogTail -Path $LogFile
    if ($tail) {
      Write-Host ""
      Write-Host "----- Last log lines for $Name -----" -ForegroundColor Yellow
      Write-Host $tail
      Write-Host "------------------------------------" -ForegroundColor Yellow
      Write-Host ""
    }

    return
  }

  Write-Host "Started $Name"
}

function Start-BackendService([string]$ServiceName, [int]$Port) {
  $logFile = Join-Path $LogDir "$ServiceName.log"

  $envMap = Get-CommonEnvironment
  Add-EnvironmentValue -Map $envMap -Name "IRMS_RABBITMQ_ENABLED" -Value "false"
  Add-EnvironmentValue -Map $envMap -Name "IRMS_FLYWAY_ENABLED" -Value "false"
  Add-EnvironmentValue -Map $envMap -Name "IRMS_RUNTIME_MODE" -Value $ServiceName
  Add-EnvironmentValue -Map $envMap -Name "IRMS_RUNTIME_SERVICE" -Value $ServiceName
  Add-EnvironmentValue -Map $envMap -Name "SERVER_PORT" -Value "$Port"

  $command = ".\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,$ServiceName"
  $arguments = "/d /s /c `"$command`""

  Start-LoggedProcess `
    -Name "backend service: $ServiceName on port $Port" `
    -FileName "cmd.exe" `
    -Arguments $arguments `
    -WorkingDirectory $BackendDir `
    -LogFile $logFile `
    -EnvironmentVars $envMap
}

function Start-Frontend {
  $logFile = Join-Path $LogDir "frontend.log"

  $envMap = @{}
  $envMap["VITE_API_BASE_URL"] = $env:VITE_API_BASE_URL

  $command = "npm run dev -- --host 0.0.0.0"
  $arguments = "/d /s /c `"$command`""

  Start-LoggedProcess `
    -Name "frontend on port 5173" `
    -FileName "cmd.exe" `
    -Arguments $arguments `
    -WorkingDirectory $FrontendDir `
    -LogFile $logFile `
    -EnvironmentVars $envMap
}

function Watch-StartedProcesses {
  Write-Host ""
  Write-Host "Monitoring local stack..."
  Write-Host "If a service stops, this window will stay open and show its log."
  Write-Host ""

  while ($true) {
    foreach ($item in $started) {
      if ($item.Process.HasExited -and -not $item.ReportedExit) {
        $item.ReportedExit = $true

        Write-Host ""
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "SERVICE STOPPED" -ForegroundColor Red
        Write-Host "========================================" -ForegroundColor Red
        Write-Host "Service: $($item.Name)"
        Write-Host "Exit code: $($item.Process.ExitCode)"
        Write-Host "Log: $($item.LogFile)"
        Write-Host ""

        $tail = Get-LogTail -Path $item.LogFile
        if ($tail) {
          Write-Host "----- Last log lines -----" -ForegroundColor Yellow
          Write-Host $tail
          Write-Host "--------------------------" -ForegroundColor Yellow
        }

        Write-Host ""
      }
    }

    Start-Sleep -Seconds 2
  }
}

# ============================================================
# Main
# ============================================================

try {
  Require-Path -Path $BackendDir -Description "backend directory"
  Require-Path -Path $FrontendDir -Description "frontend directory"
  Require-Path -Path (Join-Path $BackendDir "mvnw.cmd") -Description "Maven wrapper"
  Require-Path -Path (Join-Path $FrontendDir "package.json") -Description "frontend package.json"

  Require-Command java | Out-Null
  Require-Command node | Out-Null
  Require-Command npm | Out-Null
  Require-Command cmd.exe | Out-Null

  Assert-SupportedJava
  Warn-NodeVersion

  Write-Host ""
  Write-Host "Cleaning old local processes on ports..."
  foreach ($port in $localPorts) {
    Ensure-PortFree $port
  }

  Write-Host ""
  Ensure-Database

  Write-Host ""
  Write-Host "Compiling backend sources..."

  $compileEnv = Get-CommonEnvironment
  Add-EnvironmentValue -Map $compileEnv -Name "IRMS_RUNTIME_MODE" -Value "migration"
  Add-EnvironmentValue -Map $compileEnv -Name "IRMS_RUNTIME_SERVICE" -Value "migration"
  Add-EnvironmentValue -Map $compileEnv -Name "IRMS_RABBITMQ_ENABLED" -Value "false"

  $compile = Invoke-NativeCommand `
    -FileName "cmd.exe" `
    -Arguments '/d /s /c ".\mvnw.cmd -q compile"' `
    -WorkingDirectory $BackendDir `
    -EnvironmentVars $compileEnv `
    -EchoOutput

  if ($compile.ExitCode -ne 0) {
    throw "Backend compile failed. Output: $($compile.Output)"
  }

  Write-Host ""
  Write-Host "Running Flyway migration..."

  $migrationLog = Join-Path $LogDir "migration.log"

  $migrationEnv = Get-CommonEnvironment
  Add-EnvironmentValue -Map $migrationEnv -Name "IRMS_RUNTIME_MODE" -Value "migration"
  Add-EnvironmentValue -Map $migrationEnv -Name "IRMS_RUNTIME_SERVICE" -Value "migration"
  Add-EnvironmentValue -Map $migrationEnv -Name "IRMS_RABBITMQ_ENABLED" -Value "false"
  Add-EnvironmentValue -Map $migrationEnv -Name "IRMS_FLYWAY_ENABLED" -Value "true"
  Add-EnvironmentValue -Map $migrationEnv -Name "SERVER_PORT" -Value "0"

  $migration = Invoke-NativeCommand `
    -FileName "cmd.exe" `
    -Arguments '/d /s /c ".\mvnw.cmd -q spring-boot:run -Dspring-boot.run.profiles=local,migration"' `
    -WorkingDirectory $BackendDir `
    -EnvironmentVars $migrationEnv `
    -LogFile $migrationLog

  if ($migration.ExitCode -ne 0) {
    $tail = Get-LogTail -Path $migrationLog
    throw "Flyway migration failed. Log: $migrationLog`n$tail"
  }

  Write-Host ""
  Write-Host "Installing frontend dependencies if needed..."

  $npmInstall = Invoke-NativeCommand `
    -FileName "cmd.exe" `
    -Arguments '/d /s /c "npm install"' `
    -WorkingDirectory $FrontendDir `
    -EchoOutput

  if ($npmInstall.ExitCode -ne 0) {
    throw "npm install failed. Output: $($npmInstall.Output)"
  }

  Write-Host ""
  Write-Host "Starting backend services with delay..."

  foreach ($service in $services) {
    Start-BackendService -ServiceName $service.Name -Port $service.Port
    Start-Sleep -Seconds 3
  }

  Start-Frontend

  Write-Host ""
  Write-Host "IRMS local stack is starting."
  Write-Host "Frontend:    http://localhost:5173"
  Write-Host "API Gateway: http://localhost:8080"
  Write-Host "Health:      http://localhost:8080/api/health"
  Write-Host "Logs:        $LogDir"
  Write-Host ""
  Write-Host "Supported Java versions: 17, 21, 23"
  Write-Host "Current JVM opts: $($env:IRMS_LOCAL_JVM_OPTS)"
  Write-Host ""
  Write-Host "Press Ctrl+C to stop all spawned processes."

  Watch-StartedProcesses
}
catch {
  $scriptFailed = $true

  Write-Host ""
  Write-Host "========================================" -ForegroundColor Red
  Write-Host "RUN-LOCAL FAILED" -ForegroundColor Red
  Write-Host "========================================" -ForegroundColor Red
  Write-Host ""

  Write-Host "Error:" -ForegroundColor Yellow
  Write-Host $_.Exception.Message
  Write-Host ""

  if ($_.ScriptStackTrace) {
    Write-Host "Stack trace:" -ForegroundColor Yellow
    Write-Host $_.ScriptStackTrace
    Write-Host ""
  }

  Write-Host "Logs folder:" -ForegroundColor Yellow
  Write-Host $LogDir
  Write-Host ""

  if (Test-Path $LogDir) {
    Write-Host "Recent log files:" -ForegroundColor Yellow

    Get-ChildItem $LogDir -File -ErrorAction SilentlyContinue |
      Sort-Object LastWriteTime -Descending |
      Select-Object -First 10 |
      ForEach-Object {
        Write-Host "- $($_.FullName)"
      }

    Write-Host ""
  }

  Read-Host "Press Enter to close"
}
finally {
  Write-Host ""
  Write-Host "Stopping spawned processes..."

  foreach ($item in $started) {
    if ($item -and $item.Process -and -not $item.Process.HasExited) {
      try {
        taskkill.exe /PID $item.Process.Id /T /F *> $null
      }
      catch {
        try {
          $item.Process.Kill()
        }
        catch {
        }
      }
    }
  }

  foreach ($writer in $writers) {
    if ($writer) {
      try {
        $writer.Flush()
        $writer.Dispose()
      }
      catch {
      }
    }
  }

  Write-Host "Cleanup complete."

  if ($scriptFailed) {
    Write-Host ""
    Write-Host "Script stopped because of an error. See message above." -ForegroundColor Red
  }
}