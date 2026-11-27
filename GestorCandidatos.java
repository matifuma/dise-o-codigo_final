import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Controlador de búsqueda y validación de candidatos para CU025. */
public class GestorCandidatos {
    private final CandidatoDAO candidatoDAO;
    private final CuestionarioDAO cuestionarioDAO;

    public GestorCandidatos(CandidatoDAO candidatoDAO, CuestionarioDAO cuestionarioDAO) {
        if (candidatoDAO == null || cuestionarioDAO == null) {
            throw new IllegalArgumentException("Se requieren CandidatoDAO y CuestionarioDAO.");
        }
        this.candidatoDAO = candidatoDAO;
        this.cuestionarioDAO = cuestionarioDAO;
    }

    public List<CandidatoDTO> buscarCandidatos(String apellido, String nombre) {
        return buscarCandidatos(apellido, nombre, null);
    }

    /** El filtro por número recorre UI, gestor y DAO, como requiere la pantalla de CU025. */
    public List<CandidatoDTO> buscarCandidatos(String apellido, String nombre, Integer nroCandidato) {
        return convertir(candidatoDAO.buscarCandidatos(apellido, nombre, nroCandidato));
    }

    /** Devuelve la selección que no puede volver a evaluarse por tener un cuestionario abierto. */
    public List<Candidato> validarCandidatos(List<Candidato> candidatosAEvaluar) {
        if (candidatosAEvaluar == null || candidatosAEvaluar.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un candidato.");
        }
        java.util.Set<Integer> numeros = new java.util.HashSet<>();
        for (Candidato candidato : candidatosAEvaluar) {
            if (candidato == null || candidato.getNroCandidato() == null
                    || !numeros.add(candidato.getNroCandidato())) {
                throw new IllegalArgumentException("La selección contiene un candidato inválido o duplicado.");
            }
        }
        List<Candidato> activos = candidatoDAO.buscarActivos(candidatosAEvaluar);
        if (activos.size() != candidatosAEvaluar.size()) {
            throw new IllegalArgumentException("La selección contiene candidatos inactivos o no registrados.");
        }
        List<Candidato> conCuestionarioAbierto = new ArrayList<>();
        for (Candidato candidato : activos) {
            if (cuestionarioDAO.obtenerCuestionariosActivo(
                    candidato.getTipoDocumento(), candidato.getNroDocumento()) != null) {
                conCuestionarioAbierto.add(candidato);
            }
        }
        return conCuestionarioAbierto;
    }

    /** Firma de integración con la UI, que conserva DTO y no expone entidades. */
    public List<CandidatoDTO> validarCandidatos(Collection<CandidatoDTO> candidatosAEvaluar) {
        return convertir(validarCandidatos(resolver(candidatosAEvaluar)));
    }

    private List<Candidato> resolver(Collection<CandidatoDTO> candidatosDTO) {
        if (candidatosDTO == null || candidatosDTO.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un candidato.");
        }
        List<Candidato> resultado = new ArrayList<>();
        for (CandidatoDTO dto : candidatosDTO) {
            if (dto == null || dto.getNroCandidato() == null) {
                throw new IllegalArgumentException("La selección contiene un candidato inválido.");
            }
            List<Candidato> encontrado = candidatoDAO.buscarCandidatos(null, null, dto.getNroCandidato());
            if (encontrado.size() != 1) throw new IllegalArgumentException("El candidato ya no está disponible.");
            resultado.add(encontrado.get(0));
        }
        return resultado;
    }

    private List<CandidatoDTO> convertir(List<Candidato> candidatos) {
        List<CandidatoDTO> resultado = new ArrayList<>();
        for (Candidato candidato : candidatos) resultado.add(new CandidatoDTO(candidato));
        return resultado;
    }

    /** TODO CU de importación CSV: falta especificar formato, validaciones y tratamiento de errores. */
    public void importarCandidatosCSV(String ruta) {
        throw new UnsupportedOperationException("Pendiente definir la importación CSV de candidatos.");
    }
}
