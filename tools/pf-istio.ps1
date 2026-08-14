# tools/pf-istio.ps1
# ------------------------------------------------------------------
# Persistent port-forward: istio-ingressgateway (k8s 80) -> localhost:8080.
#
# Designed to run as a hidden background process so you don't have to
# type the kubectl port-forward command every time:
#
#   1. Test it manually:       powershell -NoProfile -File .\tools\pf-istio.ps1
#   2. Register at logon:      schtasks /Create /TN istio-pf /TR 'powershell.exe
#                              -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass
#                              -File "C:\Users\nf\IdeaProjects\spring-playground\tools\pf-istio.ps1"'
#                              /SC ONLOGON /F
#   3. Start now without logon: schtasks /Run /TN istio-pf
#      Stop:                   schtasks /End /TN istio-pf
#      Delete:                 schtasks /Delete /TN istio-pf /F
#
# Log: %TEMP%\pf-istio.log
# ------------------------------------------------------------------

$ErrorActionPreference = 'Continue'

# Hardcode kubectl path so the task doesn't depend on the user's PATH.
$kubectl = "$env:ProgramFiles\Docker\Docker\resources\bin\kubectl.exe"
if (-not (Test-Path $kubectl)) { $kubectl = "kubectl" }

$log = Join-Path $env:TEMP "pf-istio.log"

while ($true) {
    "=== $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') starting port-forward ===" | Add-Content $log
    # Blocks while the tunnel is up; returns when the cluster is unreachable
    # (e.g. Docker Desktop not started yet) or the tunnel is killed.
    & $kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80 2>&1 |
        Out-File -Append $log
    Start-Sleep -Seconds 5
}
