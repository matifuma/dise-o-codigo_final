import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.IdentityHashMap;
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

    /** Actualiza una entidad que ya fue guardada por CU025. */
    public synchronized void actualizarCuestionario(Cuestionario cuestionario) {
        validar(cuestionario);
        if (cuestionario.getId() == null || !cuestionarios.containsKey(cuestionario.getId())) {
            throw new IllegalArgumentException("No existe el cuestionario que se quiere actualizar.");
        }
        cuestionarios.put(cuestionario.getId(), cuestionario);
        cuestionario.getEvaluacion().agregarCuestionario(cuestionario);
    }

    /** Devuelve el cuestionario abierto del candidato, si existe. */
    public synchronized Cuestionario obtenerCuestionarioActivo(String tipoDoc, String nroDoc) {
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

    /** Devuelve los cuestionarios finalizados asociados a cualquier evaluación del puesto. */
    public synchronized List<Cuestionario> buscarCuestionariosCompletos(String codigoPuesto) {
        validarCodigoPuesto(codigoPuesto);
        List<Cuestionario> resultado = new ArrayList<>();
        for (Cuestionario cuestionario : cuestionarios.values()) {
            if (perteneceAlPuesto(cuestionario, codigoPuesto)
                    && cuestionario.getEstado() == EstadoCuestionario.COMPLETADO) {
                resultado.add(cuestionario);
            }
        }
        return resultado;
    }

    /**
     * Recupera todos los cuestionarios de las evaluaciones seleccionadas para
     * que GestorReportes pueda clasificarlos por estado y puntaje.
     */
    public synchronized List<Cuestionario> buscarCuestionariosPorEvaluaciones(List<Long> idsEvaluaciones) {
        if (idsEvaluaciones == null || idsEvaluaciones.isEmpty()) return List.of();
        java.util.Set<Long> ids = new java.util.HashSet<>(idsEvaluaciones);
        List<Cuestionario> resultado = new ArrayList<>();
        for (Cuestionario cuestionario : cuestionarios.values()) {
            Evaluacion evaluacion = cuestionario.getEvaluacion();
            if (evaluacion != null && evaluacion.getId() != null && ids.contains(evaluacion.getId())) {
                resultado.add(cuestionario);
            }
        }
        return resultado;
    }

    /** Cuenta candidatos distintos que tienen un cuestionario asociado al puesto. */
    public synchronized Integer contarCandidatosPorPuesto(String codigoPuesto) {
        validarCodigoPuesto(codigoPuesto);
        Set<Integer> numerosCandidato = new java.util.HashSet<>();
        Set<String> documentos = new java.util.HashSet<>();
        Set<Candidato> sinIdentificador = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        for (Cuestionario cuestionario : cuestionarios.values()) {
            if (!perteneceAlPuesto(cuestionario, codigoPuesto)) continue;
            Candidato candidato = cuestionario.getCandidato();
            if (candidato == null) continue;
            if (candidato.getNroCandidato() != null) {
                numerosCandidato.add(candidato.getNroCandidato());
            } else if (candidato.getTipoDocumento() != null || candidato.getNroDocumento() != null) {
                documentos.add(String.valueOf(candidato.getTipoDocumento()) + ":" + candidato.getNroDocumento());
            } else {
                sinIdentificador.add(candidato);
            }
        }
        return numerosCandidato.size() + documentos.size() + sinIdentificador.size();
    }

    /**
     * Cuenta cuestionarios en estado COMPLETADO para las evaluaciones del puesto.
     * El diagrama puede rotular el valor como «evaluaciones completas» en la tabla.
     */
    public synchronized Integer contarEvaluacionesCompletasPorPuesto(String codigoPuesto) {
        validarCodigoPuesto(codigoPuesto);
        int cantidad = 0;
        for (Cuestionario cuestionario : cuestionarios.values()) {
            if (perteneceAlPuesto(cuestionario, codigoPuesto)
                    && cuestionario.getEstado() == EstadoCuestionario.COMPLETADO) {
                cantidad++;
            }
        }
        return cantidad;
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

    private boolean perteneceAlPuesto(Cuestionario cuestionario, String codigoPuesto) {
        Puesto puesto = cuestionario.getPuesto();
        return puesto != null && codigoPuesto.trim().equals(puesto.getCodigo());
    }

    private void validarCodigoPuesto(String codigoPuesto) {
        if (codigoPuesto == null || codigoPuesto.trim().isEmpty()) {
            throw new IllegalArgumentException("El código del puesto es obligatorio.");
        }
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
