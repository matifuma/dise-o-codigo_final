import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** DTO de puesto según el diagrama de clases. */
public class PuestoDTO {
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final String empresa;
    private final List<CompetenciaRequeridaDTO> competenciasRequeridas;

    public PuestoDTO(Puesto puesto) {
        if (puesto == null) throw new IllegalArgumentException("El puesto es obligatorio.");
        this.codigo = puesto.getCodigo();
        this.nombre = puesto.getNombre();
        this.descripcion = puesto.getDescripcion();
        this.empresa = puesto.getEmpresa() == null ? "" : puesto.getEmpresa().getNombre();
        this.competenciasRequeridas = new ArrayList<>();
        for (CompetenciaRequerida requerida : puesto.getCompetenciasRequeridas()) {
            Competencia competencia = requerida.getCompetencia();
            competenciasRequeridas.add(new CompetenciaRequeridaDTO(
                    competencia.getId(),
                    competencia.getNombre(), requerida.getPuntajeMinimo()));
        }
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getEmpresa() { return empresa; }
    public List<CompetenciaRequeridaDTO> getCompetenciasRequeridas() {
        return Collections.unmodifiableList(competenciasRequeridas);
    }
}
