import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** DAO temporal en memoria para auditoría. */
public class AuditoriaDAO {
    private final List<RegistroAuditoria> auditorias = new ArrayList<>();

    public synchronized void registrarAuditoria(RegistroAuditoria registro) {
        if (registro == null) {
            throw new IllegalArgumentException("El registro de auditoría es obligatorio.");
        }
        auditorias.add(registro);
    }

    /** Operación del diagrama que no se utiliza en CU020. */
    public void registrarEjecucion(RegistroEjecucion registro) {
        // TODO: implementar persistencia de registros de ejecución.
        throw new UnsupportedOperationException("Pendiente implementar registrarEjecucion.");
    }

    public synchronized List<RegistroAuditoria> obtenerAuditorias() {
        return Collections.unmodifiableList(new ArrayList<>(auditorias));
    }
}
