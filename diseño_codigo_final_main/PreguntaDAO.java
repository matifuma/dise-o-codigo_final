import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DAO temporal en memoria. Conserva las operaciones del diagrama; el proyecto
 * todavía no define motor, esquema ni conexión de base de datos.
 */
public class PreguntaDAO {
    private final Map<Long, Pregunta> preguntas = new LinkedHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1L);

    public synchronized void crearPregunta(Pregunta pregunta) {
        if (pregunta == null) {
            throw new IllegalArgumentException("La pregunta es obligatoria.");
        }

        if (pregunta.getId() == null) {
            pregunta.setId(siguienteId.getAndIncrement());
        } else if (preguntas.containsKey(pregunta.getId())) {
            throw new IllegalArgumentException("Ya existe una pregunta con ese id.");
        } else {
            siguienteId.updateAndGet(actual -> Math.max(actual, pregunta.getId() + 1));
        }

        preguntas.put(pregunta.getId(), pregunta);
    }

    public synchronized void modificarPregunta(Pregunta pregunta) {
        if (pregunta == null || pregunta.getId() == null) {
            throw new IllegalArgumentException("La pregunta debe tener un id.");
        }
        if (!preguntas.containsKey(pregunta.getId())) {
            throw new IllegalArgumentException("No existe la pregunta que se quiere modificar.");
        }
        preguntas.put(pregunta.getId(), pregunta);
    }

    /**
     * CU023 es una baja lógica: se conserva el registro y se cambia su estado
     * activo, según el atributo activo de Pregunta.
     */
    public synchronized void eliminarPregunta(Long id) {
        Pregunta pregunta = preguntas.get(id);
        if (pregunta == null) {
            throw new IllegalArgumentException("No existe la pregunta que se quiere dar de baja.");
        }
        pregunta.setActivo(false);
    }

    public synchronized List<Pregunta> obtenerPreguntasPorFactor(Long idFactor) {
        List<Pregunta> resultado = new ArrayList<>();
        for (Pregunta pregunta : preguntas.values()) {
            if (pregunta.getFactor() != null
                    && idFactor != null
                    && idFactor.equals(pregunta.getFactor().getId())) {
                resultado.add(pregunta);
            }
        }
        return resultado;
    }

    /**
     * Búsqueda usada por CU020. Está en el diagrama de secuencia, aunque no
     * aparece entre las operaciones de PreguntaDAO en el diagrama de clases.
     */
    public synchronized List<Pregunta> buscarPregunta(Competencia competencia, Factor factor,
                                                       String nombrePregunta) {
        String filtro = nombrePregunta == null ? "" : nombrePregunta.trim().toLowerCase(Locale.ROOT);
        List<Pregunta> resultado = new ArrayList<>();
        for (Pregunta pregunta : preguntas.values()) {
            if (!Boolean.TRUE.equals(pregunta.getActivo())) {
                continue;
            }
            if (competencia != null && (pregunta.getCompetencia() == null
                    || !Objects.equals(pregunta.getCompetencia().getId(), competencia.getId()))) {
                continue;
            }
            if (factor != null && (pregunta.getFactor() == null
                    || !Objects.equals(pregunta.getFactor().getId(), factor.getId()))) {
                continue;
            }
            if (!filtro.isEmpty() && !normalizar(pregunta.getNombre()).contains(filtro)) {
                continue;
            }
            resultado.add(pregunta);
        }
        return resultado;
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
