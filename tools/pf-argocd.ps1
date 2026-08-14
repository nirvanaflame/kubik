# tools/pf-argocd.ps1
# ------------------------------------------------------------------
# Persistent port-forward: argocd-server (k8s 443) -> localhost:8090.
# Same pattern as pf-istio.ps1; use 8090 so it doesn't clash with the
# istio gateway tunnel on 8080.
#
#   Test:      powershell -NoProfile -File .\tools\pf-argocd.ps1
#   Register:  schtasks /Create /TN argocd-pf /TR 'powershell.exe
#              -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass
#              -File "C:\Users\nf\IdeaProjects\spring-playground\tools\pf-argocd.ps1"'
#              /SC ONLOGON /F
#   Start:     schtasks /Run /TN argocd-pf
#   Stop:      schtasks /End /TN argocd-pf
#   Delete:    schtasks /Delete /TN argocd-pf /F
#
# Log: %TEMP%\pf-argocd.log
# UI:  https://localhost:8090
# ------------------------------------------------------------------

$ErrorActionPreference = 'Continue'

$kubectl = "$env:ProgramFiles\Docker\Docker\resources\bin\kubectl.exe"
if (-not (Test-Path $kubectl)) { $kubectl = "kubectl" }

$log = Join-Path $env:TEMP "pf-argocd.log"

while ($true) {
    "=== $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') starting port-forward ===" | Add-Content $log
    & $kubectl port-forward -n argocd svc/argocd-server 8090:443 2>&1 |
        Out-File -Append $log
    Start-Sleep -Seconds 5
}
