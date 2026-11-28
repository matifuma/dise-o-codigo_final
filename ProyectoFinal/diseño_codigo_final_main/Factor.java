import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Factor asociado a una competencia. */
public class Factor {
    private Long id;
    private String nombre;
    private String descripcion;
    private Integer nroDeOrden;
    private Boolean activo;

    // Relaciones UML: cada factor pertenece a una competencia y aporta preguntas.
    private final Competencia competencia;
    private final List<Pregunta> preguntas = new ArrayList<>();

    public Factor(Long id, String nombre, String descripcion, Integer nroDeOrden,
                  Boolean activo, Competencia competencia) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nroDeOrden = nroDeOrden;
        this.activo = activo;
        this.competencia = competencia;
        if (competencia != null) {
            competencia.agregarFactor(this);
        }
    }

    public Long getId() {
        return id;
    }

    /** Asignación interna del identificador generado por FactorDAO. */
    void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Integer getNroDeOrden() {
        return nroDeOrden;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public List<Pregunta> getPreguntas() {
        return Collections.unmodifiableList(preguntas);
    }

    void agregarPregunta(Pregunta pregunta) {
        if (pregunta != null && !preguntas.contains(pregunta)) {
            preguntas.add(pregunta);
        }
    }
}
