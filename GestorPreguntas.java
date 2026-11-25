import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Controlador de gestión de preguntas (CU020 y altas invocadas por CU021). */
public class GestorPreguntas {
    private final PreguntaDAO preguntaDAO;
    private final IProveedorIA proveedorIA;
    private final ServicioAuditoria servicioAuditoria;
    private final String usuarioActual;

    public GestorPreguntas(PreguntaDAO preguntaDAO) {
        this(preguntaDAO, null, null, null);
    }

    public GestorPreguntas(PreguntaDAO preguntaDAO, IProveedorIA proveedorIA) {
        this(preguntaDAO, proveedorIA, null, null);
    }

    public GestorPreguntas(PreguntaDAO preguntaDAO, IProveedorIA proveedorIA,
                           ServicioAuditoria servicioAuditoria, String usuarioActual) {
        if (preguntaDAO == null) {
            throw new IllegalArgumentException("PreguntaDAO es obligatorio.");
        }
        this.preguntaDAO = preguntaDAO;
        this.proveedorIA = proveedorIA;
        this.servicioAuditoria = servicioAuditoria;
        this.usuarioActual = usuarioActual;
    }

    /** Busca por uno o más criterios; el nombre utiliza coincidencia "contiene". */
    public List<Pregunta> buscarPregunta(Competencia competencia, Factor factor,
                                         String nombrePregunta) {
        boolean filtroNombre = nombrePregunta != null && !nombrePregunta.trim().isEmpty();
        if (competencia == null && factor == null && !filtroNombre) {
            throw new IllegalArgumentException("Ingrese al menos un criterio de búsqueda.");
        }

        if (competencia != null && !Boolean.TRUE.equals(competencia.getActivo())) {
            throw new IllegalArgumentException("La competencia seleccionada no está activa.");
        }
        if (factor != null && !Boolean.TRUE.equals(factor.getActivo())) {
            throw new IllegalArgumentException("El factor seleccionado no está activo.");
        }
        if (competencia != null && factor != null
                && (factor.getCompetencia() == null
                || !Objects.equals(factor.getCompetencia().getId(), competencia.getId()))) {
            throw new IllegalArgumentException("El factor no pertenece a la competencia seleccionada.");
        }

        return preguntaDAO.buscarPregunta(competencia, factor, nombrePregunta);
    }

    /** Alta definida por el diagrama de clases; las opciones quedan asociadas a Pregunta. */
    public void darDeAltaPregunta(Competencia competencia, Factor factor, String nombre,
                                  String enunciado, String descripcion,
                                  List<OpcionRespuesta> opciones) {
        validarCompetenciaYFactor(competencia, factor);
        validarDatosPregunta(nombre, enunciado, opciones);
        validarOpciones(opciones);

        Set<String> camposDuplicados = camposDuplicados(competencia, factor, null, nombre, enunciado);
        if (!camposDuplicados.isEmpty()) {
            throw new IllegalArgumentException("Ya existe una pregunta con el mismo "
                    + String.join(" y ", camposDuplicados) + " para la competencia y el factor.");
        }

        Pregunta pregunta = new Pregunta(
                null,
                nombre.trim().toUpperCase(Locale.ROOT),
                enunciado.trim().toUpperCase(Locale.ROOT),
                descripcion == null ? "" : descripcion.trim().toUpperCase(Locale.ROOT),
                true,
                inferirTipoPregunta(opciones),
                competencia,
                factor,
                opciones
        );
        preguntaDAO.crearPregunta(pregunta);
    }

    /** Modifica los datos editables definidos en CU022. */
    public void modificarPregunta(Pregunta pregunta, String nombre, String enunciado,
                                  String descripcion, List<OpcionRespuesta> opciones) {
        validarPreguntaExistente(pregunta);
        validarDatosPregunta(nombre, enunciado, opciones);
        validarOpciones(opciones);

        Set<String> camposDuplicados = camposDuplicados(pregunta.getCompetencia(), pregunta.getFactor(),
                pregunta.getId(), nombre, enunciado);
        if (!camposDuplicados.isEmpty()) {
            throw new IllegalArgumentException("Ya existe otra pregunta con el mismo "
                    + String.join(" y ", camposDuplicados) + " para la competencia y el factor.");
        }

        pregunta.setNombre(nombre.trim().toUpperCase(Locale.ROOT));
        pregunta.setEnunciado(enunciado.trim().toUpperCase(Locale.ROOT));
        pregunta.setDescripcion(descripcion == null ? "" : descripcion.trim().toUpperCase(Locale.ROOT));
        pregunta.setTipo(inferirTipoPregunta(opciones));
        pregunta.reemplazarOpciones(opciones);
        preguntaDAO.modificarPregunta(pregunta);
    }

    /** Baja lógica según CU023 y el atributo activo de Pregunta. */
    public void darDeBajaPregunta(Pregunta pregunta) {
        validarPreguntaExistente(pregunta);
        if (servicioAuditoria == null || usuarioActual == null || usuarioActual.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Para dar de baja una pregunta debe configurarse ServicioAuditoria y el usuario actual.");
        }
        String entidad = "Pregunta " + pregunta.getNombre() + " (id " + pregunta.getId() + ")";
        servicioAuditoria.registrarAuditoria("Baja lógica de pregunta", entidad, usuarioActual);
        preguntaDAO.eliminarPregunta(pregunta.getId());
    }

    public void generarPreguntaIA(Competencia competencia, Factor factor) {
        validarCompetenciaYFactor(competencia, factor);
        // TODO: el diagrama marca este método como void, aunque CU021 necesita
        // devolver datos a la UI. Falta acordar el retorno y conectar IProveedorIA.
        if (proveedorIA == null) {
            throw new UnsupportedOperationException("Pendiente configurar IProveedorIA.");
        }
        throw new UnsupportedOperationException("Pendiente definir y aplicar el resultado generado.");
    }

