Write-Host "Stopping local development infrastructure..."

docker rm -f task-nginx 2>$null | Out-Null
docker compose down -v --remove-orphans

Write-Host "Stopping local Java services if they are still running..."
Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match 'java.exe' -and $_.CommandLine -match 'spring-boot:run'
} | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force
}

Write-Host "Stopped all local dev services."
