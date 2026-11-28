import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Entidad del catálogo de competencias. */
public class Competencia {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private TipoCompetencia tipo;
    private String tecnologia;
    private String comportamiento;

    // Relaciones UML: una competencia agrupa factores y corresponde a preguntas.
    private final List<Factor> factores = new ArrayList<>();
    private final List<Pregunta> preguntas = new ArrayList<>();

    public Competencia(Long id, String nombre, String descripcion, Boolean activo,
                       TipoCompetencia tipo, String tecnologia, String comportamiento) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
        this.tipo = tipo;
        this.tecnologia = tecnologia;
        this.comportamiento = comportamiento;
    }

    public Long getId() {
        return id;
    }

    /** Asignación interna del identificador generado por CompetenciaDAO. */
    void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    void setActivo(Boolean activo) {
        this.activo = activo;
    }

    /** Operación explicitada en el diagrama: devuelve el tipo como texto. */
    public String getTipo() {
        return tipo == null ? null : tipo.name();
    }

    public TipoCompetencia getTipoCompetencia() {
        return tipo;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public String getComportamiento() {
        return comportamiento;
    }

    public List<Factor> getFactores() {
        return Collections.unmodifiableList(factores);
    }

    public List<Pregunta> getPreguntas() {
        return Collections.unmodifiableList(preguntas);
    }

    void agregarFactor(Factor factor) {
        if (factor != null && !factores.contains(factor)) {
            factores.add(factor);
        }
    }

    void agregarPregunta(Pregunta pregunta) {
        if (pregunta != null && !preguntas.contains(pregunta)) {
            preguntas.add(pregunta);
        }
    }
}
