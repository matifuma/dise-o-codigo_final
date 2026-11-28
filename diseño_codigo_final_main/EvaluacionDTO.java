import java.util.Date;

/** Datos mínimos de una evaluación para seleccionarla en CU028. */
public class EvaluacionDTO {
    private final Long id;
    private final Date fecha;
    private final String nombrePuesto;

    public EvaluacionDTO(Long id, Date fecha, String nombrePuesto) {
        if (id == null) throw new IllegalArgumentException("El id de la evaluación es obligatorio.");
        this.id = id;
        this.fecha = fecha == null ? null : new Date(fecha.getTime());
        this.nombrePuesto = nombrePuesto;
    }

    public Long getId() { return id; }
    public Date getFecha() { return fecha == null ? null : new Date(fecha.getTime()); }
    public String getNombrePuesto() { return nombrePuesto; }
}
