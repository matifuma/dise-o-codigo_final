import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Pruebas ejecutables del flujo de búsqueda y emisión de orden de mérito del CU028. */
public final class PruebasCU028 {
    private static int ejecutadas;
    private static int fallidas;

    private PruebasCU028() { }

    public static void main(String[] args) throws Exception {
        probar("buscar puesto filtra por código, nombre y empresa ignorando mayúsculas", PruebasCU028::buscarPuestos);
        probar("buscar puesto excluye puestos inactivos", PruebasCU028::buscarPuestosActivos);
        probar("obtener puesto devuelve el puesto por código", PruebasCU028::obtenerPuesto);
        probar("obtener puesto rechaza código vacío", PruebasCU028::obtenerPuestoCodigoVacio);
        probar("conteos distinguen candidatos y cuestionarios completados", PruebasCU028::conteos);
        probar("listar evaluaciones devuelve DTOs del puesto correcto", PruebasCU028::listarEvaluaciones);
        probar("buscar cuestionarios filtra por ids de evaluación", PruebasCU028::buscarCuestionariosPorEvaluaciones);
        probar("orden de mérito prioriza aptos, puntaje y agrupa los demás por estado", PruebasCU028::emitirOrdenMerito);
        probar("orden de mérito rechaza selección de evaluaciones vacía", PruebasCU028::ordenSinEvaluaciones);
        probar("CSV exporta filas con cabecera, estado y datos escapados", PruebasCU028::exportarCSV);
        probar("PuestoDTO conserva ambos conteos", PruebasCU028::puestoDTOConConteos);

        System.out.println("Pruebas CU028 ejecutadas: " + ejecutadas + "; fallidas: " + fallidas + ".");
        if (fallidas > 0) throw new AssertionError("Fallaron " + fallidas + " pruebas del CU028.");
    }

    private static void buscarPuestos() {
        Empresa empresa = new Empresa("Acme SA");
        Puesto activo = puesto("P-10", "Analista de Datos", empresa, true);
        Puesto otro = puesto("P-20", "Analista QA", new Empresa("Beta"), true);
        GestorPuestos gestor = new GestorPuestos(new PuestoDAO(List.of(activo, otro)), new CuestionarioDAO());
        igual(List.of(activo), gestor.buscarPuesto("p-1", "datos", "ACME"));
        igual(2, gestor.buscarPuesto(null, "analista", null).size());
    }

    private static void buscarPuestosActivos() {
        Puesto activo = puesto("P-1", "Activo", new Empresa("Empresa"), true);
        Puesto inactivo = puesto("P-2", "Inactivo", new Empresa("Empresa"), false);
        GestorPuestos gestor = new GestorPuestos(new PuestoDAO(List.of(activo, inactivo)), new CuestionarioDAO());
        igual(List.of(activo), gestor.buscarPuesto(null, null, null));
    }

    private static void obtenerPuesto() {
        Puesto puesto = puesto("P-1", "Analista", new Empresa("Empresa"), true);
        GestorPuestos gestor = new GestorPuestos(new PuestoDAO(List.of(puesto)), new CuestionarioDAO());
        igual(puesto, gestor.obtenerPuesto("P-1"));
        igual(null, gestor.obtenerPuesto("P-no-existe"));
    }

    private static void obtenerPuestoCodigoVacio() {
        PuestoDAO dao = new PuestoDAO();
        espera(IllegalArgumentException.class, () -> dao.obtenerPuesto("  "));
    }

    private static void conteos() {
        Fixture f = new Fixture();
        Candidato repetido = candidato(1, "Pérez", "Ana");
        f.agregarCuestionario(repetido, f.evaluacion1, EstadoCuestionario.COMPLETADO, 8.0, "00000001");
        f.agregarCuestionario(repetido, f.evaluacion2, EstadoCuestionario.INCOMPLETO, null, "00000002");
        f.agregarCuestionario(candidato(2, "López", "Luis"), f.evaluacion1,
                EstadoCuestionario.COMPLETADO, 6.0, "00000003");
        f.agregarCuestionario(candidato(3, "Otro", "Puesto"), f.evaluacionOtroPuesto,
                EstadoCuestionario.COMPLETADO, 9.0, "00000004");

        GestorPuestos gestor = f.gestorPuestos();
        igual(Integer.valueOf(2), gestor.contarCandidatosPorPuesto(f.puesto.getCodigo()));
        igual(Integer.valueOf(2), gestor.contarEvaluacionesCompletasPorPuesto(f.puesto.getCodigo()));
    }

