import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Evaluación que agrupa los cuestionarios generados para un puesto. */
public class Evaluacion {
    private Long id;
    private Date fecha;
    private final Puesto puesto;
    private final List<Cuestionario> cuestionarios = new ArrayList<>();

    public Evaluacion(Long id, Date fecha) {
        this(id, fecha, null);
    }

    public Evaluacion(Long id, Date fecha, Puesto puesto) {
        this.id = id;
        this.fecha = fecha == null ? new Date() : new Date(fecha.getTime());
        this.puesto = puesto;
    }

    public Long getId() { return id; }
    public Date getFecha() { return new Date(fecha.getTime()); }
    public Puesto getPuesto() { return puesto; }

    public List<Cuestionario> obtenerCuestionarios() {
        return Collections.unmodifiableList(cuestionarios);
    }

    void setId(Long id) { this.id = id; }
    void agregarCuestionario(Cuestionario cuestionario) {
        if (cuestionario != null && !cuestionarios.contains(cuestionario)) cuestionarios.add(cuestionario);
    }
    void quitarCuestionario(Cuestionario cuestionario) { cuestionarios.remove(cuestionario); }
}
