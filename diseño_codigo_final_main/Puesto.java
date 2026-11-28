import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Entidad Puesto utilizada para fijar el modelo común de la evaluación. */
public class Puesto {
    private final String codigo;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private Empresa empresa;
    private final List<CompetenciaRequerida> competenciasRequeridas = new ArrayList<>();

    public Puesto(String codigo, String nombre, String descripcion, Boolean activo,
                  Empresa empresa, List<CompetenciaRequerida> competenciasRequeridas) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código del puesto es obligatorio.");
        }
        this.codigo = codigo.trim();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
        this.empresa = empresa;
        if (competenciasRequeridas != null) this.competenciasRequeridas.addAll(competenciasRequeridas);
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Boolean getActivo() { return activo; }
    public Empresa getEmpresa() { return empresa; }
    public List<CompetenciaRequerida> getCompetenciasRequeridas() {
        return Collections.unmodifiableList(competenciasRequeridas);
    }

    void actualizar(String nombre, String descripcion, Empresa empresa,
                    List<CompetenciaRequerida> competencias) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.empresa = empresa;
        this.competenciasRequeridas.clear();
        if (competencias != null) this.competenciasRequeridas.addAll(competencias);
    }

    void setActivo(Boolean activo) { this.activo = activo; }
}
