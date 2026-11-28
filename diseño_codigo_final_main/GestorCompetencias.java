import java.util.ArrayList;
import java.util.List;

/** Consulta competencias requeridas y valida si pueden evaluarse en CU025. */
public class GestorCompetencias {
    private final CompetenciaDAO competenciaDAO;
    private final GestorFactores gestorFactor;
    private final GestorPreguntas gestorPreguntas;

    public GestorCompetencias(CompetenciaDAO competenciaDAO, GestorFactores gestorFactor,
                             GestorPreguntas gestorPreguntas) {
        if (competenciaDAO == null || gestorFactor == null || gestorPreguntas == null) {
            throw new IllegalArgumentException("Se requieren los gestores y DAO del catálogo de competencias.");
        }
        this.competenciaDAO = competenciaDAO;
        this.gestorFactor = gestorFactor;
        this.gestorPreguntas = gestorPreguntas;
    }

    public List<CompetenciaRequeridaDTO> buscarTodasLasCompetenciasPorPuesto(Puesto puestoSeleccionado) {
        if (puestoSeleccionado == null) throw new IllegalArgumentException("Debe seleccionar un puesto.");
        List<CompetenciaRequeridaDTO> resultado = new ArrayList<>();
        for (CompetenciaRequerida requerida : puestoSeleccionado.getCompetenciasRequeridas()) {
            Competencia asociada = requerida.getCompetencia();
            Competencia registrada = resolverCompetencia(asociada);
            resultado.add(new CompetenciaRequeridaDTO(
                    registrada.getId(), registrada.getNombre(), requerida.getPuntajeMinimo()));
        }
        return resultado;
    }

    /**
     * Una competencia es evaluable si todos sus factores activos tienen al menos dos preguntas
     * activas. Devuelve las asociaciones no evaluables para que la UI informe sus nombres y puntajes.
     */
    public List<CompetenciaRequeridaDTO> validarCompetencia(Puesto puesto) {
        if (puesto == null) throw new IllegalArgumentException("Debe seleccionar un puesto.");
        if (puesto.getCompetenciasRequeridas().isEmpty()) {
            throw new IllegalStateException("El puesto no tiene competencias requeridas configuradas.");
        }
        List<CompetenciaRequeridaDTO> noEvaluables = new ArrayList<>();
        for (CompetenciaRequerida requerida : puesto.getCompetenciasRequeridas()) {
            Competencia competencia = resolverCompetencia(requerida.getCompetencia());
            List<Factor> factoresActivos = gestorFactor.buscarFactor(competencia);
            boolean evaluable = Boolean.TRUE.equals(competencia.getActivo())
                    && !factoresActivos.isEmpty();
            for (Factor factor : factoresActivos) {
                if (gestorPreguntas.buscarPregunta(factor).size() < 2) {
                    evaluable = false;
                    break;
                }
            }
            if (!evaluable) noEvaluables.add(new CompetenciaRequeridaDTO(
                    competencia.getId(), competencia.getNombre(), requerida.getPuntajeMinimo()));
        }
        return noEvaluables;
    }

    private Competencia resolverCompetencia(Competencia asociada) {
        if (asociada == null) throw new IllegalArgumentException("El puesto contiene una competencia inválida.");
        List<Competencia> encontradas = competenciaDAO.buscarCompetencias(asociada.getNombre());
        for (Competencia encontrada : encontradas) {
            if (asociada.getId() != null && asociada.getId().equals(encontrada.getId())) return encontrada;
        }
        // La asociación Puesto-Competencia es la fuente del identificador y la ponderación.
        return asociada;
    }

    /** Para el selector del CU021 solo se muestran competencias activas. */
    public List<Competencia> buscarCompetencias(String nombre) {
        List<Competencia> resultado = new ArrayList<>();
        for (Competencia competencia : competenciaDAO.buscarCompetencias(nombre)) {
            if (Boolean.TRUE.equals(competencia.getActivo())) resultado.add(competencia);
        }
        return resultado;
    }

    /** Alias conservado para la pantalla JavaFX existente. */
    public List<Competencia> buscarCompetencia(String nombre) {
        return buscarCompetencias(nombre);
    }

    /** TODO: mantenimiento fuera del flujo CU025; reglas de alta pendientes de consensuar. */
    public void darDeAltaCompetencia(String codigo, String nombre, String descripcion) {
        throw new UnsupportedOperationException("Pendiente implementar el alta de competencias.");
    }

    /** TODO: mantenimiento fuera del flujo CU025. */
    public void modificarCompetencia(Competencia competencia, String nombre, String descripcion) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de competencias.");
    }

    /** TODO: mantenimiento fuera del flujo CU025; falta determinar política ante dependencias. */
    public void darDeBajaCompetencia(Competencia competencia) {
        throw new UnsupportedOperationException("Pendiente implementar la baja de competencias.");
    }
}
