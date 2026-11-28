import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Representación transportable del orden de mérito para la interfaz y la exportación. */
public class ReporteOrdenMeritoDTO {
    private final String nombrePuesto;
    private final String nombreEmpresa;
    private final Date fechaGeneracion;
    private final List<ItemOrdenMeritoDTO> candidatosOrdenados;

    public ReporteOrdenMeritoDTO(String nombrePuesto, String nombreEmpresa, Date fechaGeneracion,
                                 List<ItemOrdenMeritoDTO> candidatosOrdenados) {
        this.nombrePuesto = nombrePuesto;
        this.nombreEmpresa = nombreEmpresa;
        this.fechaGeneracion = fechaGeneracion == null ? null : new Date(fechaGeneracion.getTime());
        this.candidatosOrdenados = new ArrayList<>(
                candidatosOrdenados == null ? List.of() : candidatosOrdenados);
    }

    public String getNombrePuesto() { return nombrePuesto; }
    public String getNombreEmpresa() { return nombreEmpresa; }
    public Date getFechaGeneracion() {
        return fechaGeneracion == null ? null : new Date(fechaGeneracion.getTime());
    }
    public List<ItemOrdenMeritoDTO> getCandidatosOrdenados() {
        return Collections.unmodifiableList(candidatosOrdenados);
    }
}
