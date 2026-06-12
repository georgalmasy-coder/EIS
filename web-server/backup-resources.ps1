# BackupResources.ps1

$sourceFolder1 = "C:\udv\web-server\src\main\resources"
$sourceFolder2 = "C:\udv\web-server\src\main\webapp"
$backupRoot = "C:\udv\backup"

# Lav timestamp til mappenavn, fx: resources_backup_2026-06-05_14-30-25
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$backupFolder = Join-Path $backupRoot "backup_$timestamp"

# Sikr at backup root findes
if (-not (Test-Path $backupRoot)) {
    New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
}

# Tjek at source folder findes
if (-not (Test-Path $sourceFolder1)) {
    Write-Error "Source folder findes ikke: $sourceFolder1"
    exit 1
}

# Tjek at source folder findes
if (-not (Test-Path $sourceFolder2)) {
    Write-Error "Source folder findes ikke: $sourceFolder2"
    exit 1
}

# Opret ny backup folder
New-Item -ItemType Directory -Path $backupFolder -Force | Out-Null

# Kopier resources-folderen ind i backup-folderen
Copy-Item -Path $sourceFolder1 -Destination $backupFolder -Recurse -Force

# Kopier resources-folderen ind i backup-folderen
Copy-Item -Path $sourceFolder2 -Destination $backupFolder -Recurse -Force

Write-Host "Backup oprettet:"
Write-Host $backupFolder