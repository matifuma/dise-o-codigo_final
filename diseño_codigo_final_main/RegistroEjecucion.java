import java.util.Date;

/** Registro de ejecución definido en el diagrama; no lo utiliza CU020. */
public class RegistroEjecucion {
    private final Date fechaHora;
    private final String accion;
    private final String detalle;

    public RegistroEjecucion(Date fechaHora, String accion, String detalle) {
        this.fechaHora = new Date(fechaHora.getTime());
        this.accion = accion;
        this.detalle = detalle;
    }

    public Date getFechaHora() {
        return new Date(fechaHora.getTime());
    }

    public String getAccion() {
        return accion;
    }

    public String getDetalle() {
        return detalle;
    }
}
