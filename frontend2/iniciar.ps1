param([switch]$Compilar,[switch]$Verificar)
$ErrorActionPreference = 'Stop'
$proyecto = Split-Path $PSScriptRoot
$dominio = Join-Path $proyecto ('dise' + [char]0x00F1 + 'o_codigo_final_main')
if (Test-Path (Join-Path $proyecto 'GestorCuestionarios.java')) { $dominio = $proyecto }
if (!(Test-Path (Join-Path $dominio 'GestorCuestionarios.java'))) { throw "No se encontro la implementacion del proyecto en $dominio" }
$lib = Join-Path $env:LOCALAPPDATA 'Faus/javafx-21.0.6'
$dependencias = 'base','graphics','controls'
$ausentes = @($dependencias | Where-Object { !(Test-Path "$lib/javafx-$_.jar") })
if ($ausentes.Count) {
    $lib = Join-Path $PSScriptRoot '.javafx'
    New-Item -Type Directory -Force $lib | Out-Null
    foreach ($modulo in $dependencias) {
        $jar = Join-Path $lib "javafx-$modulo.jar"
        if (!(Test-Path $jar)) {
            Invoke-WebRequest -UseBasicParsing "https://repo.maven.apache.org/maven2/org/openjfx/javafx-$modulo/21.0.6/javafx-$modulo-21.0.6-win.jar" -OutFile "$jar.part"
            Move-Item -LiteralPath "$jar.part" -Destination $jar -Force
        }
    }
}
$build = Join-Path $PSScriptRoot 'build'
New-Item -Type Directory -Force $build | Out-Null
$fuentes = @(Get-ChildItem -LiteralPath $dominio -Filter '*.java') + @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java')
& javac --release 21 -encoding UTF-8 --module-path $lib --add-modules javafx.controls -d $build $fuentes.FullName
if ($LASTEXITCODE) { exit $LASTEXITCODE }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'pantallas.css') -Destination $build
if (!$Compilar) {
    $opciones = @()
    if ($Verificar) { $opciones += '--verificar' }
    & java --module-path $lib --add-modules javafx.controls -cp $build Aplicacion @opciones
    exit $LASTEXITCODE
}
