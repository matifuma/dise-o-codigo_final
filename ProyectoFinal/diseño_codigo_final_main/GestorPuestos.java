import java.util.List;

/** Controlador de puestos consultado por CU025. */
public class GestorPuestos {
    private final PuestoDAO puestoDAO;
    private final CuestionarioDAO cuestionarioDAO;

    public GestorPuestos(PuestoDAO puestoDAO) {
        this(puestoDAO, null);
    }

    /** Constructor CU028: permite obtener los conteos vinculados a cuestionarios. */
    public GestorPuestos(PuestoDAO puestoDAO, CuestionarioDAO cuestionarioDAO) {
        if (puestoDAO == null) throw new IllegalArgumentException("PuestoDAO es obligatorio.");
        this.puestoDAO = puestoDAO;
        this.cuestionarioDAO = cuestionarioDAO;
    }

    /** Búsqueda usada por CU028, que admite código, nombre y empresa. */
    public List<Puesto> buscarPuesto(String codigo, String nombre, String empresa) {
        return puestoDAO.buscarPuestos(codigo, nombre, empresa);
    }

    /** Recupera el puesto completo seleccionado desde la lista del CU028. */
    public Puesto obtenerPuesto(String codigo) {
        return puestoDAO.obtenerPuesto(codigo);
    }

    /** CU028: consulta el total de candidatos asociados al puesto seleccionado. */
    public Integer contarCandidatosPorPuesto(String codigoPuesto) {
        return daoCuestionarios().contarCandidatosPorPuesto(codigoPuesto);
    }

    /** CU028: consulta el total de evaluaciones completadas para el puesto. */
    public Integer contarEvaluacionesCompletasPorPuesto(String codigoPuesto) {
        return daoCuestionarios().contarEvaluacionesCompletasPorPuesto(codigoPuesto);
    }

    /** Firma conservada para los casos de uso que ya la utilizan. */
    public List<Puesto> buscarPuestos(String nombre, String empresa) {
        return puestoDAO.buscarPuestos(nombre, empresa);
    }

    /** TODO: operación de mantenimiento de puestos; requiere definir sus reglas de negocio. */
    public void darDeAltaPuesto(String codigo, String nombre, String descripcion, Empresa empresa,
                                List<CompetenciaRequerida> competencias) {
        throw new UnsupportedOperationException("Pendiente implementar el alta de puestos.");
    }

    /** TODO: operación ajena a CU025, pendiente de reglas de modificación. */
    public void modificarPuesto(Puesto puesto, String nombre, String descripcion, Empresa empresa,
                                List<CompetenciaRequerida> competencias) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de puestos.");
    }

    /** TODO: operación ajena a CU025, pendiente de definir baja lógica o física. */
    public void darDeBajaPuesto(Puesto puesto) {
        throw new UnsupportedOperationException("Pendiente implementar la baja de puestos.");
    }

    private CuestionarioDAO daoCuestionarios() {
        if (cuestionarioDAO == null) {
            throw new IllegalStateException("CU028 requiere configurar CuestionarioDAO en GestorPuestos.");
        }
        return cuestionarioDAO;
    }
}
