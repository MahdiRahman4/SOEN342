# build-run.ps1 — one-shot build + run for Task Manager (no Maven needed)
# Usage:  cd Iteration3 ; .\build-run.ps1
# The script downloads the two dependency JARs once, compiles all sources,
# and launches the application.

$ErrorActionPreference = "Stop"

$root    = $PSScriptRoot
$lib     = "$root\lib"
$classes = "$root\target\app-classes"
$javac   = "C:\Program Files\Java\jdk-22\bin\javac.exe"
$java    = "C:\Program Files\Java\jdk-22\bin\java.exe"
$src     = "$root\src\main\java"

# ── 1. Create directories ───────────────────────────────────────────────────
New-Item -ItemType Directory -Force -Path $lib     | Out-Null
New-Item -ItemType Directory -Force -Path $classes | Out-Null

# ── 2. Download dependencies (skipped if already present) ───────────────────
$deps = @{
    "ical4j-3.2.18.jar"       = "https://repo1.maven.org/maven2/org/mnode/ical4j/ical4j/3.2.18/ical4j-3.2.18.jar"
    "sqlite-jdbc-3.45.3.0.jar"= "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.3.0/sqlite-jdbc-3.45.3.0.jar"
    "slf4j-api-1.7.36.jar"    = "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
    "slf4j-simple-1.7.36.jar" = "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar"
    "commons-lang3-3.12.0.jar"= "https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar"
    "commons-codec-1.15.jar"  = "https://repo1.maven.org/maven2/commons-codec/commons-codec/1.15/commons-codec-1.15.jar"
}

foreach ($file in $deps.Keys) {
    $path = "$lib\$file"
    if (-not (Test-Path $path)) {
        Write-Host "Downloading $file ..."
        Invoke-WebRequest -Uri $deps[$file] -OutFile $path -UseBasicParsing
    }
}

# ── 3. Build classpath string ────────────────────────────────────────────────
$cp = ($deps.Keys | ForEach-Object { "$lib\$_" }) -join ";"

# ── 4. Compile ───────────────────────────────────────────────────────────────
Write-Host "Compiling..."
$sources = Get-ChildItem -Recurse "$src" -Filter "*.java" | Select-Object -ExpandProperty FullName
& $javac --release 17 -cp $cp -d $classes @sources
if ($LASTEXITCODE -ne 0) { Write-Error "Compilation failed."; exit 1 }
Write-Host "Compilation successful."

# ── 5. Run ───────────────────────────────────────────────────────────────────
Write-Host ""
& $java -cp "$classes;$cp" taskmanager.app.Main
