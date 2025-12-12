<#
.SYNOPSIS
    Script para automatizar el versionado, commit y push del plugin Apocalipsis
.DESCRIPTION
    Incrementa la versión en pom.xml, hace commit y push a GitHub
.PARAMETER Type
    Tipo de incremento: major, minor, patch (por defecto: patch)
.PARAMETER Message
    Mensaje adicional para el commit (opcional)
.EXAMPLE
    .\version.ps1 -Type patch
    .\version.ps1 -Type minor -Message "Nueva funcionalidad"
    .\version.ps1  # Por defecto incrementa patch
#>

param(
    [ValidateSet("major", "minor", "patch")]
    [string]$Type = "patch",
    [string]$Message = ""
)

# Colores para output
function Write-Success { param($msg) Write-Host "[OK] $msg" -ForegroundColor Green }
function Write-Info { param($msg) Write-Host "[INFO] $msg" -ForegroundColor Cyan }
function Write-Error { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red }

# Leer pom.xml
$pomPath = Join-Path $PSScriptRoot "pom.xml"

if (-not (Test-Path $pomPath)) {
    Write-Error "No se encontro pom.xml en: $pomPath"
    exit 1
}

Write-Info "Leyendo pom.xml..."
$pomContent = Get-Content $pomPath -Raw

# Extraer version actual (primera ocurrencia de <version>)
if ($pomContent -match '<version>(\d+)\.(\d+)\.(\d+)</version>') {
    $major = [int]$Matches[1]
    $minor = [int]$Matches[2]
    $patch = [int]$Matches[3]
    $currentVersion = "$major.$minor.$patch"
    Write-Info "Version actual: $currentVersion"
} else {
    Write-Error "No se pudo encontrar la version en pom.xml"
    exit 1
}

# Calcular nueva version
switch ($Type) {
    "major" {
        $major++
        $minor = 0
        $patch = 0
    }
    "minor" {
        $minor++
        $patch = 0
    }
    "patch" {
        $patch++
    }
}

$newVersion = "$major.$minor.$patch"
Write-Info "Nueva version: $newVersion"

# Reemplazar version en pom.xml (solo la primera ocurrencia - version del proyecto)
$pomLines = Get-Content $pomPath
$versionReplaced = $false
$newPomLines = @()

foreach ($line in $pomLines) {
    if (-not $versionReplaced -and $line -match '^\s*<version>\d+\.\d+\.\d+</version>') {
        $indent = $line -replace '<version>.*', ''
        $newPomLines += "$indent<version>$newVersion</version>"
        $versionReplaced = $true
    } else {
        $newPomLines += $line
    }
}

# Guardar pom.xml
$newPomLines | Set-Content $pomPath -Encoding UTF8
Write-Success "pom.xml actualizado a version $newVersion"

# Crear JAR (skip tests y compilacion rapida)
Write-Info "Creando JAR (compilacion rapida)..."
$mvnResult = & mvn package -q "-DskipTests" "-Dmaven.test.skip=true" "-Dmaven.javadoc.skip=true" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Error al compilar/crear JAR. Revirtiendo cambios..."
    $pomContent | Set-Content $pomPath -Encoding UTF8
    Write-Host $mvnResult
    exit 1
}
Write-Success "JAR creado: target/Apocalipsis-$newVersion.jar"

# Limpiar JARs antiguos del target/
Write-Info "Limpiando JARs antiguos de target/..."
$targetPath = Join-Path $PSScriptRoot "target"
if (Test-Path $targetPath) {
    $newJarName = "Apocalipsis-$newVersion.jar"
    $newJarShaded = "Apocalipsis-$newVersion-shaded.jar"
    $originalJar = "original-Apocalipsis-$newVersion.jar"
    
    $oldJars = Get-ChildItem -Path $targetPath -Filter "*.jar" | Where-Object {
        $_.Name -ne $newJarName -and 
        $_.Name -ne $newJarShaded -and 
        $_.Name -ne $originalJar
    }
    
    if ($oldJars.Count -gt 0) {
        foreach ($jar in $oldJars) {
            Remove-Item $jar.FullName -Force
            Write-Host "  - Eliminado: $($jar.Name)" -ForegroundColor DarkGray
        }
        Write-Success "Eliminados $($oldJars.Count) JAR(s) antiguo(s)"
    } else {
        Write-Info "No hay JARs antiguos que eliminar"
    }
}

