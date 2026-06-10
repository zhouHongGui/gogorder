$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$pathValue = [Environment]::GetEnvironmentVariable('Path', 'Process')
[Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
[Environment]::SetEnvironmentVariable('Path', $null, 'Process')
[Environment]::SetEnvironmentVariable('Path', $pathValue, 'Process')

$requiredVariables = @(
    'DB_HOST',
    'DB_PORT',
    'DB_NAME',
    'DB_USERNAME',
    'DB_PASSWORD',
    'REDIS_HOST',
    'REDIS_PORT',
    'REDIS_DATABASE'
)

foreach ($name in $requiredVariables)
{
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value))
    {
        $value = [Environment]::GetEnvironmentVariable($name, 'User')
    }
    if ([string]::IsNullOrWhiteSpace($value))
    {
        $value = [Environment]::GetEnvironmentVariable($name, 'Machine')
    }
    if ([string]::IsNullOrWhiteSpace($value))
    {
        throw "Required environment variable is not configured: $name"
    }
    Set-Item -Path "Env:$name" -Value $value
}

foreach ($name in @('REDIS_PASSWORD', 'REDIS_TIMEOUT', 'TOKEN_SECRET', 'LOG_PATH', 'UPLOAD_PATH', 'AMAP_WEB_KEY', 'AMAP_SECURITY_CODE'))
{
    $value = [Environment]::GetEnvironmentVariable($name, 'Process')
    if ([string]::IsNullOrWhiteSpace($value))
    {
        $value = [Environment]::GetEnvironmentVariable($name, 'User')
    }
    if (![string]::IsNullOrWhiteSpace($value))
    {
        Set-Item -Path "Env:$name" -Value $value
    }
}

$redisExecutable = 'D:\Redis\redis-server.exe'
$backendJar = Join-Path $root 'RuoYi-Vue\ruoyi-admin\target\ruoyi-admin.jar'
$frontendDirectory = Join-Path $root 'RuoYi-Vue3'
$logDirectory = Join-Path $root '.run-logs'
$backendOutputLog = Join-Path $logDirectory 'backend.out.log'
$backendErrorLog = Join-Path $logDirectory 'backend.err.log'

New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null

$redis = Start-Process -FilePath $redisExecutable -WorkingDirectory (Split-Path $redisExecutable) -WindowStyle Hidden -PassThru
$backend = Start-Process -FilePath 'java.exe' -ArgumentList '-jar', $backendJar -WorkingDirectory (Join-Path $root 'RuoYi-Vue') -WindowStyle Hidden -RedirectStandardOutput $backendOutputLog -RedirectStandardError $backendErrorLog -PassThru
$frontend = Start-Process -FilePath 'npm.cmd' -ArgumentList 'run', 'dev', '--', '--no-open' -WorkingDirectory $frontendDirectory -WindowStyle Hidden -PassThru

[PSCustomObject]@{
    RedisPid = $redis.Id
    BackendPid = $backend.Id
    FrontendPid = $frontend.Id
}
