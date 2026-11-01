# Script de Build e Instalacao Automatica do Mod Item Spawner para Necesse (v2.1.0)

$ErrorActionPreference = "Stop"

function Find-JavaTool([string]$toolName) {
    # 1. Tenta via JAVA_HOME
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\$toolName")) {
        return "$env:JAVA_HOME\bin\$toolName"
    }
    # 2. Tenta no PATH do sistema
    $cmd = Get-Command $toolName -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    # 3. Busca em diretorios comuns de instalacao do JDK 17+
    $searchDirs = @(
        "C:\Program Files\Microsoft\jdk*",
        "C:\Program Files\Eclipse Adoptium\jdk*",
        "C:\Program Files\Java\jdk*"
    )
    foreach ($pattern in $searchDirs) {
        $found = Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($found -and (Test-Path "$($found.FullName)\bin\$toolName")) {
            return "$($found.FullName)\bin\$toolName"
        }
    }
    throw "Nao foi possivel localizar '$toolName'. Instale o JDK 17+ ou configure JAVA_HOME."
}

function Find-NecesseDir() {
    $candidates = @(
        "C:\Program Files (x86)\Steam\steamapps\common\Necesse",
        "C:\Program Files\Steam\steamapps\common\Necesse",
        "D:\SteamLibrary\steamapps\common\Necesse",
        "E:\SteamLibrary\steamapps\common\Necesse"
    )
    foreach ($dir in $candidates) {
        if (Test-Path "$dir\Necesse.jar") {
            return $dir
        }
    }
    throw "Nao foi possivel localizar a pasta do Necesse automaticamente. Verifique a instalacao na Steam."
}

$javac = Find-JavaTool "javac.exe"
$jarTool = Find-JavaTool "jar.exe"
$necesseDir = Find-NecesseDir
$necesseJar = "$necesseDir\Necesse.jar"
$libDir = "$necesseDir\lib\*"
$classpath = "$necesseJar;$libDir"

Write-Host "Usando JDK: $javac" -ForegroundColor Gray
Write-Host "Usando Necesse: $necesseDir" -ForegroundColor Gray
Write-Host "Compilando arquivos Java do mod (v2.1.0)..." -ForegroundColor Cyan

if (Test-Path "build") { Remove-Item -Recurse -Force "build" }
New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null

$sources = (Get-ChildItem -Recurse "src\*.java").FullName

& $javac --release 17 -encoding UTF-8 -cp $classpath -d "build\classes" $sources
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erro na compilacao!" -ForegroundColor Red
    exit 1
}

Write-Host "Empacotando JAR..." -ForegroundColor Cyan
Copy-Item "resources\mod.info" -Destination "build\classes\mod.info"
New-Item -ItemType Directory -Force -Path "build\classes\itemspawner" | Out-Null
Copy-Item "resources\itemspawner\icon.png" -Destination "build\classes\itemspawner\icon.png"
if (Test-Path "preview.png") {
    Copy-Item "preview.png" -Destination "build\classes\preview.png"
}

& $jarTool -cf "Item Spawner Menu.jar" -C "build\classes" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erro ao gerar arquivo JAR!" -ForegroundColor Red
    exit 1
}

Write-Host "Instalando mod na pasta do Necesse..." -ForegroundColor Cyan
$destMods = Join-Path $env:APPDATA "Necesse\mods"
if (!(Test-Path $destMods)) {
    New-Item -ItemType Directory -Force -Path $destMods | Out-Null
}
Copy-Item "Item Spawner Menu.jar" -Destination "$destMods\Item Spawner Menu.jar" -Force

Write-Host "`nMod 'Item Spawner Menu v2.1.0' compilado e instalado com sucesso em: $destMods" -ForegroundColor Green
Write-Host "Basta abrir o Necesse (ou reiniciar se estiver aberto) e ativar o mod no menu de Mods!" -ForegroundColor Yellow

