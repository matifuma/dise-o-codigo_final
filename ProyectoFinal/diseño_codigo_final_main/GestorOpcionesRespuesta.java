import java.util.List;

/** Controlador de consulta y mantenimiento del catálogo de opciones. */
public class GestorOpcionesRespuesta {
    private final OpcionRespuestaDAO opcionRespuestaDAO;

    public GestorOpcionesRespuesta(OpcionRespuestaDAO opcionRespuestaDAO) {
        if (opcionRespuestaDAO == null) {
            throw new IllegalArgumentException("OpcionRespuestaDAO es obligatorio.");
        }
        this.opcionRespuestaDAO = opcionRespuestaDAO;
    }

    /** Búsqueda utilizada por el selector de opciones del CU021. */
    public List<OpcionRespuesta> buscarOpciones(String nombre) {
        return opcionRespuestaDAO.buscarOpciones(nombre);
    }

    /** TODO: mantenimiento de opciones fuera del CU021. */
    public void darDeAltaOpcion(String nombre, String descripcion,
                                List<OpcionRespuesta> opciones) {
        throw new UnsupportedOperationException("Pendiente implementar el alta de opciones.");
    }

    /** TODO: mantenimiento de opciones fuera del CU021. */
    public void modificarOpcion(OpcionRespuesta opcion, String descripcion) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de opciones.");
    }

    /** TODO: mantenimiento de opciones fuera del CU021. */
    public void darDeBajaOpcion(OpcionRespuesta opcion) {
        throw new UnsupportedOperationException("Pendiente implementar la baja de opciones.");
    }
}
