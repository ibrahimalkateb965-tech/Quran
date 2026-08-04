# Run Claude Code powered by Kimi (Moonshot AI)
# Usage: .\kimi.ps1   or   .\kimi.ps1 "explain this project"

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$key = $env:MOONSHOT_API_KEY
if (-not $key -and (Test-Path "$PSScriptRoot\.kimi-key")) {
    $key = (Get-Content "$PSScriptRoot\.kimi-key" -Raw).Trim()
}
if (-not $key) {
    Write-Error "Moonshot API Key not found in .kimi-key or MOONSHOT_API_KEY environment variable"
}

# Set Anthropic token environment variables for Kimi proxy
Remove-Item Env:\ANTHROPIC_AUTH_TOKEN -ErrorAction SilentlyContinue
$env:ANTHROPIC_API_KEY    = $key

claude @args
