# Capital Humano · JavaFX

Abrí **Abrir frontend2.cmd** en Windows. Usa JDK 21 o posterior y JavaFX 21.0.6. El proyecto compila los fuentes de la raíz del repositorio o de la carpeta hermana `diseño_codigo_final_main`. Reutiliza JavaFX instalado en el equipo; si falta, lo descarga en `.javafx` dentro de esta carpeta.

En VS Code, abrí **Capital Humano.code-workspace** y presioná **Ctrl+Shift+B** para compilar y abrir esta versión.

## Cómo probar ambos roles

1. En **Modo Consultor → Evaluar candidatos**, presioná **Buscar**, marcá un candidato y elegí **Agregar**.
2. Elegí **Siguiente**, seleccioná un puesto y avanzá hasta **Candidatos a evaluar**. Copiá la clave de ocho caracteres y presioná **Finalizar**. También podés exportar la lista a Excel.
3. Volvé a la selección de modo e ingresá en **Modo Candidato** con esa clave. Respondé todas las preguntas de cada bloque. Podés salir y retomar incluso después de cerrar y volver a abrir la aplicación.
4. En **Modo Consultor → Emitir orden de mérito**, buscá el código `PST-001` si usaste el puesto Analista de sistemas. Elegí las evaluaciones y emití el listado. Los cuestionarios completos que alcanzaron los mínimos aparecen ordenados por puntaje; los demás figuran agrupados por estado.

En **Gestionar preguntas** podés buscar, crear, modificar y dar de baja. Las ponderaciones de respuesta deben sumar 10.

## Implementación

Ambos modos guardan sus datos en PostgreSQL 18 mediante JDBC. En Windows de 64 bits, abrí **Capital Humano.exe** desde la carpeta que contiene `app` y `runtime`, o ejecutá el lanzador del código fuente. En el primer inicio local se descarga PostgreSQL 18.7 y se prepara una instancia para tu usuario de Windows. Se necesita Internet para esa descarga. Los siguientes inicios reutilizan la misma base.

La base local se llama `capital_humano` y escucha en `127.0.0.1:55432`. Para consultarla desde pgAdmin, usá esos datos, el usuario `capital_app` y la contraseña guardada en `%LOCALAPPDATA%\CapitalHumano\postgres-local\app.key`. Conservá esa clave en privado.

Si existe la base anterior en `%LOCALAPPDATA%\CapitalHumano\datos\evaluaciones.mv.db`, cerrá el frontend anterior antes del primer arranque. La aplicación importa los datos en una transacción y conserva el archivo original. En una instalación nueva carga los ejemplos de `inicial.sql`. El esquema relacional está en `esquema.sql`. Los cuestionarios conservan una copia ordenada de sus preguntas y alternativas. Las respuestas y el estado se guardan durante el cuestionario. Los reportes se calculan a partir de esos datos.

Para respaldar la base local, ejecutá con las herramientas de cliente de PostgreSQL 18 y elegí una carpeta de destino privada:

```powershell
pg_dump -h 127.0.0.1 -p 55432 -U capital_app -W -d capital_humano -Fc -f capital_humano.dump
```

Para usar un servidor PostgreSQL 18 propio, creá una base y un usuario con permiso para crear las tablas del esquema. Antes de abrir el frontend, configurá `CAPITAL_DB_URL` (por ejemplo, `jdbc:postgresql://servidor:5432/capital_humano`), `CAPITAL_DB_USER` y `CAPITAL_DB_PASSWORD` en el entorno del proceso. Si necesitás importar H2 a ese servidor, indicá además la ruta absoluta del archivo en `CAPITAL_H2_ORIGEN` antes de la primera inicialización. La aplicación mantiene parte del estado en memoria y admite una instancia activa por esquema.

CU020, CU021, CU023 y CU025 utilizan los gestores del proyecto final. Los límites de nombre, enunciado y descripción son 50, 250 y 500 caracteres respectivamente, según la validación del gestor. CU033 muestra el formulario y permite seguir con el alta manual cuando el proveedor de IA está desconectado.

Para CU026, `GestorCuestionariosSesion` extiende el gestor existente y `Progreso` extiende el cuestionario para guardar en SQL las preguntas, respuestas, accesos y estado. Para CU028, `GestorReportes` clasifica esos mismos cuestionarios por puesto y evaluaciones seleccionadas.

## Verificación

Desde esta carpeta, podés ejecutar la prueba funcional, que usa una sesión independiente:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\iniciar.ps1 -Verificar
```

