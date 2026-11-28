import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Orquesta la generación de claves, la creación de evaluaciones y la exportación de CU025. */
public class GestorCuestionarios {
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
    // La configuración actual no define cantidades de preguntas; se usan valores de implementación pendientes.
    private static final int PREGUNTAS_POR_FACTOR_POR_DEFECTO = 2;
    private static final int PREGUNTAS_POR_BLOQUE_POR_DEFECTO = 2;

    private final CandidatoDAO candidatoDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final EvaluacionDAO evaluacionDAO;
    private final ExportadorExcel exportadorExcel;
    private final SecureRandom random = new SecureRandom();
    private final Map<Integer, Integer> clavesPorCandidato = new HashMap<>();
    private final Set<Integer> clavesEmitidas = new HashSet<>();
    private ConfiguracionDAO configuracionDAO;
    private FactorDAO factorDAO;
    private PreguntaDAO preguntaDAO;

    public GestorCuestionarios(CandidatoDAO candidatoDAO, CuestionarioDAO cuestionarioDAO,
                              EvaluacionDAO evaluacionDAO, ExportadorExcel exportadorExcel) {
        if (candidatoDAO == null || cuestionarioDAO == null || evaluacionDAO == null || exportadorExcel == null) {
            throw new IllegalArgumentException("Se requieren los DAO y ExportadorExcel de CU025.");
        }
        this.candidatoDAO = candidatoDAO;
        this.cuestionarioDAO = cuestionarioDAO;
        this.evaluacionDAO = evaluacionDAO;
        this.exportadorExcel = exportadorExcel;
    }

    /** Constructor de ejecución CU026; el constructor previo se conserva para no alterar CU025. */
    public GestorCuestionarios(CandidatoDAO candidatoDAO, CuestionarioDAO cuestionarioDAO,
                              EvaluacionDAO evaluacionDAO, ExportadorExcel exportadorExcel,
                              ConfiguracionDAO configuracionDAO, FactorDAO factorDAO,
                              PreguntaDAO preguntaDAO) {
        this(candidatoDAO, cuestionarioDAO, evaluacionDAO, exportadorExcel);
        if (configuracionDAO == null || factorDAO == null || preguntaDAO == null) {
            throw new IllegalArgumentException("CU026 requiere ConfiguracionDAO, FactorDAO y PreguntaDAO.");
        }
        this.configuracionDAO = configuracionDAO;
        this.factorDAO = factorDAO;
        this.preguntaDAO = preguntaDAO;
    }

    /** Genera una clave numérica por candidato y devuelve DTO para mostrarla en CU025. */
    public synchronized List<CandidatoDTO> generarClavesAccesos(List<Candidato> candidatos) {
        validarSeleccion(candidatos);
        List<Candidato> activos = candidatoDAO.buscarActivos(candidatos);
        if (activos.size() != candidatos.size()) {
            throw new IllegalArgumentException("Todos los candidatos deben estar activos y registrados.");
        }
        Map<Integer, Candidato> candidatosCanonicos = new HashMap<>();
        for (Candidato candidato : activos) candidatosCanonicos.put(candidato.getNroCandidato(), candidato);
        List<CandidatoDTO> resultado = new ArrayList<>();
        Set<Integer> clavesNuevas = new HashSet<>();
        for (Candidato candidato : candidatos) {
            Candidato canonico = candidatosCanonicos.get(candidato.getNroCandidato());
            Integer clave = crearClave(clavesNuevas);
            clavesPorCandidato.put(canonico.getNroCandidato(), clave);
            resultado.add(new CandidatoDTO(canonico, clave));
        }
        return resultado;
    }

    /** Adaptador para la pantalla JavaFX de confirmación del CU025. */
    public synchronized List<CandidatoConClaveDTO> generarClavesAcceso(Collection<CandidatoDTO> seleccion) {
        if (seleccion == null || seleccion.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un candidato.");
        }
        List<Candidato> candidatos = new ArrayList<>();
        for (CandidatoDTO dto : seleccion) {
            if (dto == null || dto.getNroCandidato() == null) {
                throw new IllegalArgumentException("La selección contiene un candidato inválido.");
            }
            List<Candidato> encontrados = candidatoDAO.buscarCandidatos(null, null, dto.getNroCandidato());
            if (encontrados.size() != 1) {
                throw new IllegalArgumentException("El candidato ya no está disponible.");
            }
            candidatos.add(encontrados.get(0));
        }
        List<CandidatoDTO> conClaves = generarClavesAccesos(candidatos);
        List<CandidatoConClaveDTO> resultado = new ArrayList<>();
        for (int i = 0; i < conClaves.size(); i++) {
            resultado.add(new CandidatoConClaveDTO(candidatos.get(i),
                    String.valueOf(conClaves.get(i).getClaveAcceso())));
        }
        return resultado;
    }

