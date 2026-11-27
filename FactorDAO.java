import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** DAO en memoria para factores. */
public class FactorDAO {
    private final Map<Long, Factor> factores = new LinkedHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1L);

    public FactorDAO() { this(List.of()); }

    /** Carga inicial de factores en memoria; la fuente definitiva se definirá con la persistencia. */
    public FactorDAO(Collection<Factor> iniciales) {
        if (iniciales == null) return;
        for (Factor factor : iniciales) {
            validar(factor);
            if (factor.getId() == null) factor.setId(siguienteId.getAndIncrement());
            else siguienteId.updateAndGet(actual -> Math.max(actual, factor.getId() + 1));
            if (factores.putIfAbsent(factor.getId(), factor) != null) {
                throw new IllegalArgumentException("Hay identificadores de factor repetidos.");
            }
        }
    }

    /** TODO fuera de CU025: alta de factores. */
    public synchronized void crearFactor(Factor factor) {
        throw new UnsupportedOperationException("Pendiente implementar la persistencia de factores.");
    }

    /** TODO fuera de CU025: modificación de factores. */
    public synchronized void modificarFactor(Factor factor) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de factores.");
    }

    /** TODO fuera de CU025: baja de factores. */
    public synchronized void eliminarFactor(Long id) {
        throw new UnsupportedOperationException("Pendiente implementar la baja de factores.");
    }

    public synchronized List<Factor> obtenerFactoresPorCompetencia(Long idCompetencia) {
        List<Factor> resultado = new ArrayList<>();
        for (Factor factor : factores.values()) {
            if (factor.getCompetencia() != null && idCompetencia != null
                    && idCompetencia.equals(factor.getCompetencia().getId())) resultado.add(factor);
        }
        return resultado;
    }

    private void validar(Factor factor) {
        if (factor == null || factor.getCompetencia() == null) throw new IllegalArgumentException("El factor y su competencia son obligatorios.");
    }
}
