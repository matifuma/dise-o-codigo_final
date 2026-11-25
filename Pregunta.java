import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Entidad Pregunta y relaciones UML con Competencia, Factor y opciones. */
public class Pregunta {
    private Long id;

    // Excepción documentada: CU021 usa nombre para alta/búsqueda; no figura
    // como atributo en el diagrama de clases ajustado.
    private String nombre;

    private String enunciado;
    private String descripcion;
    private Boolean activo;
    private TipoPregunta tipo;

    private final Competencia competencia;
    private final Factor factor;
    private final List<OpcionRespuesta> opciones = new ArrayList<>();

    public Pregunta(Long id, String nombre, String enunciado, String descripcion,
                    Boolean activo, TipoPregunta tipo, Competencia competencia,
                    Factor factor, List<OpcionRespuesta> opciones) {
        this.id = id;
        this.nombre = nombre;
        this.enunciado = enunciado;
        this.descripcion = descripcion;
        this.activo = activo;
        this.tipo = tipo;
        this.competencia = competencia;
        this.factor = factor;
        reemplazarOpciones(opciones);

        if (competencia != null) {
            competencia.agregarPregunta(this);
        }
        if (factor != null) {
            factor.agregarPregunta(this);
        }
    }

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public TipoPregunta getTipo() {
        return tipo;
    }

    public void setTipo(TipoPregunta tipo) {
        this.tipo = tipo;
    }

    public Competencia getCompetencia() {
        return competencia;
    }

    public Factor getFactor() {
        return factor;
    }

    public List<OpcionRespuesta> getOpciones() {
        return Collections.unmodifiableList(opciones);
    }

    void reemplazarOpciones(List<OpcionRespuesta> nuevasOpciones) {
        opciones.clear();
        if (nuevasOpciones != null) {
            opciones.addAll(nuevasOpciones);
            opciones.sort(Comparator.comparing(OpcionRespuesta::getOrden,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
    }
}