    /** Crea la evaluación usando las claves generadas previamente para los candidatos. */
    public synchronized void evaluarCandidatos(List<Candidato> candidatos, Puesto puesto) {
        validarSeleccion(candidatos);
        if (puesto == null || !Boolean.TRUE.equals(puesto.getActivo())) {
            throw new IllegalArgumentException("Debe seleccionar un puesto activo.");
        }
        List<Candidato> activos = candidatoDAO.buscarActivos(candidatos);
        if (activos.size() != candidatos.size()) {
            throw new IllegalArgumentException("Todos los candidatos deben estar activos y registrados.");
        }
        Map<Integer, Candidato> candidatosCanonicos = new HashMap<>();
        for (Candidato candidato : activos) candidatosCanonicos.put(candidato.getNroCandidato(), candidato);

        Set<Integer> claves = new HashSet<>();
        List<Candidato> seleccion = new ArrayList<>();
        for (Candidato candidato : candidatos) {
            Integer clave = clavesPorCandidato.get(candidato.getNroCandidato());
            if (clave == null) throw new IllegalStateException("Genere las claves antes de confirmar la evaluación.");
            if (!claves.add(clave) || cuestionarioDAO.existeClave(clave.toString())) {
                throw new IllegalStateException("Las claves de acceso deben ser únicas.");
            }
            seleccion.add(candidatosCanonicos.get(candidato.getNroCandidato()));
        }
        for (Candidato candidato : seleccion) {
            if (cuestionarioDAO.obtenerCuestionarioActivo(
                    candidato.getTipoDocumento(), candidato.getNroDocumento()) != null) {
                throw new IllegalStateException("Hay candidatos con cuestionarios activos o en curso.");
            }
        }

        // En una base real este bloque debe ejecutarse dentro de una transacción ACID.
        Evaluacion evaluacion = new Evaluacion(null, new java.util.Date(), puesto);
        List<Cuestionario> guardados = new ArrayList<>();
        try {
            evaluacionDAO.guardar(evaluacion);
            for (Candidato candidato : seleccion) {
                Integer clave = clavesPorCandidato.get(candidato.getNroCandidato());
                Cuestionario cuestionario = new Cuestionario(
                        EstadoCuestionario.ACTIVO, clave.toString(), candidato, evaluacion);
                cuestionarioDAO.guardar(cuestionario);
                guardados.add(cuestionario);
            }
        } catch (RuntimeException error) {
            for (Cuestionario cuestionario : guardados) cuestionarioDAO.eliminar(cuestionario);
            evaluacionDAO.eliminar(evaluacion);
            throw error;
        }
    }

    /** Adapta el contrato de confirmación del frontend al flujo de evaluación ya implementado. */
    public synchronized void evaluarCandidatos(Collection<CandidatoConClaveDTO> seleccion, Puesto puesto) {
        if (seleccion == null || seleccion.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un candidato.");
        }
        List<Candidato> candidatos = new ArrayList<>();
        for (CandidatoConClaveDTO dto : seleccion) {
            if (dto == null || dto.getCandidato() == null || dto.getClave() == null) {
                throw new IllegalArgumentException("La selección contiene un candidato o clave inválidos.");
            }
            Integer claveGenerada = clavesPorCandidato.get(dto.getCandidato().getNroCandidato());
            if (claveGenerada == null || !dto.getClave().equals(String.valueOf(claveGenerada))) {
                throw new IllegalStateException("La clave no corresponde a la selección generada en esta sesión.");
            }
            candidatos.add(dto.getCandidato());
        }
        evaluarCandidatos(candidatos, puesto);
    }

    /** Exporta las filas de resultado definidas en el diagrama de clases. */
    public void exportarResultados(Puesto puesto, List<ResultadoExportacionDTO> resultado,
                                   String carpetaDestino) {
        if (puesto == null || resultado == null || carpetaDestino == null || carpetaDestino.trim().isEmpty()) {
            throw new IllegalArgumentException("Puesto, resultado y carpeta de destino son obligatorios.");
        }
        String nombrePuesto = normalizarNombreArchivo(puesto.getNombre());
        String nombre = FORMATO_ARCHIVO.format(LocalDateTime.now()) + "-" + nombrePuesto + ".xls";
        java.nio.file.Path archivo = java.nio.file.Paths.get(carpetaDestino).toAbsolutePath().normalize().resolve(nombre);
        exportadorExcel.exportarDatos(resultado, archivo.toString());
    }

