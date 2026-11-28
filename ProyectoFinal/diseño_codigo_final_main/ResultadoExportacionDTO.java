import java.util.Date;

/** Fila de datos con el resultado final de un cuestionario para exportación. */
public class ResultadoExportacionDTO {
    private final Integer nroCandidato;
    private final String apellido;
    private final String nombre;
    private final Double puntajeObtenido;
    private final Date fechaCalculo;
    private final String estadoCuestionario;

    public ResultadoExportacionDTO(Integer nroCandidato, String apellido, String nombre,
                                   Double puntajeObtenido, Date fechaCalculo,
                                   String estadoCuestionario) {
        this.nroCandidato = nroCandidato;
        this.apellido = apellido;
        this.nombre = nombre;
        this.puntajeObtenido = puntajeObtenido;
        this.fechaCalculo = fechaCalculo == null ? null : new Date(fechaCalculo.getTime());
        this.estadoCuestionario = estadoCuestionario;
    }

    public Integer getNroCandidato() { return nroCandidato; }
    public String getApellido() { return apellido; }
    public String getNombre() { return nombre; }
    public Double getPuntajeObtenido() { return puntajeObtenido; }
    public Date getFechaCalculo() {
        return fechaCalculo == null ? null : new Date(fechaCalculo.getTime());
    }
    public String getEstadoCuestionario() { return estadoCuestionario; }
}
