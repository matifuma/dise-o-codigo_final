# Capital Humano · JavaFX

Abrí **Abrir frontend2.cmd** en Windows. Usa JDK 21 o posterior y JavaFX 21.0.6. El proyecto compila los fuentes de la raíz del repositorio (o de la carpeta hermana `diseño_codigo_final_main` en la distribución del TP). Reutiliza JavaFX instalado en el equipo; si falta, lo descarga en `.javafx` dentro de esta carpeta.

En VS Code, abrí **Capital Humano.code-workspace** y presioná **Ctrl+Shift+B** para compilar y abrir esta versión.

## Cómo probar ambos roles

1. En **Modo Consultor → Evaluar candidatos**, presioná **Buscar**, marcá un candidato y elegí **Agregar**.
2. Elegí **Siguiente**, seleccioná un puesto y avanzá hasta **Candidatos a evaluar**. Copiá la clave de ocho caracteres y presioná **Finalizar**. También podés exportar la lista a Excel.
3. Volvé a la selección de modo e ingresá en **Modo Candidato** con esa clave. Respondé todas las preguntas de cada bloque. Podés salir y retomar durante la misma sesión.
4. En **Modo Consultor → Emitir orden de mérito**, buscá el código `PST-001` si usaste el puesto Analista de sistemas. Elegí las evaluaciones y emití el listado. Los cuestionarios completos que alcanzaron los mínimos aparecen ordenados por puntaje; los demás figuran agrupados por estado.

En **Gestionar preguntas** podés buscar, crear, modificar y dar de baja. Las ponderaciones de respuesta deben sumar 10.

## Implementación

Los dos modos comparten los DAO en memoria. Los candidatos, puestos y preguntas de ejemplo se cargan al iniciar. Los cambios y las claves duran hasta cerrar la aplicación.

CU020, CU021, CU023 y CU025 utilizan los gestores del proyecto final. Los límites de nombre, enunciado y descripción son 50, 250 y 500 caracteres respectivamente, según la validación del gestor. CU033 muestra el formulario y permite seguir con el alta manual cuando el proveedor de IA está desconectado.

Para CU026, `GestorCuestionariosSesion` extiende el gestor existente y `Progreso` extiende el cuestionario para guardar en memoria las preguntas, respuestas, accesos y estado. Para CU028, `GestorReportes` clasifica esos mismos cuestionarios por puesto y evaluaciones seleccionadas.

## Verificación

Desde esta carpeta, podés ejecutar la prueba funcional, que usa una sesión independiente:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\iniciar.ps1 -Verificar
```