    private void validarCompetenciaYFactor(Competencia competencia, Factor factor) {
        if (competencia == null || factor == null) {
            throw new IllegalArgumentException("Debe seleccionar una competencia y un factor.");
        }
        if (competencia.getId() == null || factor.getId() == null) {
            throw new IllegalArgumentException("La competencia y el factor deben existir en el sistema.");
        }
        if (factor.getCompetencia() == null
                || !Objects.equals(factor.getCompetencia().getId(), competencia.getId())) {
            throw new IllegalArgumentException("El factor no pertenece a la competencia seleccionada.");
        }
        if (!Boolean.TRUE.equals(competencia.getActivo()) || !Boolean.TRUE.equals(factor.getActivo())) {
            throw new IllegalArgumentException("La competencia y el factor deben estar activos.");
        }
    }

    private void validarPreguntaExistente(Pregunta pregunta) {
        if (pregunta == null || pregunta.getId() == null || pregunta.getFactor() == null) {
            throw new IllegalArgumentException("Debe seleccionar una pregunta válida.");
        }
        if (!Boolean.TRUE.equals(pregunta.getActivo())) {
            throw new IllegalArgumentException("La pregunta seleccionada no está activa.");
        }
        boolean registrada = preguntaDAO.obtenerPreguntasPorFactor(pregunta.getFactor().getId()).stream()
                .anyMatch(registro -> Objects.equals(registro.getId(), pregunta.getId()));
        if (!registrada) {
            throw new IllegalArgumentException("La pregunta seleccionada no está registrada.");
        }
    }

    private void validarDatosPregunta(String nombre, String enunciado,
                                      List<OpcionRespuesta> opciones) {
        List<String> omisiones = new ArrayList<>();
        if (nombre == null || nombre.trim().isEmpty()) {
            omisiones.add("Nombre de Pregunta");
        }
        if (enunciado == null || enunciado.trim().isEmpty()) {
            omisiones.add("Pregunta / enunciado");
        }
        if (opciones == null || opciones.isEmpty()) {
            omisiones.add("Opción de Respuesta");
        }
        if (!omisiones.isEmpty()) {
            throw new IllegalArgumentException("Faltan datos obligatorios: " + String.join(", ", omisiones));
        }
    }

    private void validarOpciones(List<OpcionRespuesta> opciones) {
        if (opciones == null || opciones.isEmpty()) {
            throw new IllegalArgumentException("La pregunta debe tener al menos una opción de respuesta.");
        }
        if (opciones.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("La lista no puede contener opciones nulas.");
        }
        double suma = 0.0;
        for (OpcionRespuesta opcion : opciones) {
            if (opcion.getId() == null || !Boolean.TRUE.equals(opcion.getActivo())) {
                throw new IllegalArgumentException("Las opciones deben estar cargadas y activas en el sistema.");
            }
            if (opcion.getPuntaje() == null || opcion.getPuntaje() < 0.0) {
                throw new IllegalArgumentException("Las ponderaciones deben ser cero o positivas.");
            }
            suma += opcion.getPuntaje();
        }
        if (Math.abs(suma - 10.0) > 0.000001) {
            throw new IllegalArgumentException("Las ponderaciones de las opciones deben sumar 10 puntos.");
        }
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> camposDuplicados(Competencia competencia, Factor factor, Long idExcluido,
                                         String nombre, String enunciado) {
        String nombreNormalizado = normalizar(nombre);
        String enunciadoNormalizado = normalizar(enunciado);
        Set<String> resultado = new LinkedHashSet<>();
        for (Pregunta existente : preguntaDAO.obtenerPreguntasPorFactor(factor.getId())) {
            if (!Boolean.TRUE.equals(existente.getActivo())
                    || Objects.equals(existente.getId(), idExcluido)
                    || existente.getCompetencia() == null
                    || !Objects.equals(existente.getCompetencia().getId(), competencia.getId())) {
                continue;
            }
            if (normalizar(existente.getNombre()).equals(nombreNormalizado)) {
                resultado.add("nombre");
            }
            if (normalizar(existente.getEnunciado()).equals(enunciadoNormalizado)) {
                resultado.add("enunciado");
            }
        }
        return resultado;
    }

    /**
     * El método UML de alta no recibe TipoPregunta. Se infiere a partir de las
     * opciones hasta que el contrato del diagrama especifique cómo seleccionarlo.
     */
    private TipoPregunta inferirTipoPregunta(List<OpcionRespuesta> opciones) {
        if (opciones.size() == 2) {
            String primero = normalizar(opciones.get(0).getTexto());
            String segundo = normalizar(opciones.get(1).getTexto());
            boolean verdaderoFalso = (primero.equals("verdadero") && segundo.equals("falso"))
                    || (primero.equals("falso") && segundo.equals("verdadero"))
                    || (primero.equals("true") && segundo.equals("false"))
                    || (primero.equals("false") && segundo.equals("true"));
            if (verdaderoFalso) {
                return TipoPregunta.VERDADERO_FALSO;
            }
        }

        boolean todasNumericas = opciones.stream().allMatch(opcion -> {
            try {
                Double.parseDouble(opcion.getTexto().trim());
                return true;
            } catch (RuntimeException excepcion) {
                return false;
            }
        });
        if (opciones.size() >= 3 && todasNumericas) {
            return TipoPregunta.ESCALA;
        }
        return TipoPregunta.OPCION_MULTIPLE;
    }
}