    private static void listarEvaluaciones() {
        Fixture f = new Fixture();
        List<EvaluacionDTO> resultado = new GestorReportes(f.evaluacionDAO, f.cuestionarioDAO,
                new ExportadorCSV()).listarEvaluacionesDelPuesto(f.puesto.getCodigo());
        igual(2, resultado.size());
        igual(f.evaluacion1.getId(), resultado.get(0).getId());
        igual(f.puesto.getNombre(), resultado.get(0).getNombrePuesto());
        List<EvaluacionDTO> delOtroPuesto = new GestorReportes(f.evaluacionDAO, f.cuestionarioDAO,
                new ExportadorCSV()).listarEvaluacionesDelPuesto(f.otroPuesto.getCodigo());
        igual(1, delOtroPuesto.size());
        igual(f.evaluacionOtroPuesto.getId(), delOtroPuesto.get(0).getId());
    }

    private static void buscarCuestionariosPorEvaluaciones() {
        Fixture f = new Fixture();
        Cuestionario a = f.agregarCuestionario(candidato(1, "A", "Uno"), f.evaluacion1,
                EstadoCuestionario.COMPLETADO, 8.0, "00000001");
        f.agregarCuestionario(candidato(2, "B", "Dos"), f.evaluacion2,
                EstadoCuestionario.COMPLETADO, 7.0, "00000002");
        f.agregarCuestionario(candidato(3, "C", "Tres"), f.evaluacionOtroPuesto,
                EstadoCuestionario.COMPLETADO, 9.0, "00000003");
        List<Cuestionario> resultado = f.cuestionarioDAO.buscarCuestionariosPorEvaluaciones(
                List.of(f.evaluacion1.getId()));
        igual(1, resultado.size());
        igual(a, resultado.get(0));
        igual(0, f.cuestionarioDAO.buscarCuestionariosPorEvaluaciones(List.of()).size());
    }

    private static void emitirOrdenMerito() {
        Fixture f = new Fixture();
        Candidato mejor = candidato(1, "Alvarez", "Ana");
        Candidato segundo = candidato(2, "Bravo", "Beto");
        Candidato noAlcanza = candidato(3, "Cruz", "Ceci");
        Candidato incompleto = candidato(4, "Díaz", "Dani");
        Candidato sinContestar = candidato(5, "Estevez", "Ema");
        f.agregarCuestionario(mejor, f.evaluacion1, EstadoCuestionario.COMPLETADO, 9.0, "00000001");
        f.agregarCuestionario(segundo, f.evaluacion1, EstadoCuestionario.COMPLETADO, 7.0, "00000002");
        f.agregarCuestionario(noAlcanza, f.evaluacion1, EstadoCuestionario.COMPLETADO, 3.0, "00000003");
        f.agregarCuestionario(incompleto, f.evaluacion1, EstadoCuestionario.INCOMPLETO, null, "00000004");
        f.agregarCuestionario(sinContestar, f.evaluacion1, EstadoCuestionario.SIN_CONTESTAR, null, "00000005");

        GestorReportes gestor = new GestorReportes(f.evaluacionDAO, f.cuestionarioDAO, new ExportadorCSV());
        ReporteOrdenMerito reporte = gestor.emitirOrdenMerito(f.puesto, List.of(f.evaluacion1.getId()));
        igual(f.puesto, reporte.leerPuesto());
        igual(5, reporte.leerCandidatos().size());
        igual(List.of(mejor, segundo, noAlcanza, incompleto, sinContestar), reporte.leerCandidatos());
        verdadero(reporte.leerFechaEmision() != null);
    }

    private static void ordenSinEvaluaciones() {
        Fixture f = new Fixture();
        GestorReportes gestor = new GestorReportes(f.evaluacionDAO, f.cuestionarioDAO, new ExportadorCSV());
        espera(IllegalArgumentException.class, () -> gestor.emitirOrdenMerito(f.puesto, List.of()));
    }

    private static void exportarCSV() throws Exception {
        Fixture f = new Fixture();
        f.agregarCuestionario(candidato(1, "Pérez, Jr.", "Ana \"A\""), f.evaluacion1,
                EstadoCuestionario.COMPLETADO, 8.0, "00000001");
        Path archivo = Files.createTempFile("cu028-", ".csv");
        try {
            new GestorReportes(f.evaluacionDAO, f.cuestionarioDAO, new ExportadorCSV())
                    .exportarResultadosCSV(f.puesto, archivo.toString());
            String texto = Files.readString(archivo, StandardCharsets.UTF_8);
            verdadero(texto.contains("Tipo de documento"));
            verdadero(texto.contains("Pérez, Jr."));
            verdadero(texto.contains("Ana \"\"A\"\""));
            verdadero(texto.contains("COMPLETADO"));
            verdadero(texto.contains("Puesto"));
        } finally {
            Files.deleteIfExists(archivo);
        }
    }

