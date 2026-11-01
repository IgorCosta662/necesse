# Script de Build e Instalacao do Mod Item Spawner para Necesse

$ErrorActionPreference = "Stop"

$javac = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\javac.exe"
$jarTool = "C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot\bin\jar.exe"
$necesseJar = "C:\Program Files (x86)\Steam\steamapps\common\Necesse\Necesse.jar"
$libDir = "C:\Program Files (x86)\Steam\steamapps\common\Necesse\lib\*"
$classpath = "$necesseJar;$libDir"

Write-Host "Compilando arquivos Java do mod..." -ForegroundColor Cyan

if (Test-Path "build") { Remove-Item -Recurse -Force "build" }
New-Item -ItemType Directory -Force -Path "build\classes" | Out-Null

$sources = (Get-ChildItem -Recurse "src\*.java").FullName

& $javac -encoding UTF-8 -cp $classpath -d "build\classes" $sources
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erro na compilacao!" -ForegroundColor Red
    exit 1
}

Write-Host "Empacotando JAR..." -ForegroundColor Cyan
Copy-Item "resources\mod.info" -Destination "build\classes\mod.info"
New-Item -ItemType Directory -Force -Path "build\classes\itemspawner" | Out-Null
Copy-Item "resources\itemspawner\icon.png" -Destination "build\classes\itemspawner\icon.png"
Copy-Item "preview.png" -Destination "build\classes\preview.png"

& $jarTool -cf "Item Spawner Menu.jar" -C "build\classes" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erro ao gerar arquivo JAR!" -ForegroundColor Red
    exit 1
}

Write-Host "Instalando mod na pasta do Necesse..." -ForegroundColor Cyan
$destMods = "C:\Users\medei\AppData\Roaming\Necesse\mods"
if (!(Test-Path $destMods)) {
    New-Item -ItemType Directory -Force -Path $destMods | Out-Null
}
Copy-Item "Item Spawner Menu.jar" -Destination "$destMods\Item Spawner Menu.jar" -Force

Write-Host "`nMod 'Item Spawner Menu' compilado e instalado com sucesso!" -ForegroundColor Green
Write-Host "Basta abrir o Necesse (ou reiniciar se estiver aberto) e ativar o mod no menu de Mods!" -ForegroundColor Yellow

