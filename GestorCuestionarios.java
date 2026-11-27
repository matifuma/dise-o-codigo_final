import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Orquesta la generación de claves, la creación de evaluaciones y la exportación de CU025. */
public class GestorCuestionarios {
    private static final char[] CARACTERES_CLAVE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");

    private final CandidatoDAO candidatoDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final EvaluacionDAO evaluacionDAO;
    private final ExportadorExcel exportadorExcel;
    private final SecureRandom random = new SecureRandom();
    private final Map<Integer, String> clavesPorCandidato = new HashMap<>();
    private final Set<String> clavesEmitidas = new HashSet<>();

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

    /** Operación del diagrama de clases: genera y conserva claves para candidatos seleccionados. */
    public synchronized void generarClavesAccesos(List<Candidato> candidatos) {
        generarDTOs(candidatos);
    }

    /** Mensaje de la secuencia corregida; devuelve los datos para la pantalla de confirmación. */
    public synchronized List<CandidatoConClaveDTO> generarClavesAcceso(List<Candidato> candidatos) {
        return generarDTOs(candidatos);
    }

    /** Firma de integración con la UI, que recibe los DTO devueltos por GestorCandidatos. */
    public synchronized List<CandidatoConClaveDTO> generarClavesAcceso(Collection<CandidatoDTO> candidatosDTO) {
        if (candidatosDTO == null || candidatosDTO.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un candidato.");
        }
        List<Candidato> candidatos = new ArrayList<>();
        for (CandidatoDTO dto : candidatosDTO) {
            if (dto == null || dto.getNroCandidato() == null) throw new IllegalArgumentException("Hay un candidato inválido.");
            List<Candidato> encontrado = candidatoDAO.buscarCandidatos(null, null, dto.getNroCandidato());
            if (encontrado.size() != 1) throw new IllegalArgumentException("El candidato ya no está disponible.");
            candidatos.add(encontrado.get(0));
        }
        return generarDTOs(candidatos);
    }

    private List<CandidatoConClaveDTO> generarDTOs(List<Candidato> candidatos) {
        validarSeleccion(candidatos);
        List<Candidato> activos = candidatoDAO.buscarActivos(candidatos);
        if (activos.size() != candidatos.size()) {
            throw new IllegalArgumentException("Todos los candidatos deben estar activos y registrados.");
        }
        Map<Integer, Candidato> candidatosCanonicos = new HashMap<>();
        for (Candidato candidato : activos) candidatosCanonicos.put(candidato.getNroCandidato(), candidato);
        List<CandidatoConClaveDTO> resultado = new ArrayList<>();
        Set<String> clavesNuevas = new HashSet<>();
        for (Candidato candidato : candidatos) {
            Candidato canonico = candidatosCanonicos.get(candidato.getNroCandidato());
            String clave = crearClave(clavesNuevas);
            clavesPorCandidato.put(canonico.getNroCandidato(), clave);
            resultado.add(new CandidatoConClaveDTO(canonico, clave));
        }
        return resultado;
    }

    /** Firma definida en el diagrama de clases; conserva las claves generadas por candidato. */
    public synchronized void evaluarCandidatos(List<Candidato> candidatos, Puesto puesto) {
        validarSeleccion(candidatos);
        List<CandidatoConClaveDTO> candidatosConClave = new ArrayList<>();
        for (Candidato candidato : candidatos) {
            String clave = clavesPorCandidato.get(candidato.getNroCandidato());
            if (clave == null) throw new IllegalStateException("Genere las claves antes de confirmar la evaluación.");
            candidatosConClave.add(new CandidatoConClaveDTO(candidato, clave));
        }
        crearEvaluacion(candidatosConClave, puesto);
    }

    /** Firma de la secuencia: recibe la lista presentada y confirmada por la UI. */
    public synchronized void evaluarCandidatos(Collection<CandidatoConClaveDTO> candidatosConClave,
                                                Puesto puesto) {
        if (candidatosConClave == null) throw new IllegalArgumentException("La lista de candidatos es obligatoria.");
        crearEvaluacion(new ArrayList<>(candidatosConClave), puesto);
    }

