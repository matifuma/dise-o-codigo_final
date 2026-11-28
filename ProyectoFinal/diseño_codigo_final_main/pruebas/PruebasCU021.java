import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pruebas ejecutables sin dependencias externas para CU021 y regresiones de CU020/CU022/CU025.
 * Ejecutar con: java PruebasCU021
 */
public final class PruebasCU021 {
    private static int ejecutadas;
    private static int fallidas;

    private PruebasCU021() { }

    public static void main(String[] args) {
        probar("búsqueda de opciones por texto, ignorando mayúsculas y ordenando", PruebasCU021::buscarOpciones);
        probar("búsqueda de opciones excluye inactivas", PruebasCU021::opcionesActivas);
        probar("búsqueda vacía de opciones devuelve todas las activas", PruebasCU021::opcionesSinFiltro);
        probar("DAO rechaza opciones iniciales sin id y ids repetidos", PruebasCU021::validarCatalogoOpciones);
        probar("búsqueda de competencias lista solo activas", PruebasCU021::buscarCompetenciasActivas);
        probar("búsqueda de factores resuelve competencia y filtra activo/nombre", PruebasCU021::buscarFactores);
        probar("búsqueda de factores requiere DAO de competencias", PruebasCU021::factorSinCompetenciaDAO);
        probar("alta correcta persiste, normaliza y conserva relaciones", PruebasCU021::altaCorrecta);
        probar("alta admite descripción opcional nula", PruebasCU021::descripcionOpcional);
        probar("alta acepta los límites máximos exactos de los tres campos", PruebasCU021::limitesMaximosAceptados);
        probar("alta rechaza nombre mayor a 50 caracteres", PruebasCU021::nombreExcedido);
        probar("alta rechaza enunciado mayor a 250 caracteres", PruebasCU021::enunciadoExcedido);
        probar("alta rechaza descripción mayor a 500 caracteres", PruebasCU021::descripcionExcedida);
        probar("alta informa todos los campos excedidos", PruebasCU021::informaTodosLosExcedidos);
        probar("alta detecta nombre exacto duplicado sin distinguir mayúsculas", PruebasCU021::nombreDuplicado);
        probar("alta no toma coincidencias parciales como duplicados", PruebasCU021::nombreParcialNoEsDuplicado);
        probar("alta permite distinto nombre con mismo enunciado según búsqueda CU021", PruebasCU021::enunciadoDuplicadoNoBloqueaAlta);
        probar("alta rechaza nombre vacío", () -> errorEnAlta(null, "Enunciado", opcionesValidas(), TipoPregunta.ESCALA));
        probar("alta rechaza nombre en blanco", () -> errorEnAlta("   ", "Enunciado", opcionesValidas(), TipoPregunta.ESCALA));
        probar("alta rechaza enunciado vacío", () -> errorEnAlta("Nombre", "", opcionesValidas(), TipoPregunta.ESCALA));
        probar("alta rechaza lista de opciones vacía", () -> errorEnAlta("Nombre", "Enunciado", List.of(), TipoPregunta.ESCALA));
        probar("alta rechaza lista de opciones nula", () -> errorEnAlta("Nombre", "Enunciado", null, TipoPregunta.ESCALA));
        probar("alta rechaza tipo de pregunta nulo", () -> errorEnAlta("Nombre", "Enunciado", opcionesValidas(), null));
        probar("alta rechaza competencia inactiva", PruebasCU021::competenciaInactiva);
        probar("alta rechaza factor inactivo", PruebasCU021::factorInactivo);
        probar("alta rechaza factor que pertenece a otra competencia", PruebasCU021::factorDeOtraCompetencia);
        probar("alta rechaza competencia/factor sin id", PruebasCU021::elementosSinId);
        probar("alta rechaza opción nula", PruebasCU021::opcionNula);
        probar("alta rechaza opción sin id", PruebasCU021::opcionSinId);
        probar("alta rechaza opción inactiva", PruebasCU021::opcionInactiva);
        probar("alta rechaza puntaje nulo", PruebasCU021::puntajeNulo);
        probar("alta rechaza puntaje negativo", PruebasCU021::puntajeNegativo);
        probar("alta rechaza suma de puntajes distinta de 10", PruebasCU021::sumaIncorrecta);
        probar("búsqueda CU020 sigue filtrando y devuelve preguntas activas", PruebasCU021::regresionBusquedaCU020);
        probar("modificación CU022 conserva id y valida duplicados", PruebasCU021::regresionModificacionCU022);
        probar("modificación CU022 también rechaza longitudes excedidas sin mutar la pregunta", PruebasCU021::regresionLongitudCU022);
        probar("CU025 exige dos preguntas activas en todos los factores activos", PruebasCU021::regresionEvaluabilidadCU025);

        System.out.println("Pruebas ejecutadas: " + ejecutadas + "; fallidas: " + fallidas + ".");
        if (fallidas > 0) throw new AssertionError("Fallaron " + fallidas + " pruebas.");
    }

