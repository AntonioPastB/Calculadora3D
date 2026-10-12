<#
  Genera la version distribuible de la Calculadora de impresion 3D para Windows.

  Uso:
    powershell -ExecutionPolicy Bypass -File empaquetar.ps1 [-Version 1.0] [-Jdk <carpeta del JDK>]

  Resultado:
    dist\Calculadora3D\                      carpeta lista para usar (Calculadora3D.bat)
    dist\Calculadora3D-<version>-Windows.zip  la misma carpeta comprimida, para repartir

  El paquete incluye su propio Java (copiado del JDK usado para compilar),
  asi que los usuarios no necesitan instalar nada.
#>
param(
    [string]$Version = "1.0",
    [string]$Jdk = ""
)

$ErrorActionPreference = "Stop"
$proyecto = $PSScriptRoot

function Paso($texto) { Write-Host "==> $texto" -ForegroundColor Cyan }

function Comprobar($accion) {
    if ($LASTEXITCODE -ne 0) { throw "Ha fallado: $accion (codigo $LASTEXITCODE)" }
}

function Buscar-Jdk {
    if ($Jdk) { return $Jdk }
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) { return $env:JAVA_HOME }
    # JDK que incluye Eclipse (carpeta "eclipse" junto al workspace o en el escritorio)
    $candidatos = @("$proyecto\..\..\eclipse\plugins", "$env:USERPROFILE\Desktop\eclipse\plugins",
                    "$env:USERPROFILE\eclipse\plugins")
    foreach ($c in $candidatos) {
        if (Test-Path $c) {
            $jre = Get-ChildItem $c -Directory -Filter "org.eclipse.justj.openjdk.hotspot.jre.full.win32*" |
                   Sort-Object Name -Descending | Select-Object -First 1
            if ($jre -and (Test-Path "$($jre.FullName)\jre\bin\javac.exe")) { return "$($jre.FullName)\jre" }
        }
    }
    throw "No se encuentra un JDK. Indica su carpeta con -Jdk o define la variable JAVA_HOME."
}

$jdkDir = (Resolve-Path (Buscar-Jdk)).Path
$bin    = "$jdkDir\bin"
$build  = "$proyecto\build"
$dist   = "$proyecto\dist"
$app    = "$dist\Calculadora3D"
$zip    = "$dist\Calculadora3D-$Version-Windows.zip"
Write-Host "JDK: $jdkDir"

# Si la calculadora esta abierta desde dist, Windows no deja reemplazar sus archivos.
$abierta = Get-Process javaw -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -and $_.Path.StartsWith($app, [StringComparison]::OrdinalIgnoreCase) }
if ($abierta) {
    throw "La calculadora esta abierta desde $app. Cierrala y vuelve a ejecutar este script."
}

Paso "Limpiando compilaciones anteriores"
foreach ($d in @($build, $app, $zip)) {
    if (Test-Path $d) { Remove-Item -Recurse -Force $d }
}
New-Item -ItemType Directory -Force "$build\clases", "$build\herramientas", "$app\app" | Out-Null

Paso "Compilando el programa"
# Codigo comun (nucleo) mas el de la app de escritorio. Se compila para Java 17,
# la misma version que usa la app de Android.
$fuentes = Get-ChildItem "$proyecto\nucleo\src\main\java", "$proyecto\escritorio\src\main\java" -Recurse -Filter *.java |
           ForEach-Object { $_.FullName }
& "$bin\javac.exe" -encoding UTF-8 -nowarn --release 17 -d "$build\clases" $fuentes
Comprobar "compilar"

Paso "Creando Calculadora3D.jar"
& "$bin\jar.exe" --create --file "$app\app\Calculadora3D.jar" --main-class com.impresion3d.Main -C "$build\clases" .
Comprobar "crear el JAR"

Paso "Generando el icono"
& "$bin\javac.exe" -encoding UTF-8 -nowarn -cp "$build\clases" -d "$build\herramientas" "$proyecto\empaquetado\GeneradorIcono.java"
Comprobar "compilar el generador de iconos"
& "$bin\java.exe" -cp "$build\clases;$build\herramientas" com.impresion3d.ui.GeneradorIcono "$build\icono.ico"
Comprobar "generar el icono"

Paso "Copiando lanzadores e icono"
# No se usa un .exe propio: sin firma digital, Smart App Control de Windows 11
# lo bloquea. Los .bat arrancan javaw.exe, que si esta firmado.
Copy-Item "$build\icono.ico" "$app\app\icono.ico"
foreach ($bat in Get-ChildItem "$proyecto\empaquetado" -Filter *.bat) {
    # cmd.exe necesita saltos de linea de Windows (CRLF)
    $texto = (Get-Content $bat.FullName) -join "`r`n"
    [System.IO.File]::WriteAllText("$app\$($bat.Name)", $texto + "`r`n", [System.Text.Encoding]::ASCII)
}

Paso "Copiando Java dentro del paquete"
Copy-Item -Recurse $jdkDir "$app\runtime"
# Se quita lo que solo sirve para programar: codigo fuente de Java, datos del
# compilador, cabeceras de C y herramientas de desarrollo. Tambien la cache CDS
# para memorias de mas de 32 GB, que este programa nunca usa.
$sobrantes = @("lib\src.zip", "lib\ct.sym", "lib\jvm.lib", "include", "bin\server\classes_nocoops.jsa")
foreach ($s in $sobrantes) {
    if (Test-Path "$app\runtime\$s") { Remove-Item -Recurse -Force "$app\runtime\$s" }
}
Get-ChildItem "$app\runtime\bin" -Filter *.exe |
    Where-Object { $_.Name -notin @("java.exe", "javaw.exe") } |
    Remove-Item -Force
Copy-Item "$proyecto\empaquetado\LEEME.txt" "$app\LEEME.txt"

Paso "Comprimiendo en $zip"
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($app, $zip, [System.IO.Compression.CompressionLevel]::Optimal, $true)

$mbCarpeta = [math]::Round((Get-ChildItem $app -Recurse -File | Measure-Object Length -Sum).Sum / 1MB)
$mbZip = [math]::Round((Get-Item $zip).Length / 1MB)
Write-Host ""
Write-Host "Listo." -ForegroundColor Green
Write-Host "  Carpeta: $app ($mbCarpeta MB)"
Write-Host "  ZIP:     $zip ($mbZip MB)"
