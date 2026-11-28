import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** DAO en memoria para evaluaciones. */
public class EvaluacionDAO {
    private final Map<Long, Evaluacion> evaluaciones = new LinkedHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1L);

    public synchronized void guardar(Evaluacion evaluacion) {
        if (evaluacion == null) throw new IllegalArgumentException("La evaluación es obligatoria.");
        if (evaluacion.getId() == null) evaluacion.setId(siguienteId.getAndIncrement());
        else if (evaluaciones.containsKey(evaluacion.getId())) throw new IllegalArgumentException("La evaluación ya existe.");
        evaluaciones.put(evaluacion.getId(), evaluacion);
    }

    /** Consulta las evaluaciones asociadas a un puesto y devuelve sus datos de presentación. */
    public synchronized List<EvaluacionDTO> obtenerEvaluacionesPorPuesto(String codigoPuesto) {
        if (codigoPuesto == null || codigoPuesto.trim().isEmpty()) {
            throw new IllegalArgumentException("El código del puesto es obligatorio.");
        }
        List<EvaluacionDTO> resultado = new ArrayList<>();
        for (Evaluacion evaluacion : evaluaciones.values()) {
            Puesto puesto = evaluacion.getPuesto();
            if (puesto != null && codigoPuesto.trim().equals(puesto.getCodigo())) {
                resultado.add(new EvaluacionDTO(evaluacion.getId(), evaluacion.getFecha(), puesto.getNombre()));
            }
        }
        return resultado;
    }

    /** Consulta de entidades para las pantallas existentes que navegan cuestionarios por evaluación. */
    public synchronized List<Evaluacion> obtenerEvaluacionesEntidadPorPuesto(String codigoPuesto) {
        if (codigoPuesto == null || codigoPuesto.trim().isEmpty()) {
            throw new IllegalArgumentException("El código del puesto es obligatorio.");
        }
        List<Evaluacion> resultado = new ArrayList<>();
        for (Evaluacion evaluacion : evaluaciones.values()) {
            Puesto puesto = evaluacion.getPuesto();
            if (puesto != null && codigoPuesto.trim().equals(puesto.getCodigo())) {
                resultado.add(evaluacion);
            }
        }
        return resultado;
    }

    synchronized List<Evaluacion> todos() { return new ArrayList<>(evaluaciones.values()); }

    synchronized void eliminar(Evaluacion evaluacion) {
        if (evaluacion != null && evaluacion.getId() != null) evaluaciones.remove(evaluacion.getId());
    }
}
