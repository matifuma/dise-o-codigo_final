$ErrorActionPreference = 'Stop'
$raiz = Join-Path $env:LOCALAPPDATA 'CapitalHumano'
$motor = Join-Path $raiz 'pg18'
$privado = Join-Path $raiz 'postgres-local'
$datos = Join-Path $privado 'cluster'
$mutex = New-Object Threading.Mutex($false, 'Local\CapitalHumanoPostgres18')
try {
    try { $adquirido = $mutex.WaitOne(120000) } catch [Threading.AbandonedMutexException] { $adquirido=$true }
    if (!$adquirido) { throw 'Otro proceso esta configurando PostgreSQL.' }
    New-Item -ItemType Directory -Force $motor,$privado | Out-Null
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    & icacls.exe $privado /inheritance:r /grant:r ("*"+$sid+":(OI)(CI)F") | Out-Null
    if ($LASTEXITCODE) { throw 'No se pudo proteger el directorio de credenciales.' }
    if (!(Test-Path "$motor/bin/postgres.exe")) {
        $url='https://repo.maven.apache.org/maven2/io/zonky/test/postgres/embedded-postgres-binaries-windows-amd64/18.7.0/embedded-postgres-binaries-windows-amd64-18.7.0.jar'
        & curl.exe -fsSL --retry 2 $url -o "$motor/binarios.jar"
        if ($LASTEXITCODE -or (Get-FileHash "$motor/binarios.jar").Hash -ne '789D0261833F6C1FC9EA9E0A882BA5EF5D49366144516A44ED6FD7461D08C3F8') { throw 'Descarga de PostgreSQL no verificada.' }
        & tar.exe -xf "$motor/binarios.jar" -C $motor postgres-windows-x86_64.txz
        if ($LASTEXITCODE) { throw 'No se pudo extraer el paquete.' }
        & tar.exe -xf "$motor/postgres-windows-x86_64.txz" -C $motor
        if ($LASTEXITCODE) { throw 'No se pudieron extraer los binarios.' }
    }
    foreach ($archivo in 'admin.key','app.key') {
        $clave=Join-Path $privado $archivo
        if (!(Test-Path $clave)) {
            $bytes = New-Object byte[] 32
            $rng=[Security.Cryptography.RandomNumberGenerator]::Create()
            try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
            [IO.File]::WriteAllText($clave,([BitConverter]::ToString($bytes)).Replace('-',''))
        }
    }
    if (!(Test-Path "$datos/PG_VERSION")) {
        & "$motor/bin/initdb.exe" -D $datos -U capital_admin --pwfile="$privado/admin.key" --auth=scram-sha-256 --encoding=UTF8 --locale=C
        if ($LASTEXITCODE) { throw 'No se pudo inicializar el cluster PostgreSQL.' }
    }
    & "$motor/bin/pg_ctl.exe" -D $datos status *> $null
    if ($LASTEXITCODE -ne 0) {
        & "$motor/bin/pg_ctl.exe" -D $datos -l "$privado/servidor.log" -o "-h 127.0.0.1 -p 55432" -w start
        if ($LASTEXITCODE) { throw "No se pudo iniciar PostgreSQL. Consulte $privado/servidor.log" }
    }
} finally {
    if ($adquirido) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