    /** Exportación de claves que usa la pantalla de confirmación del CU025. */
    public void exportarResultado(Puesto puesto, List<CandidatoConClaveDTO> resultado,
                                  String carpetaDestino) {
        if (puesto == null || resultado == null || carpetaDestino == null || carpetaDestino.trim().isEmpty()) {
            throw new IllegalArgumentException("Puesto, resultado y carpeta de destino son obligatorios.");
        }
        String nombrePuesto = normalizarNombreArchivo(puesto.getNombre());
        String nombre = FORMATO_ARCHIVO.format(LocalDateTime.now()) + "-" + nombrePuesto + ".xls";
        java.nio.file.Path archivo = java.nio.file.Paths.get(carpetaDestino)
                .toAbsolutePath().normalize().resolve(nombre);
        exportadorExcel.exportarDatos(resultado, archivo.toString());
    }

    /** Recupera el único cuestionario ACTIVO/EN_CURSO del candidato y verifica sus límites. */
    public Cuestionario iniciarCuestionario(String tipoDoc, String nroDoc, String clave) {
        requerirDependenciasCU026();
        if (tipoDoc == null || tipoDoc.isBlank() || nroDoc == null || nroDoc.isBlank()
                || clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("Tipo, número de documento y clave son obligatorios.");
        }
        ConfiguracionSistema configuracion = configuracionDAO.obtenerParametros();
        Cuestionario cuestionario = cuestionarioDAO.obtenerCuestionarioActivo(tipoDoc, nroDoc);
        if (cuestionario == null) throw new IllegalArgumentException("El candidato no tiene un cuestionario vigente.");
        if (!clave.equals(cuestionario.getClave())) throw new IllegalArgumentException("La clave de acceso no es válida.");

        if (valor(cuestionario.getCantidadAccesos()) >= configuracion.getCantidadAccesosMaximos()) {
            cuestionario.cambiarEstado(EstadoCuestionario.INCOMPLETO);
            cuestionarioDAO.actualizarCuestionario(cuestionario);
            return cuestionario;
        }
        if (marcarVencidoSiCorresponde(cuestionario, configuracion)) {
            return cuestionario;
        }

        // El acceso se cuenta al abrir la pantalla; cancelar deja el cuestionario ACTIVO sin responderlo.
        cuestionario.registrarAcceso();
        cuestionarioDAO.actualizarCuestionario(cuestionario);
        return cuestionario;
    }

    /**
     * Devuelve el primer bloque pendiente. En el primer ingreso, esta llamada ocurre
     * solamente después de que la interfaz recibió la aceptación de las instrucciones.
     */
    public BloqueCuestionario obtenerBloquePendiente(Cuestionario cuestionario) {
        requerirDependenciasCU026();
        validarCuestionarioPersistido(cuestionario);
        if (marcarVencidoSiCorresponde(cuestionario)) return null;
        if (cuestionario.getEstado() != EstadoCuestionario.ACTIVO
                && cuestionario.getEstado() != EstadoCuestionario.EN_CURSO) {
            return null;
        }
        if (cuestionario.getEstado() == EstadoCuestionario.ACTIVO && cuestionario.getBloques().isEmpty()) {
            prepararBloques(cuestionario);
            cuestionario.iniciar();
            cuestionario.cambiarEstado(EstadoCuestionario.EN_CURSO);
            cuestionarioDAO.actualizarCuestionario(cuestionario);
        }
        for (BloqueCuestionario bloque : cuestionario.getBloques()) {
            if (!Boolean.TRUE.equals(bloque.getCompletado())) return bloque;
        }
        return null;
    }

    /** Registra respuestas solo cuando están completas y actualiza el cuestionario asociado. */
    public void guardarBloqueCuestionario(BloqueCuestionario bloque) {
        if (bloque == null || bloque.getCuestionario() == null) {
            throw new IllegalArgumentException("El bloque debe pertenecer a un cuestionario.");
        }
        Cuestionario cuestionario = bloque.getCuestionario();
        requerirDependenciasCU026();
        validarCuestionarioPersistido(cuestionario);
        if (marcarVencidoSiCorresponde(cuestionario)) return;
        if (!Boolean.TRUE.equals(bloque.validarRespuestasCompletas())) return;
        if (cuestionario.getEstado() != EstadoCuestionario.EN_CURSO) {
            throw new IllegalStateException("Solo se pueden guardar bloques de un cuestionario EN_CURSO.");
        }
        for (RespuestaCandidato respuesta : bloque.getRespuestas()) respuesta.registrar();
        bloque.marcarCompletado();
        cuestionarioDAO.actualizarCuestionario(cuestionario);
    }

