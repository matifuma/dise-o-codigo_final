import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Pruebas ejecutables de CU026 y regresiones del flujo CU025 ya existente. */
public final class PruebasCU026 {
    private static int ejecutadas;
    private static int fallidas;

    private PruebasCU026() { }

    public static void main(String[] args) {
        probar("DAO obtiene un único cuestionario vigente con la firma singular del diagrama",
                PruebasCU026::consultaSingular);
        probar("la clave incorrecta impide iniciar y no consume accesos", PruebasCU026::claveIncorrecta);
        probar("cancelar conserva el cuestionario ACTIVO y no genera bloques", PruebasCU026::cancelarInstrucciones);
        probar("el máximo de accesos finaliza el cuestionario como INCOMPLETO", PruebasCU026::maximoAccesos);
        probar("el vencimiento EN_CURSO finaliza como INCOMPLETO", PruebasCU026::vencimientoEnCurso);
        probar("el vencimiento ACTIVO antes del primer ingreso queda SIN_CONTESTAR",
                PruebasCU026::vencimientoActivoAntesDeIniciar);
        probar("modificarParametros actualiza el tiempo máximo central", PruebasCU026::modificarParametrosActualizaTiempo);
        probar("el cierre programado finaliza cuestionarios vencidos según su estado",
                PruebasCU026::cierreProgramado);
        probar("reanudar devuelve el primer bloque pendiente sin volver a generar preguntas", PruebasCU026::reanudar);
        probar("aceptar genera bloques, guardar exige respuestas completas y finalizar calcula resultados",
                PruebasCU026::flujoCompleto);
        ejecutar("Pruebas CU026 ejecutadas: " + ejecutadas + "; fallidas: " + fallidas + ".");
        if (fallidas > 0) throw new AssertionError("Fallaron " + fallidas + " pruebas de CU026.");
    }

    private static void consultaSingular() {
        Fixture f = new Fixture(EstadoCuestionario.ACTIVO, 0, new Date(), null, null);
        igual(f.cuestionario, f.cuestionarioDAO.obtenerCuestionarioActivo("DNI", "100"));
        igual(null, f.cuestionarioDAO.obtenerCuestionarioActivo("DNI", "999"));
    }

    private static void claveIncorrecta() {
        Fixture f = new Fixture(EstadoCuestionario.ACTIVO, 0, new Date(), null, null);
        esperar(IllegalArgumentException.class,
                () -> f.gestor.iniciarCuestionario("DNI", "100", "INCORRECTA"));
        igual(0, f.cuestionario.getCantidadAccesos());
        igual(EstadoCuestionario.ACTIVO, f.cuestionario.getEstado());
    }

    private static void cancelarInstrucciones() {
        Fixture f = new Fixture(EstadoCuestionario.ACTIVO, 0, new Date(), null, null);
        Cuestionario devuelto = f.gestor.iniciarCuestionario("DNI", "100", "CLAVE123");
        igual(f.cuestionario, devuelto);
        igual(EstadoCuestionario.ACTIVO, devuelto.getEstado());
        igual(1, devuelto.getCantidadAccesos());
        igual(0, devuelto.getBloques().size());
        // La interfaz termina el flujo al cancelar y no invoca obtenerBloquePendiente.
        igual(EstadoCuestionario.ACTIVO, f.cuestionarioDAO.obtenerCuestionarioActivo("DNI", "100").getEstado());
    }

    private static void maximoAccesos() {
        Fixture f = new Fixture(EstadoCuestionario.ACTIVO, 3, new Date(), null, null);
        Cuestionario devuelto = f.gestor.iniciarCuestionario("DNI", "100", "CLAVE123");
        igual(EstadoCuestionario.INCOMPLETO, devuelto.getEstado());
        igual(3, devuelto.getCantidadAccesos());
        igual(EstadoCuestionario.INCOMPLETO, f.cuestionarioDAO.todos().get(0).getEstado());
    }

    private static void vencimientoEnCurso() {
        Date antigua = new Date(System.currentTimeMillis() - 120_000L);
        Fixture f = new Fixture(EstadoCuestionario.EN_CURSO, 0, antigua, antigua, antigua);
        Cuestionario devuelto = f.gestor.iniciarCuestionario("DNI", "100", "CLAVE123");
        igual(EstadoCuestionario.INCOMPLETO, devuelto.getEstado());
    }

    private static void vencimientoActivoAntesDeIniciar() {
        Date antigua = new Date(System.currentTimeMillis() - 120_000L);
        Fixture f = new Fixture(EstadoCuestionario.ACTIVO, 0, antigua, null, antigua);
        Cuestionario devuelto = f.gestor.iniciarCuestionario("DNI", "100", "CLAVE123");
        igual(EstadoCuestionario.SIN_CONTESTAR, devuelto.getEstado());
        igual(0, devuelto.getCantidadAccesos());
    }

