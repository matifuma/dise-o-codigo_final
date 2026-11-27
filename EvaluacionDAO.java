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

    /** TODO fuera de CU025: consulta de evaluaciones para reportes del puesto. */
    public synchronized List<Evaluacion> obtenerEvaluacionesPorPuesto(String codigoPuesto) {
        throw new UnsupportedOperationException("Pendiente implementar la consulta de evaluaciones por puesto.");
    }

    synchronized List<Evaluacion> todos() { return new ArrayList<>(evaluaciones.values()); }

    synchronized void eliminar(Evaluacion evaluacion) {
        if (evaluacion != null && evaluacion.getId() != null) evaluaciones.remove(evaluacion.getId());
    }
}
