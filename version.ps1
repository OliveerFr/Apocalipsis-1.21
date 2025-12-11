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

# Compilar para verificar
Write-Info "Compilando proyecto..."
$mvnResult = & mvn compile -q 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Error de compilacion. Revirtiendo cambios..."
    $pomContent | Set-Content $pomPath -Encoding UTF8
    Write-Host $mvnResult
    exit 1
}
Write-Success "Compilacion exitosa"

# Crear JAR
Write-Info "Creando JAR..."
& mvn package -q -DskipTests 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Error "Error al crear JAR"
} else {
    Write-Success "JAR creado: target/Apocalipsis-$newVersion.jar"
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
