$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$templateFolder = Join-Path $projectRoot 'common/src/eis/mail-templates'
$destination = Join-Path $projectRoot 'common/src/main/resources/db/mail-templates.sql'
$png = [IO.File]::ReadAllBytes((Join-Path $projectRoot 'common/src/main/resources/mail-assets/eis-logo-light.png'))
$logo = '<img src="data:image/png;base64,' + [Convert]::ToBase64String($png) + '" alt="EIS - Engineering in Systems" width="110" height="26" style="display:block;border:0;">'
function SqlLiteral([string] $value) {
    $chunks = @()
    for ($i = 0; $i -lt $value.Length; $i += 3000) {
        $chunks += "N'" + $value.Substring($i, [Math]::Min(3000, $value.Length - $i)).Replace("'", "''") + "'"
    }
    if ($chunks.Count -eq 0) { return "N''" }
    $chunks[0] = 'CAST(' + $chunks[0] + ' AS nvarchar(max))'
    return $chunks -join ' + '
}
$sql = @'
-- Generated from the original .mail files. Existing database edits are never overwritten.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @lockResult int;
EXEC @lockResult = sys.sp_getapplock @Resource = N'EIS_MAIL_TEMPLATE_INSTALL', @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 30000;
IF @lockResult < 0 THROW 51000, 'Could not lock mail template installation', 1;
IF OBJECT_ID(N'dbo.MAIL_TEMPLATE', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MAIL_TEMPLATE (
        TemplateKey nvarchar(100) NOT NULL CONSTRAINT PK_MAIL_TEMPLATE PRIMARY KEY,
        Name nvarchar(200) NOT NULL,
        Subject nvarchar(500) NOT NULL,
        BodyHtml nvarchar(max) NOT NULL,
        ContentType nvarchar(100) NOT NULL,
        SourceFileName nvarchar(200) NOT NULL,
        PlaceholdersJson nvarchar(max) NOT NULL,
        Revision int NOT NULL CONSTRAINT DF_MAIL_TEMPLATE_Revision DEFAULT 1,
        CreatedAt datetime2 NOT NULL CONSTRAINT DF_MAIL_TEMPLATE_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt datetime2 NOT NULL CONSTRAINT DF_MAIL_TEMPLATE_UpdatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedByUserId int NULL
    );
END;

'@
foreach ($file in Get-ChildItem -LiteralPath $templateFolder -Filter '*.mail' | Sort-Object Name) {
    $original = [IO.File]::ReadAllText($file.FullName)
    $subject = ''; $contentType = 'text/html; charset=UTF-8'; $body = ''
    if ($original.Length -gt 0) {
        $parts = [regex]::new('\r?\n\r?\n').Split($original, 2)
        $subject = [regex]::Match($parts[0], '(?m)^Subject:\s*(.*)').Groups[1].Value.Trim()
        $contentType = [regex]::Match($parts[0], '(?m)^Content-Type:\s*(.*)').Groups[1].Value.Trim()
        if ($parts.Count -ne 2 -or !$subject -or $contentType -notlike 'text/html*') { throw "Unsupported template $($file.Name)" }
        $body = $parts[1]
    }
    $heading = [regex]::Match($body, '(?is)<h1\b[^>]*>(.*?)</h1>').Groups[1].Value
    $heading = [regex]::Replace($heading, '<[^>]+>', '')
    if (!$heading) { $heading = ($file.BaseName -replace '-', ' ') }
    if ($file.BaseName -eq 'user-created') { $heading = 'Welcome to BEPA EIS' }
    # Remove only the legacy blue heading row; keep the message and other rows intact.
    $body = [regex]::Replace($body, '(?is)<tr>\s*<td\b[^>]*background-color:\s*#(?:102a43|0f7b4f|9b1c1c|b7791f)[^>]*>\s*<h1\b[^>]*>.*?</h1>\s*</td>\s*</tr>', '')
    $banner = @"
<table data-eis-banner="true" role="presentation" width="640" cellspacing="0" cellpadding="0" style="width:100%;max-width:640px;margin:24px auto 0;background-color:#073f34;background-image:linear-gradient(180deg,#073f34 0%,#052f27 100%);border-radius:12px;color:#ffffff;font-family:Arial,Helvetica,sans-serif;">
<tr><td style="width:120px;padding:18px 12px;vertical-align:middle;">$logo</td><td style="padding:18px 8px;text-align:center;color:#ffffff;font-size:18px;font-weight:bold;white-space:nowrap;">$heading</td><td style="width:120px;padding:18px 12px;"></td></tr>
</table>
"@
    if ($body) {
        $bodyTag = [regex]::Match($body, '(?is)<body\b[^>]*>')
        if (!$bodyTag.Success) { throw "Missing body: $($file.Name)" }
        $body = $body.Insert($bodyTag.Index + $bodyTag.Length, "`n" + $banner + "`n")
    } else {
        $body = "<!doctype html><html><head><meta charset=`"UTF-8`"></head><body>$banner</body></html>"
    }
    $tokens = @([regex]::Matches($subject + $body, '\{\{([\w]+)\}\}') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
    $tokenJson = ConvertTo-Json -InputObject $tokens -Compress
    $key = $file.BaseName.Replace('-', '_').ToUpperInvariant()
    $name = (Get-Culture).TextInfo.ToTitleCase($file.BaseName.Replace('-', ' '))
    $values = @($key, $name, $subject, $body, $contentType, $file.Name, $tokenJson) | ForEach-Object { SqlLiteral $_ }
    $sql += "`nIF NOT EXISTS (SELECT 1 FROM dbo.MAIL_TEMPLATE WHERE TemplateKey = N'$key')`nINSERT INTO dbo.MAIL_TEMPLATE (TemplateKey, Name, Subject, BodyHtml, ContentType, SourceFileName, PlaceholdersJson) VALUES (`n" + ($values -join ",`n") + ");`n"
}
$sql += "`nCOMMIT TRANSACTION;`n"
[IO.Directory]::CreateDirectory((Split-Path $destination -Parent)) | Out-Null
[IO.File]::WriteAllText($destination, $sql, [Text.UTF8Encoding]::new($false))
Write-Output "Generated $destination"
