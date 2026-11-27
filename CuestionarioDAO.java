import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** DAO en memoria para las operaciones de CU025; pendiente persistencia transaccional real. */
public class CuestionarioDAO {
    private final Map<Long, Cuestionario> cuestionarios = new LinkedHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1L);

    public synchronized void guardar(Cuestionario cuestionario) {
        validar(cuestionario);
        if (cuestionarios.values().stream().anyMatch(c -> c.getClave().equals(cuestionario.getClave()))) {
            throw new IllegalArgumentException("La clave ya está asignada a otro cuestionario.");
        }
        if (cuestionario.getId() == null) cuestionario.setId(siguienteId.getAndIncrement());
        else if (cuestionarios.containsKey(cuestionario.getId())) throw new IllegalArgumentException("El cuestionario ya existe.");
        cuestionarios.put(cuestionario.getId(), cuestionario);
        cuestionario.getEvaluacion().agregarCuestionario(cuestionario);
    }

    /** TODO fuera de CU025: guardar cambios del flujo de respuestas del candidato. */
    public synchronized void actualizarCuestionario(Cuestionario cuestionario) {
        throw new UnsupportedOperationException("Pendiente implementar la actualización del cuestionario.");
    }

    /** Devuelve el cuestionario abierto del candidato, si existe. */
    public synchronized Cuestionario obtenerCuestionariosActivo(String tipoDoc, String nroDoc) {
        for (Cuestionario cuestionario : cuestionarios.values()) {
            Candidato candidato = cuestionario.getCandidato();
            if (candidato != null
                    && Objects.equals(candidato.getTipoDocumento(), tipoDoc)
                    && Objects.equals(candidato.getNroDocumento(), nroDoc)
                    && estadoBloqueante(cuestionario.getEstado())) {
                return cuestionario;
            }
        }
        return null;
    }

    /** TODO fuera de CU025: falta implementar la consulta de resultados completos por puesto. */
    public synchronized List<Cuestionario> buscarCuestionariosCompletos(String codigoPuesto) {
        throw new UnsupportedOperationException("Pendiente implementar la búsqueda de cuestionarios completos.");
    }

    synchronized boolean existeClave(String clave) {
        return cuestionarios.values().stream().anyMatch(c -> Objects.equals(c.getClave(), clave));
    }

    synchronized List<Cuestionario> todos() { return new ArrayList<>(cuestionarios.values()); }

    synchronized void eliminar(Cuestionario cuestionario) {
        if (cuestionario != null && cuestionario.getId() != null) {
            cuestionarios.remove(cuestionario.getId());
            if (cuestionario.getEvaluacion() != null) cuestionario.getEvaluacion().quitarCuestionario(cuestionario);
        }
    }

    private boolean estadoBloqueante(EstadoCuestionario estado) {
        return estado == EstadoCuestionario.ACTIVO || estado == EstadoCuestionario.EN_CURSO;
    }

    private void validar(Cuestionario cuestionario) {
        if (cuestionario == null || cuestionario.getCandidato() == null
                || cuestionario.getEvaluacion() == null || cuestionario.getClave() == null
                || cuestionario.getClave().length() != 8 || cuestionario.getEstado() == null) {
            throw new IllegalArgumentException("El cuestionario debe tener candidato, evaluación, estado y clave válida.");
        }
        if (cuestionario.getEvaluacion().getPuesto() == null) {
            throw new IllegalArgumentException("La evaluación debe estar asociada al puesto seleccionado.");
        }
    }
}
