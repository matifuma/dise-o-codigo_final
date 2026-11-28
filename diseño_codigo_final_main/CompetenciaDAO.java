import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** DAO en memoria del catálogo de competencias. */
public class CompetenciaDAO {
    private final Map<Long, Competencia> competencias = new LinkedHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1L);

    public CompetenciaDAO() { this(List.of()); }

    /** Carga inicial para pruebas/datos en memoria, sin reemplazar la futura persistencia real. */
    public CompetenciaDAO(Collection<Competencia> iniciales) {
        if (iniciales == null) return;
        for (Competencia competencia : iniciales) {
            validar(competencia);
            if (competencia.getId() == null) competencia.setId(siguienteId.getAndIncrement());
            else siguienteId.updateAndGet(actual -> Math.max(actual, competencia.getId() + 1));
            if (competencias.putIfAbsent(competencia.getId(), competencia) != null) {
                throw new IllegalArgumentException("Hay identificadores de competencia repetidos.");
            }
        }
    }

    /** TODO fuera de CU025: alta de competencias. */
    public synchronized void crearCompetencia(Competencia competencia) {
        throw new UnsupportedOperationException("Pendiente implementar la persistencia de competencias.");
    }

    /** TODO fuera de CU025: modificación de competencias. */
    public synchronized void modificarCompetencia(Competencia competencia) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de competencias.");
    }

    /** TODO fuera de CU025: definir baja lógica/física de competencias. */
    public synchronized void eliminarCompetencia(Long id) {
        throw new UnsupportedOperationException("Pendiente implementar la baja de competencias.");
    }

    /** TODO fuera de CU025: consulta individual por id. */
    public synchronized Competencia obtenerCompetencia(Long id) {
        throw new UnsupportedOperationException("Pendiente implementar la consulta de competencia por id.");
    }

    /** Búsqueda por nombre de la operación definida en el diagrama de clases. */
    public synchronized List<Competencia> buscarCompetencias(String nombre) {
        String filtro = nombre == null ? "" : nombre.trim().toLowerCase(java.util.Locale.ROOT);
        List<Competencia> resultado = new ArrayList<>();
        for (Competencia competencia : competencias.values()) {
            String nombreCompetencia = competencia.getNombre() == null
                    ? "" : competencia.getNombre().trim().toLowerCase(java.util.Locale.ROOT);
            if (filtro.isEmpty() || nombreCompetencia.contains(filtro)) resultado.add(competencia);
        }
        return resultado;
    }

    private void validar(Competencia competencia) {
        if (competencia == null) throw new IllegalArgumentException("La competencia es obligatoria.");
    }
}
