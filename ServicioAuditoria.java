import java.util.Date;

/** Servicio de auditoría definido en el diagrama de clases. */
public class ServicioAuditoria {
    private final AuditoriaDAO auditoriaDAO;

    public ServicioAuditoria(AuditoriaDAO auditoriaDAO) {
        if (auditoriaDAO == null) {
            throw new IllegalArgumentException("AuditoriaDAO es obligatorio.");
        }
        this.auditoriaDAO = auditoriaDAO;
    }

    public void registrarAuditoria(String detalleOperacion, String entidadAfectada, String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario que realiza la operación es obligatorio.");
        }
        String detalleConUsuario = "Usuario: " + usuario.trim() + " | " + detalleOperacion;
        auditoriaDAO.registrarAuditoria(
                new RegistroAuditoria(new Date(), detalleConUsuario, entidadAfectada));
    }
}