    private static void modificarParametrosActualizaTiempo() {
        ConfiguracionSistema configuracion = new ConfiguracionSistema(1L, "Instrucciones", 10, 3);
        configuracion.modificarParametros("Instrucciones actualizadas", 4);
        igual(4, configuracion.getTiempoMaximoMinutos());
        configuracion.modificarParametros("Nuevas instrucciones", 2);
        igual(2, configuracion.getTiempoMaximoMinutos());
        igual("Nuevas instrucciones", configuracion.getInstruccionesCuestionario());
        esperar(IllegalArgumentException.class, () -> configuracion.modificarParametros("No aplicar", 0));
        igual(2, configuracion.getTiempoMaximoMinutos());
        igual("Nuevas instrucciones", configuracion.getInstruccionesCuestionario());
    }

    private static void cierreProgramado() {
        Date antigua = new Date(System.currentTimeMillis() - 120_000L);
        Fixture activo = new Fixture(EstadoCuestionario.ACTIVO, 0, antigua, null, antigua);
        activo.gestor.finalizarCuestionariosVencidos();
        igual(EstadoCuestionario.SIN_CONTESTAR, activo.cuestionario.getEstado());

        Fixture enCurso = new Fixture(EstadoCuestionario.EN_CURSO, 0, antigua, antigua, antigua);
        enCurso.gestor.finalizarCuestionariosVencidos();
        igual(EstadoCuestionario.INCOMPLETO, enCurso.cuestionario.getEstado());
    }

    private static void reanudar() {
        Fixture f = new Fixture(EstadoCuestionario.ACTIVO, 0, new Date(), null, null);
        Cuestionario cuestionario = f.gestor.iniciarCuestionario("DNI", "100", "CLAVE123");
        BloqueCuestionario primero = f.gestor.obtenerBloquePendiente(cuestionario);
        responderTodo(primero);
        f.gestor.guardarBloqueCuestionario(primero);
        int cantidadBloques = cuestionario.getBloques().size();
        Long idSegundoBloque = cuestionario.getBloques().get(1).getId();

        Cuestionario retomado = f.gestor.iniciarCuestionario("DNI", "100", "CLAVE123");
        igual(cuestionario, retomado);
        igual(2, retomado.getCantidadAccesos());
        igual(cantidadBloques, retomado.getBloques().size());
        igual(idSegundoBloque, f.gestor.obtenerBloquePendiente(retomado).getId());
    }

    private static void flujoCompleto() {
        Fixture f = new Fixture(EstadoCuestionario.ACTIVO, 0, new Date(), null, null);
        Cuestionario devuelto = f.gestor.iniciarCuestionario("DNI", "100", "CLAVE123");
        igual(EstadoCuestionario.ACTIVO, devuelto.getEstado());
        igual(0, devuelto.getBloques().size());

        BloqueCuestionario bloque = f.gestor.obtenerBloquePendiente(devuelto);
        noNulo(bloque);
        igual(EstadoCuestionario.EN_CURSO, devuelto.getEstado());
        igual(2, devuelto.getCantidadPreguntasPorFactor());
        igual(2, devuelto.getCantidadPreguntasPorBloque());
        afirmar(devuelto.getBloques().size() == 3, "Se esperaban tres bloques para seis preguntas.");

        Pregunta faltante = bloque.getPreguntas().get(1);
        bloque.agregarRespuesta(respuestaAlta(bloque.getPreguntas().get(0)));
        afirmar(!bloque.validarRespuestasCompletas(), "El bloque debía marcar la pregunta pendiente.");
        f.gestor.guardarBloqueCuestionario(bloque);
        igual(false, bloque.getCompletado());
        igual(null, bloque.getRespuestas().get(0).getFechaRespuesta());

        bloque.agregarRespuesta(respuestaAlta(faltante));
        f.gestor.guardarBloqueCuestionario(bloque);
        igual(true, bloque.getCompletado());
        afirmar(bloque.getRespuestas().stream().allMatch(r -> r.getId() != null && r.getFechaRespuesta() != null),
                "Al guardar el bloque deben registrarse todas sus respuestas.");

        BloqueCuestionario siguiente;
        while ((siguiente = f.gestor.obtenerBloquePendiente(devuelto)) != null) {
            responderTodo(siguiente);
            f.gestor.guardarBloqueCuestionario(siguiente);
        }
        afirmar(devuelto.getBloques().stream().allMatch(b -> Boolean.TRUE.equals(b.getCompletado())),
                "Todos los bloques deben quedar completos antes de finalizar.");

        f.gestor.finalizarCuestionario(devuelto);
        igual(EstadoCuestionario.COMPLETADO, devuelto.getEstado());
        noNulo(devuelto.getFechaFin());
        igual(1, devuelto.getResultados().size());
        Resultado resultado = devuelto.getResultados().get(0);
        igual(3, resultado.getResultadosFactor().size());
        igual(6.0, resultado.getPuntajeObtenido());
        igual(5.0, resultado.getPuntajeRequerido());
        igual(true, resultado.cumpleMinimo());
        noNulo(resultado.getFechaCalculo());
        igual(null, f.cuestionarioDAO.obtenerCuestionarioActivo("DNI", "100"));
    }