    /** Calcula los resultados del puesto y completa el cuestionario. */
    public void finalizarCuestionario(Cuestionario cuestionario) {
        requerirDependenciasCU026();
        validarCuestionarioPersistido(cuestionario);
        if (marcarVencidoSiCorresponde(cuestionario)) return;
        if (cuestionario.getEstado() == EstadoCuestionario.COMPLETADO) return;
        if (cuestionario.getEstado() != EstadoCuestionario.EN_CURSO
                || cuestionario.getBloques().isEmpty()
                || cuestionario.getBloques().stream().anyMatch(b -> !Boolean.TRUE.equals(b.getCompletado()))) {
            throw new IllegalStateException("El cuestionario solo finaliza cuando todos sus bloques están completos.");
        }
        cuestionario.finalizar();
        calcularResultados(cuestionario);
        cuestionarioDAO.actualizarCuestionario(cuestionario);
        cuestionario.cambiarEstado(EstadoCuestionario.COMPLETADO);
        cuestionarioDAO.actualizarCuestionario(cuestionario);
    }

    /** Cierra los cuestionarios vencidos cuando la tarea programada invoca esta operación. */
    public void finalizarCuestionariosVencidos() {
        requerirDependenciasCU026();
        ConfiguracionSistema configuracion = configuracionDAO.obtenerParametros();
        for (Cuestionario cuestionario : cuestionarioDAO.todos()) {
            marcarVencidoSiCorresponde(cuestionario, configuracion);
        }
    }

    private void prepararBloques(Cuestionario cuestionario) {
        int preguntasPorFactor = cuestionario.getCantidadPreguntasPorFactor() == null
                ? PREGUNTAS_POR_FACTOR_POR_DEFECTO : cuestionario.getCantidadPreguntasPorFactor();
        int preguntasPorBloque = cuestionario.getCantidadPreguntasPorBloque() == null
                ? PREGUNTAS_POR_BLOQUE_POR_DEFECTO : cuestionario.getCantidadPreguntasPorBloque();
        if (preguntasPorFactor <= 0 || preguntasPorBloque <= 0) {
            throw new IllegalStateException("Las cantidades de preguntas por factor y por bloque deben ser positivas.");
        }
        cuestionario.setCantidadPreguntasPorFactor(preguntasPorFactor);
        cuestionario.setCantidadPreguntasPorBloque(preguntasPorBloque);

        Puesto puesto = cuestionario.getPuesto();
        if (puesto == null || puesto.getCompetenciasRequeridas().isEmpty()) {
            throw new IllegalStateException("El cuestionario no tiene competencias requeridas asociadas al puesto.");
        }
        List<Pregunta> seleccionadas = new ArrayList<>();
        for (CompetenciaRequerida requerida : puesto.getCompetenciasRequeridas()) {
            Competencia competencia = requerida.getCompetencia();
            if (competencia == null || competencia.getId() == null) continue;
            for (Factor factor : factorDAO.obtenerFactoresPorCompetencia(competencia.getId())) {
                if (!Boolean.TRUE.equals(factor.getActivo())) continue;
                List<Pregunta> disponibles = new ArrayList<>();
                for (Pregunta pregunta : preguntaDAO.obtenerPreguntasPorFactor(factor.getId())) {
                    if (Boolean.TRUE.equals(pregunta.getActivo())) disponibles.add(pregunta);
                }
                // El diagrama marca esta selección como opcional: solo participan factores con preguntas suficientes.
                if (disponibles.size() < 2 || disponibles.size() < preguntasPorFactor) continue;
                Collections.shuffle(disponibles, random);
                seleccionadas.addAll(disponibles.subList(0, preguntasPorFactor));
            }
        }
        if (seleccionadas.isEmpty()) {
            throw new IllegalStateException("No hay factores activos con preguntas suficientes para iniciar el cuestionario.");
        }
        for (int inicio = 0, numero = 1; inicio < seleccionadas.size(); inicio += preguntasPorBloque, numero++) {
            int fin = Math.min(inicio + preguntasPorBloque, seleccionadas.size());
            cuestionario.agregarBloque(new BloqueCuestionario(numero,
                    new ArrayList<>(seleccionadas.subList(inicio, fin))));
        }
    }