# Git operations
Write-Info "Ejecutando operaciones de Git..."

# Verificar si estamos en un repositorio git
$gitCheck = git rev-parse --is-inside-work-tree 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "No es un repositorio Git"
    exit 1
}

# Git add
git add -A
if ($LASTEXITCODE -ne 0) {
    Write-Error "Error en git add"
    exit 1
}
Write-Success "Archivos agregados al staging"

# Obtener estadisticas de cambios
Write-Info "Analizando cambios..."
$gitDiff = git diff --cached --stat --numstat 2>&1
$gitDiffSummary = git diff --cached --shortstat 2>&1

# Contar archivos y lineas
$filesChanged = @()
$totalInsertions = 0
$totalDeletions = 0

$numstatLines = git diff --cached --numstat 2>&1
foreach ($line in $numstatLines) {
    if ($line -match '^(\d+|-)\s+(\d+|-)\s+(.+)$') {
        $insertions = if ($Matches[1] -eq '-') { 0 } else { [int]$Matches[1] }
        $deletions = if ($Matches[2] -eq '-') { 0 } else { [int]$Matches[2] }
        $fileName = $Matches[3]
        $totalInsertions += $insertions
        $totalDeletions += $deletions
        $filesChanged += [PSCustomObject]@{
            File = $fileName
            Insertions = $insertions
            Deletions = $deletions
        }
    }
}

# Mostrar resumen de cambios
if ($filesChanged.Count -gt 0) {
    Write-Host ""
    Write-Host "  ARCHIVOS MODIFICADOS:" -ForegroundColor Magenta
    Write-Host "  ---------------------" -ForegroundColor Magenta
    foreach ($file in $filesChanged) {
        $ins = if ($file.Insertions -gt 0) { "+$($file.Insertions)" } else { "" }
        $del = if ($file.Deletions -gt 0) { "-$($file.Deletions)" } else { "" }
        $stats = "$ins $del".Trim()
        Write-Host "    $($file.File)" -ForegroundColor White -NoNewline
        if ($file.Insertions -gt 0) { Write-Host " +$($file.Insertions)" -ForegroundColor Green -NoNewline }
        if ($file.Deletions -gt 0) { Write-Host " -$($file.Deletions)" -ForegroundColor Red -NoNewline }
        Write-Host ""
    }
    Write-Host ""
    Write-Host "  TOTAL: $($filesChanged.Count) archivo(s), " -ForegroundColor Cyan -NoNewline
    Write-Host "+$totalInsertions" -ForegroundColor Green -NoNewline
    Write-Host " / " -ForegroundColor Cyan -NoNewline
    Write-Host "-$totalDeletions" -ForegroundColor Red
    Write-Host ""
}

# Crear mensaje de commit
$commitMessage = "v$newVersion"
if ($Message -ne "") {
    $commitMessage = "v$newVersion - $Message"
}

# Git commit
git commit -m $commitMessage
if ($LASTEXITCODE -ne 0) {
    Write-Error "Error en git commit (puede que no haya cambios)"
} else {
    Write-Success "Commit creado: $commitMessage"
}

# Git push
Write-Info "Subiendo a GitHub..."
git push
if ($LASTEXITCODE -ne 0) {
    Write-Error "Error en git push. Verifica tu conexion o credenciales."
    exit 1
}
Write-Success "Push completado exitosamente"

# Resumen final
Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  VERSIONADO COMPLETADO" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  Version anterior: $currentVersion" -ForegroundColor White
Write-Host "  Nueva version:    $newVersion" -ForegroundColor Green
Write-Host "  Commit:           $commitMessage" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Yellow
