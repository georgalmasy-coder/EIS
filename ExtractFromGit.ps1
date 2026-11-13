Set-Location C:\udv\EIS

$commit = git rev-list -1 --before="2026-10-31T23:59:59+01:00" HEAD

if ($commit) {
    git archive --format=zip --output="C:\udv\EIS\source-2026-10-31.zip" $commit
    if ($LASTEXITCODE -eq 0) {
        Write-Host "ZIP oprettet: C:\udv\EIS\source-2026-10-31.zip"
    }
} else {
    Write-Host "Ingen commits fundet før den valgte dato."
}