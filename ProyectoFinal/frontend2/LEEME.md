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

Si existe la base anterior en `%LOCALAPPDATA%\CapitalHumano\datos\evaluaciones.mv.db`, cerrá el frontend anterior antes del primer arranque. La aplicación importa los datos en una transacción y conserva el archivo original. En una instalación nueva carga los ejemplos de `inicial.sql`. El esquema relacional está en `esquema.sql`. Los cuestionarios conservan una copia ordenada de sus preguntas y alternativas. Las respuestas y el estado se guardan durante el cuestionario. Al completar un cuestionario, PostgreSQL registra el resultado global, el resultado por competencia y el detalle por factor. El orden de mérito usa esos puntajes al reabrir la aplicación.

Para respaldar la base local, ejecutá con las herramientas de cliente de PostgreSQL 18 y elegí una carpeta de destino privada:

```powershell
pg_dump -h 127.0.0.1 -p 55432 -U capital_app -W -d capital_humano -Fc -f capital_humano.dump
```

Para usar un servidor PostgreSQL 18 propio, creá una base y un usuario con permiso para crear las tablas del esquema. Antes de abrir el frontend, configurá `CAPITAL_DB_URL` (por ejemplo, `jdbc:postgresql://servidor:5432/capital_humano`), `CAPITAL_DB_USER` y `CAPITAL_DB_PASSWORD` en el entorno del proceso. Si necesitás importar H2 a ese servidor, indicá además la ruta absoluta del archivo en `CAPITAL_H2_ORIGEN` antes de la primera inicialización. La aplicación mantiene parte del estado en memoria y admite una instancia activa por esquema.

CU020, CU021, CU023 y CU025 utilizan los gestores del proyecto final. Los límites de nombre, enunciado y descripción son 50, 250 y 500 caracteres respectivamente, según la validación del gestor. CU033 muestra el formulario y permite seguir con el alta manual cuando el proveedor de IA está desconectado.

Para CU026, `GestorCuestionariosSesion` extiende el gestor existente y `Progreso` extiende el cuestionario para guardar en SQL las preguntas, respuestas, accesos y estado. Para CU028, `GestorReportes` clasifica esos mismos cuestionarios por puesto y evaluaciones seleccionadas.

## Correspondencia con el DER de la entrega 4

El esquema de versión 2 conserva los nombres del DER entre comillas, incluida la columna `"Puesto"."descripción"`. Las relaciones uno a muchos se implementan mediante claves foráneas nombradas como los rombos. `requiere` e `incluye` guardan respectivamente `"puntajeMinimo"` y `"ordenEnBloque"`. Para consultar el modelo en pgAdmin, por ejemplo:

```sql
SELECT e."idEvaluacion", q."idCuestionario", c.apellido, r."puntajeObtenido", r.cumplimiento
FROM "Evaluacion" e
JOIN "Cuestionario" q USING ("idEvaluacion")
JOIN "Candidato" c USING ("nroCandidato")
LEFT JOIN "Resultado" r USING ("idCuestionario");
```

- **CU020–023.** `"PlantillaOpcion"` conserva las opciones reutilizables del catálogo. Cada `"OpcionRespuesta"` pertenece a una pregunta y mantiene su ponderación.
- **CU025–026.** `incluye` vincula el bloque y la pregunta. `"AlternativaEmitida"` conserva el texto y puntaje vistos por el candidato aunque el catálogo se edite después. Los límites de tiempo y accesos provienen de `"ConfiguracionSistema"`.
- **CU028.** El DER introduce `"ResultadoCompetencia"` entre el resultado del cuestionario y el detalle por factor. La capa Java recupera estas filas como los objetos `Resultado` por competencia que recibe el gestor de reportes del proyecto. El puntaje del reporte es el promedio de las competencias. Para entrar al orden de mérito se debe alcanzar cada mínimo.
- **Migración.** La versión 1 se transforma dentro de una transacción. Los tiempos históricos se conservan en milisegundos desde Unix. Las fechas de respuesta o de confirmación que la versión anterior no registró quedan nulas. Para las preguntas históricas se recupera la referencia cuando hay una única coincidencia por factor y enunciado. La cuenta lógica `CONSULTOR` corresponde al único modo consultor de la interfaz; su contraseña queda nula. El atributo `"Evaluacion".nombre` queda disponible para una futura entrada de nombre: el formulario actual identifica las evaluaciones por ID y fecha.

## Verificación

Desde esta carpeta, podés ejecutar la prueba funcional, que usa una sesión independiente:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\iniciar.ps1 -Verificar
```
