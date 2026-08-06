# Run Claude Code powered by Kimi (Moonshot AI)
# Usage:
#   .\kimi.ps1                          (Default: kimi-k3 for reasoning, kimi-k2.7-code-highspeed for fast sub-tasks)
#   .\kimi.ps1 -Fast                    (High-speed mode using kimi-k2.7-code-highspeed)
#   .\kimi.ps1 -Opus                    (Heavy reasoning mode using kimi-k3)
#   .\kimi.ps1 -Model "custom-model"    (Specify custom model)
#   .\kimi.ps1 "explain this project"   (Pass prompt or args directly)

[CmdletBinding()]
param(
    [string]$Model = "kimi-k3",
    [switch]$Fast,
    [switch]$Opus,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ClaudeArgs
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$key = $env:MOONSHOT_API_KEY
if (-not $key -and (Test-Path "$PSScriptRoot\.kimi-key")) {
    $key = (Get-Content "$PSScriptRoot\.kimi-key" -Raw).Trim()
}
if (-not $key) {
    Write-Error "Moonshot API Key not found in .kimi-key or MOONSHOT_API_KEY environment variable"
}

# Clear conflicts
Remove-Item Env:\ANTHROPIC_AUTH_TOKEN -ErrorAction SilentlyContinue
$env:ANTHROPIC_API_KEY = $key

# Task-Specific Model Tier Mapping
if ($Fast) {
    # High-speed mode for all task tiers
    $primaryModel = "kimi-k2.7-code-highspeed"
    $env:ANTHROPIC_DEFAULT_OPUS_MODEL   = "kimi-k2.7-code-highspeed"
    $env:ANTHROPIC_DEFAULT_SONNET_MODEL = "kimi-k2.7-code-highspeed"
    $env:ANTHROPIC_DEFAULT_HAIKU_MODEL  = "kimi-k2.7-code-highspeed"
} elseif ($Opus) {
    # High-reasoning mode for all task tiers
    $primaryModel = "kimi-k3"
    $env:ANTHROPIC_DEFAULT_OPUS_MODEL   = "kimi-k3"
    $env:ANTHROPIC_DEFAULT_SONNET_MODEL = "kimi-k3"
    $env:ANTHROPIC_DEFAULT_HAIKU_MODEL  = "kimi-k3"
} else {
    # Tiered task mapping: kimi-k3 for heavy/coding tasks, kimi-k2.7-code-highspeed for fast sub-tasks
    $primaryModel = $Model
    $env:ANTHROPIC_DEFAULT_OPUS_MODEL   = "kimi-k3"
    $env:ANTHROPIC_DEFAULT_SONNET_MODEL = "kimi-k3"
    $env:ANTHROPIC_DEFAULT_HAIKU_MODEL  = "kimi-k2.7-code-highspeed"
}

$env:ANTHROPIC_MODEL = $primaryModel

Write-Host "Running Kimi Proxy -> Model: $primaryModel [Opus: $env:ANTHROPIC_DEFAULT_OPUS_MODEL | Sonnet: $env:ANTHROPIC_DEFAULT_SONNET_MODEL | Haiku: $env:ANTHROPIC_DEFAULT_HAIKU_MODEL]" -ForegroundColor Cyan

claude --model $primaryModel @ClaudeArgs


