param([switch]$Compilar,[switch]$Verificar,[switch]$Inicializar)
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
$deps = Join-Path $env:LOCALAPPDATA 'CapitalHumano/dependencias'
New-Item -ItemType Directory -Force $deps | Out-Null
$jdbc = Join-Path $deps 'postgresql-42.7.15.jar'
if (!(Test-Path $jdbc)) {
    $descarga = "$jdbc.$([guid]::NewGuid()).part"
    & curl.exe -fsSL --retry 2 'https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.15/postgresql-42.7.15.jar' -o $descarga
    if ($LASTEXITCODE -or (Get-FileHash $descarga).Hash -ne '9776FCA62DFC84F312D5C1AFF9791F6F3CF56D518A8ECA4A3A8DCA7A95EF8881') { throw 'No se pudo verificar el controlador PostgreSQL.' }
    Move-Item -LiteralPath $descarga -Destination $jdbc -Force
}
$legado = Join-Path $deps 'h2-2.3.232.jar'
if (!(Test-Path $legado) -and (Test-Path (Join-Path $proyecto 'app/h2-2.3.232.jar'))) { $legado=Join-Path $proyecto 'app/h2-2.3.232.jar' }
$build = Join-Path $PSScriptRoot 'build'
New-Item -Type Directory -Force $build | Out-Null
$fuentes = @(Get-ChildItem -LiteralPath $dominio -Filter '*.java') + @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java')
& javac --release 21 -encoding UTF-8 --module-path $lib --add-modules javafx.controls -d $build $fuentes.FullName
if ($LASTEXITCODE) { exit $LASTEXITCODE }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'pantallas.css') -Destination $build
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'esquema.sql'),(Join-Path $PSScriptRoot 'inicial.sql') -Destination $build
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'postgres-local.ps1') -Destination $build
$cp="$build;$jdbc;$legado"
if ($Compilar) { exit 0 }
if ($Inicializar) { & java -cp $cp Postgres inicializar; exit $LASTEXITCODE }
if (!$Verificar) {
    & java --module-path $lib --add-modules javafx.controls -cp $cp Aplicacion
    exit $LASTEXITCODE
}
foreach ($modo in 'interfaz','persistencia') {
    $env:CAPITAL_TEST_SCHEMA='prueba_'+[guid]::NewGuid().ToString('N')
    & java -cp $cp Postgres crear-prueba
    if ($LASTEXITCODE) { exit $LASTEXITCODE }
    $control = Join-Path ([IO.Path]::GetTempPath()) $env:CAPITAL_TEST_SCHEMA
    try {
        if ($modo -eq 'interfaz') {
            & java --module-path $lib --add-modules javafx.controls -cp $cp Aplicacion --verificar
            if ($LASTEXITCODE) { throw 'Fallo la prueba JavaFX.' }
        } else {
            foreach ($fase in 'crear','retomar','reporte') {
                & java -cp $cp VerificacionPersistencia $control $fase
                if ($LASTEXITCODE) { throw "Fallo la prueba de persistencia: $fase" }
            }
        }
    } finally {
        & java -cp $cp Postgres limpiar-prueba
        if (Test-Path "$control.control") { Remove-Item -LiteralPath "$control.control" }
        Remove-Item Env:CAPITAL_TEST_SCHEMA
    }
}
Write-Host 'PostgreSQL 18: interfaz y persistencia verificadas.'
