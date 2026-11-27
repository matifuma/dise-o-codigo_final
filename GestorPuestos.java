import java.util.List;

/** Controlador de puestos consultado por CU025. */
public class GestorPuestos {
    private final PuestoDAO puestoDAO;

    public GestorPuestos(PuestoDAO puestoDAO) {
        if (puestoDAO == null) throw new IllegalArgumentException("PuestoDAO es obligatorio.");
        this.puestoDAO = puestoDAO;
    }

    /** El gestor devuelve la entidad seleccionable que recibirá el flujo de CU025. */
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
}
