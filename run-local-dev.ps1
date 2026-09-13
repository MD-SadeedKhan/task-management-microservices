param(
    [switch]$InfraOnly,
    [switch]$AppsOnly
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host "Loading local development environment from .env"
Get-Content "$root/.env" | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
    $name, $value = $_ -split '=', 2
    if (-not $name) { return }
    Set-Item -Path "Env:$name" -Value $value.Trim('"')
}

if (-not $AppsOnly) {
    Write-Host "Starting PostgreSQL containers..."
    docker compose up -d user-postgres task-postgres

    Write-Host "Starting nginx container for frontend..."
    docker rm -f task-nginx 2>$null | Out-Null
    docker run --rm -d `
        --name task-nginx `
        -p 3000:80 `
        -e GATEWAY_HOST=host.docker.internal `
        -e GATEWAY_PORT=8080 `
        -v "$root/frontend/src:/usr/share/nginx/html:ro" `
        nginx:1.27-alpine
}

if (-not $InfraOnly) {
    Write-Host "Starting user-service..."
    Start-Process powershell -ArgumentList @(
        '-NoExit',
        '-Command',
        "Set-Location '$root/user-service'; `$env:DB_HOST = '$env:USER_DB_HOST'; `$env:DB_PORT = '$env:USER_DB_PORT'; `$env:DB_NAME = '$env:USER_DB_NAME'; `$env:DB_USERNAME = '$env:USER_DB_USERNAME'; `$env:DB_PASSWORD = '$env:USER_DB_PASSWORD'; .\mvnw spring-boot:run"
    )

    Write-Host "Starting task-service..."
    Start-Process powershell -ArgumentList @(
        '-NoExit',
        '-Command',
        "Set-Location '$root/task-service'; `$env:DB_HOST = '$env:TASK_DB_HOST'; `$env:DB_PORT = '$env:TASK_DB_PORT'; `$env:DB_NAME = '$env:TASK_DB_NAME'; `$env:DB_USERNAME = '$env:TASK_DB_USERNAME'; `$env:DB_PASSWORD = '$env:TASK_DB_PASSWORD'; .\mvnw spring-boot:run"
    )

    Write-Host "Starting gateway..."
    Start-Process powershell -ArgumentList @(
        '-NoExit',
        '-Command',
        "Set-Location '$root/gateway'; `$env:USER_SERVICE_HOST = '$env:USER_SERVICE_HOST'; `$env:USER_SERVICE_PORT = '$env:USER_SERVICE_PORT'; `$env:TASK_SERVICE_HOST = '$env:TASK_SERVICE_HOST'; `$env:TASK_SERVICE_PORT = '$env:TASK_SERVICE_PORT'; .\mvnw spring-boot:run"
    )
}

Write-Host ""
Write-Host "Ready"
Write-Host "Frontend: http://localhost:3000"
Write-Host "Gateway:  http://localhost:8080"
Write-Host "User DB:  localhost:5433"
Write-Host "Task DB:  localhost:5434"
