import java.util.Date;

/** Registro generado por las operaciones auditables del sistema. */
public class RegistroAuditoria {
    private final Date fechaHora;
    private final String detalleOperacion;
    private final String entidadAfectada;

    public RegistroAuditoria(Date fechaHora, String detalleOperacion, String entidadAfectada) {
        this.fechaHora = new Date(fechaHora.getTime());
        this.detalleOperacion = detalleOperacion;
        this.entidadAfectada = entidadAfectada;
    }

    public Date getFechaHora() {
        return new Date(fechaHora.getTime());
    }

    public String getDetalleOperacion() {
        return detalleOperacion;
    }

    public String getEntidadAfectada() {
        return entidadAfectada;
    }
}
