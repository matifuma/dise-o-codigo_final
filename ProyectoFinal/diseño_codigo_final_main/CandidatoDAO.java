import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** DAO en memoria para el alcance actual; debe sustituirse por persistencia real. */
public class CandidatoDAO {
    private final Map<Integer, Candidato> candidatos = new LinkedHashMap<>();

    public CandidatoDAO() { this(List.of()); }

    /** Carga inicial de datos usada por el adaptador en memoria; la persistencia real aún no existe. */
    public CandidatoDAO(Collection<Candidato> candidatosIniciales) {
        if (candidatosIniciales == null) return;
        for (Candidato candidato : candidatosIniciales) {
            validarClave(candidato);
            if (candidatos.putIfAbsent(candidato.getNroCandidato(), candidato) != null) {
                throw new IllegalArgumentException("Hay números de candidato repetidos en la carga inicial.");
            }
        }
    }

    /** TODO fuera de CU025: persistir altas en el repositorio definitivo. */
    public synchronized void crearCandidato(Candidato candidato) {
        throw new UnsupportedOperationException("Pendiente implementar la persistencia de candidatos.");
    }

    /** TODO fuera de CU025: persistir modificaciones en el repositorio definitivo. */
    public synchronized void modificarCandidato(Candidato candidato) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de candidatos.");
    }

    /** TODO fuera de CU025: definir el acceso por tipo y número de documento en el repositorio. */
    public synchronized Candidato obtenerCandidato(String tipoDoc, String nroDoc) {
        throw new UnsupportedOperationException("Pendiente implementar la consulta de un candidato.");
    }

    /** Operación del diagrama: devuelve candidatos activos y no eliminados de la selección. */
    public synchronized List<Candidato> buscarActivos(List<Candidato> candidatosAEvaluar) {
        if (candidatosAEvaluar == null) return List.of();
        List<Candidato> resultado = new ArrayList<>();
        for (Candidato candidato : candidatosAEvaluar) {
            Candidato registrado = candidato == null ? null : candidatos.get(candidato.getNroCandidato());
            if (registrado != null && Boolean.TRUE.equals(registrado.getActivo())
                    && !Boolean.TRUE.equals(registrado.getEliminado())) {
                resultado.add(registrado);
            }
        }
        return resultado;
    }

    /** Búsqueda por los tres criterios que presenta la pantalla de CU025. */
    public synchronized List<Candidato> buscarCandidatos(String apellido, String nombre,
                                                          Integer nroCandidato) {
        List<Candidato> resultado = new ArrayList<>();
        for (Candidato candidato : candidatos.values()) {
            if (!Boolean.TRUE.equals(candidato.getActivo())
                    || Boolean.TRUE.equals(candidato.getEliminado())) continue;
            if (!contiene(candidato.getApellido(), apellido)
                    || !contiene(candidato.getNombre(), nombre)) continue;
            if (nroCandidato != null && !nroCandidato.equals(candidato.getNroCandidato())) continue;
            resultado.add(candidato);
        }
        return resultado;
    }

    private boolean contiene(String valor, String filtro) {
        return filtro == null || filtro.trim().isEmpty()
                || normalizar(valor).contains(normalizar(filtro));
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private void validarClave(Candidato candidato) {
        if (candidato == null || candidato.getNroCandidato() == null) {
            throw new IllegalArgumentException("El candidato y su número son obligatorios.");
        }
    }
}
