import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/** Coordina las consultas y la emisión del orden de mérito del CU028. */
public class GestorReportes {
    private final EvaluacionDAO evaluacionDAO;
    private final CuestionarioDAO cuestionarioDAO;
    private final IExportadorArchivos exportadorCSV;

    public GestorReportes(EvaluacionDAO evaluacionDAO, CuestionarioDAO cuestionarioDAO,
                          IExportadorArchivos exportadorCSV) {
        if (evaluacionDAO == null || cuestionarioDAO == null || exportadorCSV == null) {
            throw new IllegalArgumentException("GestorReportes requiere ambos DAO y un exportador CSV.");
        }
        this.evaluacionDAO = evaluacionDAO;
        this.cuestionarioDAO = cuestionarioDAO;
        this.exportadorCSV = exportadorCSV;
    }

    /** Lista las evaluaciones del puesto seleccionado en el formato de la interfaz. */
    public List<EvaluacionDTO> listarEvaluacionesDelPuesto(String codigoPuesto) {
        return evaluacionDAO.obtenerEvaluacionesPorPuesto(codigoPuesto);
    }

    /**
     * Emite primero a quienes completaron y superaron todos los mínimos, ordenados
     * por puntaje descendente. El resto queda agrupado por estado del cuestionario.
     */
    public ReporteOrdenMerito emitirOrdenMerito(Puesto puesto, List<Long> idsEvaluaciones) {
        if (puesto == null) throw new IllegalArgumentException("El puesto es obligatorio.");
        if (idsEvaluaciones == null || idsEvaluaciones.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionarse al menos una evaluación.");
        }
        if (idsEvaluaciones.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("La selección contiene un id de evaluación inválido.");
        }

        List<Cuestionario> cuestionarios = cuestionarioDAO.buscarCuestionariosPorEvaluaciones(idsEvaluaciones);
        List<EntradaOrden> entradas = new ArrayList<>();
        for (Cuestionario cuestionario : cuestionarios) {
            if (!perteneceAlPuesto(cuestionario, puesto)) continue;
            Candidato candidato = cuestionario.getCandidato();
            if (candidato == null) continue;
            boolean superoMinimos = superaMinimos(cuestionario);
            entradas.add(new EntradaOrden(cuestionario, itemOrdenMerito(cuestionario), superoMinimos));
        }

        entradas.sort(comparadorOrden());
        ReporteOrdenMerito reporte = new ReporteOrdenMerito(puesto, new Date());
        for (EntradaOrden entrada : entradas) reporte.agregarCuestionario(entrada.cuestionario());
        return reporte;
    }

    /** Exporta a CSV los cuestionarios completados asociados al puesto. */
    public void exportarResultadosCSV(Puesto puesto, String ruta) {
        if (puesto == null) throw new IllegalArgumentException("El puesto es obligatorio.");
        if (ruta == null || ruta.trim().isEmpty()) throw new IllegalArgumentException("La ruta de destino es obligatoria.");

        List<ItemOrdenMeritoDTO> items = new ArrayList<>();
        for (Cuestionario cuestionario : cuestionarioDAO.buscarCuestionariosCompletos(puesto.getCodigo())) {
            Candidato candidato = cuestionario.getCandidato();
            if (candidato == null) continue;
            items.add(itemOrdenMerito(cuestionario));
        }
        items.sort(Comparator.comparing(ItemOrdenMeritoDTO::getPuntajeObtenido,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ItemOrdenMeritoDTO::getApellidoYNombre,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        ReporteOrdenMeritoDTO reporte = new ReporteOrdenMeritoDTO(
                puesto.getNombre(), nombreEmpresa(puesto), new Date(), items);
        exportadorCSV.exportarDatos(List.of(reporte), ruta);
    }

    /** TODO: operación de reportes comparativos, no utilizada por CU028. */
    public ReporteComparativo emitirReporteComparativo(Puesto puesto, Candidato candidato) {
        throw new UnsupportedOperationException("La emisión del reporte comparativo está pendiente fuera del CU028.");
    }

    private Comparator<EntradaOrden> comparadorOrden() {
        return Comparator.comparingInt((EntradaOrden e) -> e.superoMinimos() ? 0 : 1)
                .thenComparingInt(e -> e.superoMinimos()
                        ? -1 : ordenEstado(e.cuestionario().getEstado()))
                .thenComparing((EntradaOrden e) -> e.item().getPuntajeObtenido(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(e -> e.item().getApellidoYNombre(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private int ordenEstado(EstadoCuestionario estado) {
        return estado == null ? Integer.MAX_VALUE : estado.ordinal();
    }

    private ItemOrdenMeritoDTO itemOrdenMerito(Cuestionario cuestionario) {
        Candidato candidato = cuestionario.getCandidato();
        return new ItemOrdenMeritoDTO(candidato.getTipoDocumento(), candidato.getNroDocumento(),
                nombreCompleto(candidato), puntajePromedio(cuestionario),
                cuestionario.getEstado() == null ? null : cuestionario.getEstado().name(),
                superaMinimos(cuestionario));
    }

    private boolean superaMinimos(Cuestionario cuestionario) {
        if (cuestionario.getEstado() != EstadoCuestionario.COMPLETADO) return false;
        List<CompetenciaRequerida> requeridas = cuestionario.getPuesto().getCompetenciasRequeridas();
        if (requeridas.isEmpty()) return false;
        for (CompetenciaRequerida requerida : requeridas) {
            Resultado resultado = cuestionario.getResultados().stream()
                    .filter(r -> mismaCompetencia(r.getCompetenciaRequerida(), requerida))
                    .findFirst().orElse(null);
            if (resultado == null || !Boolean.TRUE.equals(resultado.cumpleMinimo())) return false;
        }
        return true;
    }

    private boolean mismaCompetencia(CompetenciaRequerida primera, CompetenciaRequerida segunda) {
        if (primera == null || segunda == null || primera.getCompetencia() == null
                || segunda.getCompetencia() == null) return false;
        Long idPrimera = primera.getCompetencia().getId();
        Long idSegunda = segunda.getCompetencia().getId();
        return idPrimera != null && idSegunda != null
                ? idPrimera.equals(idSegunda) : primera == segunda;
    }

    private Double puntajePromedio(Cuestionario cuestionario) {
        if (cuestionario.getResultados().isEmpty()) return null;
        double suma = 0.0;
        int cantidad = 0;
        for (Resultado resultado : cuestionario.getResultados()) {
            Double puntaje = resultado.getPuntajeObtenido();
            if (puntaje == null) puntaje = resultado.calcularPuntaje();
            if (puntaje != null) {
                suma += puntaje;
                cantidad++;
            }
        }
        return cantidad == 0 ? null : suma / cantidad;
    }

    private boolean perteneceAlPuesto(Cuestionario cuestionario, Puesto puesto) {
        Puesto asociado = cuestionario.getPuesto();
        return asociado != null && puesto.getCodigo().equals(asociado.getCodigo());
    }

    private String nombreCompleto(Candidato candidato) {
        String apellido = candidato.getApellido() == null ? "" : candidato.getApellido().trim();
        String nombre = candidato.getNombre() == null ? "" : candidato.getNombre().trim();
        return (apellido + " " + nombre).trim();
    }

    private String nombreEmpresa(Puesto puesto) {
        return puesto.getEmpresa() == null ? "" : puesto.getEmpresa().getNombre();
    }

    private record EntradaOrden(Cuestionario cuestionario, ItemOrdenMeritoDTO item, boolean superoMinimos) { }
}
