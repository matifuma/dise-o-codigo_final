import java.util.ArrayList;
import java.util.List;

/** Búsqueda de factores para CU021 y validación de evaluabilidad de CU025. */
public class GestorFactores {
    private final FactorDAO factorDAO;
    private final CompetenciaDAO competenciaDAO;

    public GestorFactores(FactorDAO factorDAO) {
        this(factorDAO, null);
    }

    /**
     * Constructor completo para búsquedas del CU021 por nombre de competencia.
     * El constructor de un solo DAO se conserva para los usos existentes de CU025.
     */
    public GestorFactores(FactorDAO factorDAO, CompetenciaDAO competenciaDAO) {
        if (factorDAO == null) throw new IllegalArgumentException("FactorDAO es obligatorio.");
        this.factorDAO = factorDAO;
        this.competenciaDAO = competenciaDAO;
    }

    public List<Factor> buscarFactor(Competencia competencia) {
        if (competencia == null || competencia.getId() == null) return List.of();
        List<Factor> resultado = new ArrayList<>();
        for (Factor factor : factorDAO.obtenerFactoresPorCompetencia(competencia.getId())) {
            if (Boolean.TRUE.equals(factor.getActivo())) resultado.add(factor);
        }
        return resultado;
    }

    /** Búsqueda del CU021: resuelve las competencias por nombre y luego consulta sus factores. */
    public List<Factor> buscarFactor(String nombreCompetencia, String nombreFactor) {
        if (competenciaDAO == null) {
            throw new IllegalStateException(
                    "Para buscar factores por nombre debe configurarse CompetenciaDAO.");
        }
        if (nombreCompetencia == null || nombreCompetencia.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar el nombre de la competencia.");
        }

        String filtroCompetencia = normalizar(nombreCompetencia);
        String filtroFactor = normalizar(nombreFactor);
        List<Factor> resultado = new ArrayList<>();
        for (Competencia competencia : competenciaDAO.buscarCompetencias(nombreCompetencia)) {
            // La consulta por texto puede devolver coincidencias parciales; solo se incluyen
            // competencias activas cuyo nombre coincida con el criterio recibido.
            if (!Boolean.TRUE.equals(competencia.getActivo())
                    || !normalizar(competencia.getNombre()).contains(filtroCompetencia)) {
                continue;
            }
            for (Factor factor : factorDAO.obtenerFactoresPorCompetencia(competencia.getId())) {
                if (Boolean.TRUE.equals(factor.getActivo())
                        && normalizar(factor.getNombre()).contains(filtroFactor)) {
                    resultado.add(factor);
                }
            }
        }
        return resultado;
    }

    /** TODO: mantenimiento de factores, operación fuera de CU025. */
    public void darDeAltaFactor(String codigo, String nombre, String descripcion,
                                Competencia competencia, Integer nroOrden) {
        throw new UnsupportedOperationException("Pendiente implementar el alta de factores.");
    }

    /** TODO: mantenimiento de factores, operación fuera de CU025. */
    public void modificarFactor(Factor factor, String nombre, String descripcion,
                                Competencia competencia, Integer nroOrden) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de factores.");
    }

    /** TODO: mantenimiento de factores, operación fuera de CU025. */
    public void darDeBajaFactor(Factor factor) {
        throw new UnsupportedOperationException("Pendiente implementar la baja de factores.");
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