    private static RespuestaCandidato respuestaAlta(Pregunta pregunta) {
        OpcionRespuesta opcion = pregunta.getOpciones().stream()
                .filter(o -> Boolean.TRUE.equals(o.getActivo()) && o.getPuntaje() == 6.0)
                .findFirst().orElseThrow();
        return new RespuestaCandidato(pregunta, opcion);
    }

    private static void responderTodo(BloqueCuestionario bloque) {
        for (Pregunta pregunta : bloque.getPreguntas()) bloque.agregarRespuesta(respuestaAlta(pregunta));
    }

    private static void probar(String nombre, Runnable caso) {
        ejecutadas++;
        try {
            caso.run();
            System.out.println("OK  " + nombre);
        } catch (Throwable error) {
            fallidas++;
            System.err.println("ERROR  " + nombre + " -> " + error);
        }
    }

    private static void ejecutar(String mensaje) { System.out.println(mensaje); }

    private static void esperar(Class<? extends Throwable> tipo, Runnable accion) {
        try {
            accion.run();
        } catch (Throwable error) {
            if (tipo.isInstance(error)) return;
            throw new AssertionError("Se esperaba " + tipo.getSimpleName() + " pero se recibió "
                    + error.getClass().getSimpleName(), error);
        }
        throw new AssertionError("Se esperaba " + tipo.getSimpleName() + " y no se lanzó excepción.");
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

    private static final class Fixture {
        private final Competencia competencia = new Competencia(10L, "Análisis", "", true,
                TipoCompetencia.ACTITUDINAL, null, null);
        private final Puesto puesto;
        private final Evaluacion evaluacion;
        private final Candidato candidato = new Candidato(true, 1, "Pérez", "Ana", "DNI", "100",
                null, null, null, null, "ana@example.test", false);
        private final Cuestionario cuestionario;
        private final CuestionarioDAO cuestionarioDAO = new CuestionarioDAO();
        private final GestorCuestionarios gestor;

        private Fixture(EstadoCuestionario estado, int accesos, Date fechaCambioEstado,
                        Date fechaInicio, Date fechaCreacion) {
            List<CompetenciaRequerida> requeridas = List.of(new CompetenciaRequerida(competencia, 5.0));
            puesto = new Puesto("P-1", "Analista", "", true, new Empresa("Empresa"), requeridas);
            evaluacion = new Evaluacion(null, new Date(), puesto);
            EvaluacionDAO evaluacionDAO = new EvaluacionDAO();
            evaluacionDAO.guardar(evaluacion);

            List<Factor> factoresIniciales = new ArrayList<>();
            PreguntaDAO preguntaDAO = new PreguntaDAO();
            for (int i = 0; i < 3; i++) {
                Factor factor = new Factor(20L + i, "Factor " + i, "", i + 1, true, competencia);
                factoresIniciales.add(factor);
                for (int j = 0; j < 2; j++) {
                    List<OpcionRespuesta> opciones = List.of(
                            new OpcionRespuesta(1000L + i * 10L + j * 2L, "Bajo", 4.0, 1, true),
                            new OpcionRespuesta(1001L + i * 10L + j * 2L, "Alto", 6.0, 2, true));
                    new Pregunta(2000L + i * 10L + j, "Pregunta " + i + "." + j,
                            "Enunciado", "", true, TipoPregunta.ESCALA, competencia, factor, opciones);
                    preguntaDAO.crearPregunta(factor.getPreguntas().get(factor.getPreguntas().size() - 1));
                }
            }
            FactorDAO factorDAO = new FactorDAO(factoresIniciales);
            cuestionario = new Cuestionario(null, fechaCreacion == null ? new Date() : fechaCreacion,
                    null, null, fechaInicio, null, fechaCambioEstado, accesos, "CLAVE123", estado,
                    candidato, evaluacion);
            cuestionarioDAO.guardar(cuestionario);
            ConfiguracionDAO configuracionDAO = new ConfiguracionDAO(
                    new ConfiguracionSistema(1L, "Lea las instrucciones", 1, 3));
            gestor = new GestorCuestionarios(new CandidatoDAO(List.of(candidato)), cuestionarioDAO,
                    evaluacionDAO, new ExportadorExcel(), configuracionDAO, factorDAO, preguntaDAO);
        }

    }
}