    private void calcularResultados(Cuestionario cuestionario) {
        Puesto puesto = cuestionario.getPuesto();
        Map<Factor, List<RespuestaCandidato>> respuestasPorFactor = new LinkedHashMap<>();
        for (BloqueCuestionario bloque : cuestionario.getBloques()) {
            for (RespuestaCandidato respuesta : bloque.getRespuestas()) {
                Factor factor = respuesta.getPregunta().getFactor();
                if (factor != null) respuestasPorFactor.computeIfAbsent(factor, ignorado -> new ArrayList<>()).add(respuesta);
            }
        }
        for (CompetenciaRequerida requerida : puesto.getCompetenciasRequeridas()) {
            Resultado resultado = new Resultado(requerida);
            for (Map.Entry<Factor, List<RespuestaCandidato>> entry : respuestasPorFactor.entrySet()) {
                if (entry.getKey().getCompetencia() != null
                        && Objects.equals(entry.getKey().getCompetencia().getId(), requerida.getCompetencia().getId())) {
                    ResultadoFactor resultadoFactor = new ResultadoFactor(entry.getKey(), entry.getValue());
                    resultadoFactor.calcularPuntajeFactor();
                    resultado.agregarResultadoFactor(resultadoFactor);
                }
            }
            resultado.calcularPuntaje();
            resultado.cumpleMinimo();
            cuestionario.agregarResultado(resultado);
        }
    }

    private void validarCuestionarioPersistido(Cuestionario cuestionario) {
        if (cuestionario == null || cuestionario.getId() == null
                || cuestionarioDAO.todos().stream().noneMatch(c -> c.getId().equals(cuestionario.getId()))) {
            throw new IllegalArgumentException("El cuestionario debe estar guardado antes de ejecutar CU026.");
        }
    }

    private void requerirDependenciasCU026() {
        if (configuracionDAO == null || factorDAO == null || preguntaDAO == null) {
            throw new IllegalStateException("CU026 requiere crear GestorCuestionarios con sus DAO de ejecución.");
        }
    }

    /** Aplica la configuración vigente antes de cada avance, guardado o cierre de CU026. */
    private boolean marcarVencidoSiCorresponde(Cuestionario cuestionario) {
        return marcarVencidoSiCorresponde(cuestionario, configuracionDAO.obtenerParametros());
    }

    private boolean marcarVencidoSiCorresponde(Cuestionario cuestionario,
                                                ConfiguracionSistema configuracion) {
        if (!cuestionario.validarTiempoMaximo(configuracion.getTiempoMaximoMinutos())) {
            return false;
        }
        EstadoCuestionario estadoFinal = cuestionario.getEstado() == EstadoCuestionario.EN_CURSO
                ? EstadoCuestionario.INCOMPLETO : EstadoCuestionario.SIN_CONTESTAR;
        cuestionario.cambiarEstado(estadoFinal);
        cuestionarioDAO.actualizarCuestionario(cuestionario);
        return true;
    }

    private int valor(Integer numero) { return numero == null ? 0 : numero; }

    private void validarSeleccion(List<Candidato> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) throw new IllegalArgumentException("Debe seleccionar al menos un candidato.");
        Set<Integer> numeros = new HashSet<>();
        for (Candidato candidato : candidatos) {
            if (candidato == null || candidato.getNroCandidato() == null) throw new IllegalArgumentException("La lista contiene un candidato inválido.");
            if (!numeros.add(candidato.getNroCandidato())) throw new IllegalArgumentException("La selección contiene candidatos duplicados.");
        }
    }

    private Integer crearClave(Set<Integer> clavesNuevas) {
        Integer clave;
        do {
            clave = 10_000_000 + random.nextInt(90_000_000);
        } while (clavesNuevas.contains(clave) || clavesEmitidas.contains(clave)
                || cuestionarioDAO.existeClave(clave.toString()));
        clavesNuevas.add(clave);
        clavesEmitidas.add(clave);
        return clave;
    }

    private String normalizarNombreArchivo(String nombre) {
        String limpio = nombre == null ? "Puesto" : nombre.trim();
        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        limpio = limpio.replaceAll("[^A-Za-z0-9._ -]", "_").replaceAll("\\s+", "-");
        return limpio.isBlank() ? "Puesto" : limpio;
    }
}