    private static void buscarOpciones() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> encontradas = f.gestorOpciones.buscarOpciones("  sí ");
        igual(2, encontradas.size());
        igual(11L, encontradas.get(0).getId());
        igual(12L, encontradas.get(1).getId());
    }

    private static void opcionesActivas() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> encontradas = f.gestorOpciones.buscarOpciones("inactiva");
        igual(0, encontradas.size());
    }

    private static void opcionesSinFiltro() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> encontradas = f.gestorOpciones.buscarOpciones(null);
        igual(3, encontradas.size());
    }

    private static void validarCatalogoOpciones() {
        esperar(IllegalArgumentException.class,
                () -> new OpcionRespuestaDAO(List.of(new OpcionRespuesta(null, "Sí", 10.0, 1, true))));
        OpcionRespuesta opcion = new OpcionRespuesta(90L, "Sí", 10.0, 1, true);
        esperar(IllegalArgumentException.class, () -> new OpcionRespuestaDAO(Arrays.asList(opcion, opcion)));
    }

    private static void buscarCompetenciasActivas() {
        Fixture f = new Fixture();
        List<Competencia> resultados = f.gestorCompetencias.buscarCompetencias("datos");
        igual(1, resultados.size());
        igual(f.competencia.getId(), resultados.get(0).getId());
    }

    private static void buscarFactores() {
        Fixture f = new Fixture();
        List<Factor> encontrados = f.gestorFactores.buscarFactor("DATOS", "comunic");
        igual(1, encontrados.size());
        igual(f.factor.getId(), encontrados.get(0).getId());
        igual(0, f.gestorFactores.buscarFactor("datos", "no existe").size());
        esperar(IllegalArgumentException.class, () -> f.gestorFactores.buscarFactor(" ", "comunic"));
    }

    private static void factorSinCompetenciaDAO() {
        GestorFactores gestor = new GestorFactores(new FactorDAO());
        esperar(IllegalStateException.class, () -> gestor.buscarFactor("Datos", "Comunicación"));
    }

    private static void altaCorrecta() {
        Fixture f = new Fixture();
        f.gestorPreguntas.darDeAltaPregunta(f.competencia, f.factor, "  liderazgo ", "  ¿Lidera? ",
                " descripción ", opcionesValidas(), TipoPregunta.ESCALA);
        Pregunta guardada = f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId()).get(0);
        noNulo(guardada.getId());
        igual("LIDERAZGO", guardada.getNombre());
        igual("¿LIDERA?", guardada.getEnunciado());
        igual("DESCRIPCIÓN", guardada.getDescripcion());
        igual(TipoPregunta.ESCALA, guardada.getTipo());
        igual(f.competencia.getId(), guardada.getCompetencia().getId());
        igual(f.factor.getId(), guardada.getFactor().getId());
        igual(2, guardada.getOpciones().size());
        contiene(f.competencia.getPreguntas(), guardada);
        contiene(f.factor.getPreguntas(), guardada);
    }

    private static void descripcionOpcional() {
        Fixture f = new Fixture();
        f.gestorPreguntas.darDeAltaPregunta(f.competencia, f.factor, "Nombre", "Enunciado",
                null, opcionesValidas(), TipoPregunta.OPCION_MULTIPLE);
        igual("", f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId()).get(0).getDescripcion());
    }

    private static void limitesMaximosAceptados() {
        Fixture f = new Fixture();
        String nombre = "N".repeat(50);
        String enunciado = "E".repeat(250);
        String descripcion = "D".repeat(500);
        alta(f, nombre, enunciado, descripcion, opcionesValidas(), TipoPregunta.ESCALA);
        Pregunta guardada = f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId()).get(0);
        igual(50, guardada.getNombre().length());
        igual(250, guardada.getEnunciado().length());
        igual(500, guardada.getDescripcion().length());
    }

    private static void nombreExcedido() {
        Fixture f = new Fixture();
        String mensaje = esperarMensaje(IllegalArgumentException.class, () -> alta(f,
                "N".repeat(51), "Enunciado", opcionesValidas(), TipoPregunta.ESCALA));
        afirmar(mensaje.contains("Nombre de Pregunta") && mensaje.contains("50"),
                "El error debe identificar el nombre y su máximo de 50.");
    }

    private static void enunciadoExcedido() {
        Fixture f = new Fixture();
        String mensaje = esperarMensaje(IllegalArgumentException.class, () -> alta(f,
                "Nombre", "E".repeat(251), opcionesValidas(), TipoPregunta.ESCALA));
        afirmar(mensaje.contains("Pregunta / enunciado") && mensaje.contains("250"),
                "El error debe identificar el enunciado y su máximo de 250.");
    }

    private static void descripcionExcedida() {
        Fixture f = new Fixture();
        String mensaje = esperarMensaje(IllegalArgumentException.class, () -> f.gestorPreguntas.darDeAltaPregunta(
                f.competencia, f.factor, "Nombre", "Enunciado", "D".repeat(501),
                opcionesValidas(), TipoPregunta.ESCALA));
        afirmar(mensaje.contains("Descripción") && mensaje.contains("500"),
                "El error debe identificar la descripción y su máximo de 500.");
    }

    private static void informaTodosLosExcedidos() {
        Fixture f = new Fixture();
        String mensaje = esperarMensaje(IllegalArgumentException.class, () -> f.gestorPreguntas.darDeAltaPregunta(
                f.competencia, f.factor, "N".repeat(51), "E".repeat(251), "D".repeat(501),
                opcionesValidas(), TipoPregunta.ESCALA));
        afirmar(mensaje.contains("Nombre de Pregunta")
                        && mensaje.contains("Pregunta / enunciado")
                        && mensaje.contains("Descripción"),
                "El error debe enumerar los tres campos excedidos.");
    }

    private static void nombreDuplicado() {
        Fixture f = new Fixture();
        alta(f, "Liderazgo", "¿Puede liderar?");
        esperar(IllegalArgumentException.class, () -> alta(f, " LIDERAZGO ", "Otro enunciado"));
        igual(1, f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId()).size());
    }

    private static void nombreParcialNoEsDuplicado() {
        Fixture f = new Fixture();
        alta(f, "Liderazgo de equipo", "¿Puede liderar?");
        alta(f, "Liderazgo", "¿Lidera a otros?");
        igual(2, f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId()).size());
    }

    private static void enunciadoDuplicadoNoBloqueaAlta() {
        Fixture f = new Fixture();
        alta(f, "Pregunta A", "¿Puede liderar?");
        alta(f, "Pregunta B", "¿Puede liderar?");
        igual(2, f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId()).size());
    }

    private static void competenciaInactiva() {
        Fixture f = new Fixture();
        Competencia inactiva = new Competencia(80L, "Inactiva", "", false,
                TipoCompetencia.ACTITUDINAL, null, null);
        Factor factor = new Factor(81L, "Factor", "", 1, true, inactiva);
        esperar(IllegalArgumentException.class, () -> f.gestorPreguntas.darDeAltaPregunta(
                inactiva, factor, "Nombre", "Enunciado", "", opcionesValidas(), TipoPregunta.ESCALA));
    }

    private static void factorInactivo() {
        Fixture f = new Fixture();
        Factor inactivo = new Factor(82L, "Inactivo", "", 1, false, f.competencia);
        esperar(IllegalArgumentException.class, () -> f.gestorPreguntas.darDeAltaPregunta(
                f.competencia, inactivo, "Nombre", "Enunciado", "", opcionesValidas(), TipoPregunta.ESCALA));
    }

    private static void factorDeOtraCompetencia() {
        Fixture f = new Fixture();
        Competencia otra = new Competencia(83L, "Otra", "", true,
                TipoCompetencia.ACTITUDINAL, null, null);
        Factor factor = new Factor(84L, "Factor", "", 1, true, otra);
        esperar(IllegalArgumentException.class, () -> f.gestorPreguntas.darDeAltaPregunta(
                f.competencia, factor, "Nombre", "Enunciado", "", opcionesValidas(), TipoPregunta.ESCALA));
    }

    private static void elementosSinId() {
        Fixture f = new Fixture();
        Competencia sinId = new Competencia(null, "Sin id", "", true,
                TipoCompetencia.ACTITUDINAL, null, null);
        Factor factorSinId = new Factor(null, "Sin id", "", 1, true, sinId);
        esperar(IllegalArgumentException.class, () -> f.gestorPreguntas.darDeAltaPregunta(
                sinId, factorSinId, "Nombre", "Enunciado", "", opcionesValidas(), TipoPregunta.ESCALA));
    }

    private static void opcionNula() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> opciones = new ArrayList<>(opcionesValidas());
        opciones.set(0, null);
        esperar(IllegalArgumentException.class, () -> alta(f, "Nombre", "Enunciado", opciones));
    }

    private static void opcionSinId() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> opciones = List.of(
                new OpcionRespuesta(null, "A", 5.0, 1, true),
                new OpcionRespuesta(51L, "B", 5.0, 2, true));
        esperar(IllegalArgumentException.class, () -> alta(f, "Nombre", "Enunciado", opciones));
    }

    private static void opcionInactiva() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> opciones = List.of(
                new OpcionRespuesta(51L, "A", 5.0, 1, true),
                new OpcionRespuesta(52L, "B", 5.0, 2, false));
        esperar(IllegalArgumentException.class, () -> alta(f, "Nombre", "Enunciado", opciones));
    }

    private static void puntajeNulo() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> opciones = List.of(
                new OpcionRespuesta(51L, "A", null, 1, true),
                new OpcionRespuesta(52L, "B", 10.0, 2, true));
        esperar(IllegalArgumentException.class, () -> alta(f, "Nombre", "Enunciado", opciones));
    }

    private static void puntajeNegativo() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> opciones = List.of(
                new OpcionRespuesta(51L, "A", -1.0, 1, true),
                new OpcionRespuesta(52L, "B", 11.0, 2, true));
        esperar(IllegalArgumentException.class, () -> alta(f, "Nombre", "Enunciado", opciones));
    }

    private static void sumaIncorrecta() {
        Fixture f = new Fixture();
        List<OpcionRespuesta> opciones = List.of(
                new OpcionRespuesta(51L, "A", 4.0, 1, true),
                new OpcionRespuesta(52L, "B", 5.0, 2, true));
        esperar(IllegalArgumentException.class, () -> alta(f, "Nombre", "Enunciado", opciones));
    }

    private static void regresionBusquedaCU020() {
        Fixture f = new Fixture();
        alta(f, "Liderazgo de equipo", "¿Coordina al equipo?");
        alta(f, "Comunicación", "¿Se comunica con claridad?");
        igual(1, f.gestorPreguntas.buscarPregunta(f.competencia, f.factor, "liderazgo").size());
        igual(2, f.gestorPreguntas.buscarPregunta(f.factor).size());
        esperar(IllegalArgumentException.class, () -> f.gestorPreguntas.buscarPregunta(null, null, " "));
    }

    private static void regresionModificacionCU022() {
        Fixture f = new Fixture();
        alta(f, "Pregunta A", "Enunciado A");
        alta(f, "Pregunta B", "Enunciado B");
        List<Pregunta> preguntas = f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId());
        Pregunta a = preguntas.get(0);
        Long idOriginal = a.getId();
        f.gestorPreguntas.modificarPregunta(a, TipoPregunta.VERDADERO_FALSO,
                "Pregunta A modificada", "Enunciado A modificado", "", opcionesValidas());
        igual(idOriginal, a.getId());
        igual(TipoPregunta.VERDADERO_FALSO, a.getTipo());
        esperar(IllegalArgumentException.class, () -> f.gestorPreguntas.modificarPregunta(a,
                TipoPregunta.ESCALA, "Nombre diferente", "Enunciado B", "", opcionesValidas()));
    }

    private static void regresionLongitudCU022() {
        Fixture f = new Fixture();
        alta(f, "Pregunta", "Enunciado");
        Pregunta pregunta = f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId()).get(0);
        String nombreOriginal = pregunta.getNombre();
        esperar(IllegalArgumentException.class, () -> f.gestorPreguntas.modificarPregunta(pregunta,
                TipoPregunta.ESCALA, "N".repeat(51), "Otro enunciado", "", opcionesValidas()));
        igual(nombreOriginal, pregunta.getNombre());
    }

    private static void regresionEvaluabilidadCU025() {
        Fixture f = new Fixture();
        Factor segundoFactor = new Factor(null, "Análisis", "", 2, true, f.competencia);
        FactorDAO factores = new FactorDAO(List.of(f.factor, segundoFactor));
        GestorFactores gestorFactores = new GestorFactores(factores, f.competenciaDAO);
        GestorCompetencias gestorCompetencias = new GestorCompetencias(
                f.competenciaDAO, gestorFactores, f.gestorPreguntas);
        Puesto puesto = new Puesto("P-1", "Analista", "", true, new Empresa("Empresa"),
                List.of(new CompetenciaRequerida(f.competencia, 5.0)));

        alta(f, "Pregunta 1", "Enunciado 1");
        alta(f, "Pregunta 2", "Enunciado 2");
        igual(1, gestorCompetencias.validarCompetencia(puesto).size());

        f.gestorPreguntas.darDeAltaPregunta(f.competencia, segundoFactor, "Pregunta 3", "Enunciado 3",
                "", opcionesValidas(), TipoPregunta.ESCALA);
        f.gestorPreguntas.darDeAltaPregunta(f.competencia, segundoFactor, "Pregunta 4", "Enunciado 4",
                "", opcionesValidas(), TipoPregunta.ESCALA);
        igual(0, gestorCompetencias.validarCompetencia(puesto).size());
    }

    private static void errorEnAlta(String nombre, String enunciado, List<OpcionRespuesta> opciones,
                                    TipoPregunta tipo) {
        Fixture f = new Fixture();
        esperar(IllegalArgumentException.class, () -> alta(f, nombre, enunciado, opciones, tipo));
        igual(0, f.preguntaDAO.obtenerPreguntasPorFactor(f.factor.getId()).size());
    }

    private static void alta(Fixture f, String nombre, String enunciado) {
        alta(f, nombre, enunciado, opcionesValidas(), TipoPregunta.ESCALA);
    }

    private static void alta(Fixture f, String nombre, String enunciado,
                             List<OpcionRespuesta> opciones) {
        alta(f, nombre, enunciado, opciones, TipoPregunta.ESCALA);
    }

    private static void alta(Fixture f, String nombre, String enunciado,
                             List<OpcionRespuesta> opciones, TipoPregunta tipo) {
        f.gestorPreguntas.darDeAltaPregunta(f.competencia, f.factor, nombre, enunciado,
                "Descripción", opciones, tipo);
    }

    private static void alta(Fixture f, String nombre, String enunciado, String descripcion,
                             List<OpcionRespuesta> opciones, TipoPregunta tipo) {
        f.gestorPreguntas.darDeAltaPregunta(f.competencia, f.factor, nombre, enunciado,
                descripcion, opciones, tipo);
    }

    private static List<OpcionRespuesta> opcionesValidas() {
        return List.of(new OpcionRespuesta(101L, "Bajo", 4.0, 2, true),
                new OpcionRespuesta(102L, "Alto", 6.0, 1, true));
    }

    private static void probar(String nombre, Runnable prueba) {
        ejecutadas++;
        try {
            prueba.run();
            System.out.println("OK  " + nombre);
        } catch (Throwable error) {
            fallidas++;
            System.err.println("ERROR  " + nombre + " -> " + error);
        }
    }

    private static void esperar(Class<? extends Throwable> tipoEsperado, Runnable accion) {
        esperarMensaje(tipoEsperado, accion);
    }

    private static String esperarMensaje(Class<? extends Throwable> tipoEsperado, Runnable accion) {
        try {
            accion.run();
        } catch (Throwable error) {
            if (tipoEsperado.isInstance(error)) return error.getMessage();
            throw new AssertionError("Se esperaba " + tipoEsperado.getSimpleName()
                    + " pero se recibió " + error.getClass().getSimpleName(), error);
        }
        throw new AssertionError("Se esperaba " + tipoEsperado.getSimpleName() + " y no se lanzó excepción.");
    }

    private static void afirmar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void igual(Object esperado, Object actual) {
        if (esperado == null ? actual != null : !esperado.equals(actual)) {
            throw new AssertionError("Esperado: " + esperado + "; actual: " + actual);
        }
    }

    private static void noNulo(Object valor) {
        if (valor == null) throw new AssertionError("Se esperaba un valor no nulo.");
    }

    private static void contiene(List<?> elementos, Object esperado) {
        if (!elementos.contains(esperado)) throw new AssertionError("No se encontró el elemento esperado.");
    }

    private static final class Fixture {
        private final Competencia competencia = new Competencia(1L, "Datos", "", true,
                TipoCompetencia.ACTITUDINAL, null, null);
        private final Competencia competenciaInactiva = new Competencia(2L, "Datos antiguos", "", false,
                TipoCompetencia.ACTITUDINAL, null, null);
        private final Factor factor = new Factor(1L, "Comunicación", "", 1, true, competencia);
        private final Factor factorInactivo = new Factor(2L, "Comunicación inactiva", "", 2, false, competencia);
        private final Factor factorEnCompetenciaInactiva = new Factor(3L, "Comunicación", "", 1, true,
                competenciaInactiva);
        private final CompetenciaDAO competenciaDAO = new CompetenciaDAO(
                List.of(competencia, competenciaInactiva));
        private final FactorDAO factorDAO = new FactorDAO(
                List.of(factor, factorInactivo, factorEnCompetenciaInactiva));
        private final PreguntaDAO preguntaDAO = new PreguntaDAO();
        private final GestorPreguntas gestorPreguntas = new GestorPreguntas(preguntaDAO);
        private final GestorFactores gestorFactores = new GestorFactores(factorDAO, competenciaDAO);
        private final GestorCompetencias gestorCompetencias = new GestorCompetencias(
                competenciaDAO, gestorFactores, gestorPreguntas);
        private final GestorOpcionesRespuesta gestorOpciones = new GestorOpcionesRespuesta(
                new OpcionRespuestaDAO(List.of(
                        new OpcionRespuesta(11L, "Sí", 5.0, 1, true),
                        new OpcionRespuesta(12L, "Sí, parcialmente", 3.0, 2, true),
                        new OpcionRespuesta(13L, "No", 0.0, 3, true),
                        new OpcionRespuesta(14L, "Inactiva", 2.0, 4, false))));
    }
}
