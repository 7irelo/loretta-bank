param(
  [string]$Profile = "minikube",
  [switch]$SkipBuild,
  [int]$MinikubeMemoryMB = 6144,
  [int]$MinikubeCPUs = 4
)

$ErrorActionPreference = "Stop"

function Require-Command {
  param([string]$Name)
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Required command not found: $Name"
  }
}

Require-Command "minikube"
Require-Command "kubectl"
if (-not $SkipBuild) {
  Require-Command "docker"
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

$statusJson = ""
$statusExitCode = 0
$statusJson = minikube status -p $Profile -o json 2>$null | Out-String
$statusExitCode = $LASTEXITCODE
$isRunning = $false
if ($statusExitCode -eq 0 -and $statusJson.Trim()) {
  try {
    $status = $statusJson | ConvertFrom-Json
    $isRunning = ($status.Host -eq "Running" -and $status.Kubelet -eq "Running" -and $status.APIServer -eq "Running")
  } catch {
    $isRunning = $false
  }
}

if (-not $isRunning) {
  minikube start -p $Profile --cpus=$MinikubeCPUs --memory=$MinikubeMemoryMB
}

minikube addons enable ingress -p $Profile | Out-Null

$images = @(
  @{ Name = "loretta-discovery-service:latest"; Dockerfile = "server/discovery-service/Dockerfile"; Context = "server" },
  @{ Name = "loretta-api-gateway:latest"; Dockerfile = "server/api-gateway/Dockerfile"; Context = "server" },
  @{ Name = "loretta-auth-service:latest"; Dockerfile = "server/auth-service/Dockerfile"; Context = "server" },
  @{ Name = "loretta-customer-service:latest"; Dockerfile = "server/customer-service/Dockerfile"; Context = "server" },
  @{ Name = "loretta-account-service:latest"; Dockerfile = "server/account-service/Dockerfile"; Context = "server" },
  @{ Name = "loretta-transaction-service:latest"; Dockerfile = "server/transaction-service/Dockerfile"; Context = "server" },
  @{ Name = "loretta-notification-service:latest"; Dockerfile = "server/notification-service/Dockerfile"; Context = "server" },
  @{ Name = "loretta-audit-service:latest"; Dockerfile = "server/audit-service/Dockerfile"; Context = "server" },
  @{ Name = "loretta-reporting-service:latest"; Dockerfile = "server/reporting-service/Dockerfile"; Context = "server" },
  @{ Name = "loretta-bank-client:latest"; Dockerfile = "client/Dockerfile"; Context = "client"; BuildArgs = @("--build-arg", "NEXT_PUBLIC_API_URL=") }
)

if (-not $SkipBuild) {
  foreach ($image in $images) {
    $args = @("build", "-f", $image.Dockerfile, "-t", $image.Name)
    if ($image.ContainsKey("BuildArgs")) {
      $args += $image.BuildArgs
    }
    $args += $image.Context
    docker @args
  }
}

foreach ($image in $images) {
  minikube image load $image.Name -p $Profile
}

kubectl apply -k k8s/all

$minikubeIp = (minikube ip -p $Profile).Trim()
Write-Host ""
Write-Host "Deployment submitted."
Write-Host "1) Start a tunnel in another terminal: minikube tunnel -p $Profile"
Write-Host "2) Add this hosts entry (run as admin): $minikubeIp loretta-bank.local"
Write-Host "3) Open: http://loretta-bank.local"
