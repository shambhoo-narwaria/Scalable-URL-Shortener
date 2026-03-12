Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   STOPPING URL SHORTENER SERVICES       " -ForegroundColor Cyan
Write-Host "=========================================`n" -ForegroundColor Cyan

# 1. Stop Spring Boot application running on port 8080
Write-Host "1. Finding and stopping Spring Boot server (port 8080)..." -ForegroundColor Yellow
$portConnection = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue

if ($portConnection) {
    # Get unique Process IDs, ignoring '0' (System Idle connections in TIME_WAIT)
    $processIds = $portConnection.OwningProcess | Select-Object -Unique | Where-Object { $_ -ne 0 }
    
    if ($processIds) {
        foreach ($procId in $processIds) {
            if (Get-Process -Id $procId -ErrorAction SilentlyContinue) {
                try {
                    Stop-Process -Id $procId -Force -ErrorAction Stop
                    Write-Host "Successfully stopped process (PID: $procId)`n" -ForegroundColor Green
                } catch {
                    if (Get-Process -Id $procId -ErrorAction SilentlyContinue) {
                        Write-Host "Failed to stop process (PID: $procId): $($_.Exception.Message)" -ForegroundColor Red
                        Write-Host "You might need to run this script as Administrator.`n" -ForegroundColor Red
                    }
                }
            }
        }
    } else {
        Write-Host "No active Java server running on port 8080.`n" -ForegroundColor Green
    }
} else {
    Write-Host "No server running on port 8080.`n" -ForegroundColor Green
}

# 2. Stop Docker Background Services
Write-Host "2. Stopping Database (PostgreSQL) & Cache (Redis) in Docker..." -ForegroundColor Yellow
try {
    docker-compose down
    Write-Host "Docker containers stopped successfully!`n" -ForegroundColor Green
} catch {
    Write-Host "Failed to stop Docker." -ForegroundColor Red
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "All services have been fully stopped." -ForegroundColor Cyan