    private void crearEvaluacion(List<CandidatoConClaveDTO> seleccion, Puesto puesto) {
        if (puesto == null || !Boolean.TRUE.equals(puesto.getActivo())) {
            throw new IllegalArgumentException("Debe seleccionar un puesto activo.");
        }
        if (seleccion.isEmpty()) throw new IllegalArgumentException("Debe seleccionar al menos un candidato.");

        List<Candidato> candidatos = new ArrayList<>();
        Set<Integer> numeros = new LinkedHashSet<>();
        Set<String> claves = new HashSet<>();
        for (CandidatoConClaveDTO dto : seleccion) {
            if (dto == null || dto.getCandidato() == null || dto.getCandidato().getNroCandidato() == null
                    || dto.getClave() == null || dto.getClave().length() != 8) {
                throw new IllegalArgumentException("Cada candidato debe tener una clave válida de ocho caracteres.");
            }
            if (!numeros.add(dto.getCandidato().getNroCandidato())) {
                throw new IllegalArgumentException("La selección contiene un candidato duplicado.");
            }
            if (!claves.add(dto.getClave()) || cuestionarioDAO.existeClave(dto.getClave())) {
                throw new IllegalArgumentException("Las claves de acceso deben ser únicas.");
            }
            String generada = clavesPorCandidato.get(dto.getCandidato().getNroCandidato());
            if (generada == null || !generada.equals(dto.getClave())) {
                throw new IllegalArgumentException("La clave no corresponde a la selección generada por el sistema.");
            }
            candidatos.add(dto.getCandidato());
        }
        List<Candidato> activos = candidatoDAO.buscarActivos(candidatos);
        if (activos.size() != candidatos.size()) {
            throw new IllegalArgumentException("Todos los candidatos deben estar activos y registrados.");
        }
        Map<Integer, Candidato> candidatosCanonicos = new HashMap<>();
        for (Candidato candidato : activos) candidatosCanonicos.put(candidato.getNroCandidato(), candidato);
        List<CandidatoConClaveDTO> seleccionCanonica = new ArrayList<>();
        for (CandidatoConClaveDTO dto : seleccion) {
            Candidato candidato = candidatosCanonicos.get(dto.getCandidato().getNroCandidato());
            seleccionCanonica.add(new CandidatoConClaveDTO(candidato, dto.getClave()));
        }
        seleccion = seleccionCanonica;
        candidatos.clear();
        for (CandidatoConClaveDTO dto : seleccion) candidatos.add(dto.getCandidato());
        for (Candidato candidato : candidatos) {
            if (cuestionarioDAO.obtenerCuestionariosActivo(
                    candidato.getTipoDocumento(), candidato.getNroDocumento()) != null) {
                throw new IllegalStateException("Hay candidatos con cuestionarios activos o en curso.");
            }
        }

        // En una base real este bloque debe ejecutarse dentro de una transacción ACID.
        Evaluacion evaluacion = new Evaluacion(null, new java.util.Date(), puesto);
        List<Cuestionario> guardados = new ArrayList<>();
        try {
            evaluacionDAO.guardar(evaluacion);
            for (CandidatoConClaveDTO dto : seleccion) {
                Cuestionario cuestionario = new Cuestionario(
                        EstadoCuestionario.ACTIVO, dto.getClave(), dto.getCandidato(), evaluacion);
                cuestionarioDAO.guardar(cuestionario);
                guardados.add(cuestionario);
            }
        } catch (RuntimeException error) {
            for (Cuestionario cuestionario : guardados) cuestionarioDAO.eliminar(cuestionario);
            evaluacionDAO.eliminar(evaluacion);
            throw error;
        }
    }

    /** Exporta la lista de candidatos y claves generadas al nombre indicado por CU025. */
    public void exportarResultado(Puesto puesto, List<CandidatoConClaveDTO> resultado,
                                  String carpetaDestino) {
        if (puesto == null || resultado == null || carpetaDestino == null || carpetaDestino.trim().isEmpty()) {
            throw new IllegalArgumentException("Puesto, resultado y carpeta de destino son obligatorios.");
        }
        String nombrePuesto = normalizarNombreArchivo(puesto.getNombre());
        String nombre = FORMATO_ARCHIVO.format(LocalDateTime.now()) + "-" + nombrePuesto + ".xls";
        java.nio.file.Path archivo = java.nio.file.Paths.get(carpetaDestino).toAbsolutePath().normalize().resolve(nombre);
        exportadorExcel.exportarDatos(resultado, archivo.toString());
    }

    /** TODO fuera de CU025: falta cerrar autenticación, vigencia y control de intentos. */
    public Cuestionario iniciarCuestionario(String tipoDoc, String nroDoc, String clave) {
        throw new UnsupportedOperationException("Pendiente implementar el acceso del candidato.");
    }

    /** TODO fuera de CU025: requiere definir la selección de preguntas y la navegación por bloques. */
    public BloqueCuestionario obtenerBloquePendiente(Cuestionario cuestionario) {
        throw new UnsupportedOperationException("Pendiente implementar la obtención del bloque pendiente.");
    }

    /** TODO fuera de CU025: falta persistir respuestas y validar el bloque. */
    public void guardarBloqueCuestionario(BloqueCuestionario bloque) {
        throw new UnsupportedOperationException("Pendiente implementar el guardado del bloque.");
    }

    /** TODO fuera de CU025: definir el cálculo final de resultados. */
    public void finalizarCuestionario(Cuestionario cuestionario) {
        throw new UnsupportedOperationException("Pendiente implementar la finalización del cuestionario.");
    }

    /** TODO fuera de CU025: depende de configuración de vencimientos y tarea programada. */
    public void finalizarCuestionariosVencidos() {
        throw new UnsupportedOperationException("Pendiente implementar el cierre de cuestionarios vencidos.");
    }

    private void validarSeleccion(List<Candidato> candidatos) {
        if (candidatos == null || candidatos.isEmpty()) throw new IllegalArgumentException("Debe seleccionar al menos un candidato.");
        Set<Integer> numeros = new HashSet<>();
        for (Candidato candidato : candidatos) {
            if (candidato == null || candidato.getNroCandidato() == null) throw new IllegalArgumentException("La lista contiene un candidato inválido.");
            if (!numeros.add(candidato.getNroCandidato())) throw new IllegalArgumentException("La selección contiene candidatos duplicados.");
        }
    }

    private String crearClave(Set<String> clavesNuevas) {
        String clave;
        do {
            StringBuilder texto = new StringBuilder(8);
            for (int i = 0; i < 8; i++) texto.append(CARACTERES_CLAVE[random.nextInt(CARACTERES_CLAVE.length)]);
            clave = texto.toString();
        } while (!clave.matches(".*[A-Z].*") || !clave.matches(".*[0-9].*")
                || clavesNuevas.contains(clave) || clavesEmitidas.contains(clave)
                || cuestionarioDAO.existeClave(clave));
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