    private static void puestoDTOConConteos() {
        Puesto puesto = puesto("P-1", "Analista", new Empresa("Empresa"), true);
        PuestoDTO dto = new PuestoDTO(puesto, 12, 8);
        igual(Integer.valueOf(12), dto.getCantidadCandidatos());
        igual(Integer.valueOf(8), dto.getCantidadEvaluacionesCompletas());
    }

    private static Puesto puesto(String codigo, String nombre, Empresa empresa, boolean activo) {
        return new Puesto(codigo, nombre, "Descripción", activo, empresa, List.of());
    }

    private static Candidato candidato(int nro, String apellido, String nombre) {
        return new Candidato(true, nro, apellido, nombre, "DNI", String.valueOf(30_000_000 + nro),
                new Date(0), "X", "Argentina", "Universitario", "c" + nro + "@test.local", false);
    }

    private static void probar(String nombre, Prueba prueba) {
        ejecutadas++;
        try {
            prueba.ejecutar();
            System.out.println("OK: " + nombre);
        } catch (Throwable error) {
            fallidas++;
            System.err.println("FALLO: " + nombre + " -> " + error);
        }
    }

    @FunctionalInterface
    private interface Prueba {
        void ejecutar() throws Exception;
    }

    private static void igual(Object esperado, Object actual) {
        if (esperado == null ? actual != null : !esperado.equals(actual)) {
            throw new AssertionError("Esperado: " + esperado + "; recibido: " + actual);
        }
    }

    private static void verdadero(boolean condicion) {
        if (!condicion) throw new AssertionError("Se esperaba verdadero.");
    }

    private static void espera(Class<? extends Throwable> tipo, Runnable accion) {
        try {
            accion.run();
        } catch (Throwable error) {
            if (tipo.isInstance(error)) return;
            throw new AssertionError("Se esperaba " + tipo.getSimpleName() + " pero se recibió " + error, error);
        }
        throw new AssertionError("Se esperaba " + tipo.getSimpleName() + " y no se lanzó ninguna excepción.");
    }

    private static final class Fixture {
        private final Empresa empresa = new Empresa("Acme");
        private final Competencia competencia = new Competencia(1L, "Análisis", "", true,
                TipoCompetencia.TECNICA, null, null);
        private final CompetenciaRequerida requerida = new CompetenciaRequerida(competencia, 5.0);
        private final Puesto puesto = new Puesto("P-1", "Analista", "", true, empresa, List.of(requerida));
        private final Puesto otroPuesto = puesto("P-2", "Tester", new Empresa("Beta"), true);
        private final EvaluacionDAO evaluacionDAO = new EvaluacionDAO();
        private final CuestionarioDAO cuestionarioDAO = new CuestionarioDAO();
        private final Evaluacion evaluacion1 = crearEvaluacion(puesto);
        private final Evaluacion evaluacion2 = crearEvaluacion(puesto);
        private final Evaluacion evaluacionOtroPuesto = crearEvaluacion(otroPuesto);

        private Evaluacion crearEvaluacion(Puesto puesto) {
            Evaluacion evaluacion = new Evaluacion(null, new Date(1_700_000_000_000L), puesto);
            evaluacionDAO.guardar(evaluacion);
            return evaluacion;
        }

        private Cuestionario agregarCuestionario(Candidato candidato, Evaluacion evaluacion,
                                                  EstadoCuestionario estado, Double puntaje, String clave) {
            Cuestionario cuestionario = new Cuestionario(estado, clave, candidato, evaluacion);
            if (puntaje != null) {
                Factor factor = new Factor(1L, "Factor", "", 1, true, competencia);
                OpcionRespuesta opcion = new OpcionRespuesta(1L, "Ponderada", puntaje, 1, true);
                Pregunta pregunta = new Pregunta(1L, "Pregunta", "Enunciado", "", true,
                        TipoPregunta.ESCALA, competencia, factor, List.of(opcion));
                RespuestaCandidato respuesta = new RespuestaCandidato(pregunta, opcion);
                ResultadoFactor resultadoFactor = new ResultadoFactor(factor, List.of(respuesta));
                Resultado resultado = new Resultado(requerida);
                resultado.agregarResultadoFactor(resultadoFactor);
                resultado.calcularPuntaje();
                cuestionario.agregarResultado(resultado);
            }
            cuestionarioDAO.guardar(cuestionario);
            return cuestionario;
        }

        private GestorPuestos gestorPuestos() {
            return new GestorPuestos(new PuestoDAO(List.of(puesto, otroPuesto)), cuestionarioDAO);
        }
    }
}
