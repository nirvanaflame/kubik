' start-pf-argocd.vbs — hidden launcher for pf-argocd.ps1 (runs at logon via Startup folder)
' 0 = hidden window, False = don't wait
CreateObject("Wscript.Shell").Run "powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File ""C:\Users\nf\IdeaProjects\spring-playground\tools\pf-argocd.ps1""", 0, False
