/** Fila de un candidato en el orden de mérito y su estado de evaluación. */
public class ItemOrdenMeritoDTO {
    private final String tipoDoc;
    private final String nroDoc;
    private final String apellidoYNombre;
    private final Double puntajeObtenido;
    private final String estadoCuestionario;
    private final Boolean superoMinimos;

    public ItemOrdenMeritoDTO(String tipoDoc, String nroDoc, String apellidoYNombre,
                              Double puntajeObtenido, String estadoCuestionario,
                              Boolean superoMinimos) {
        this.tipoDoc = tipoDoc;
        this.nroDoc = nroDoc;
        this.apellidoYNombre = apellidoYNombre;
        this.puntajeObtenido = puntajeObtenido;
        this.estadoCuestionario = estadoCuestionario;
        this.superoMinimos = superoMinimos;
    }

    public String getTipoDoc() { return tipoDoc; }
    public String getNroDoc() { return nroDoc; }
    public String getApellidoYNombre() { return apellidoYNombre; }
    public Double getPuntajeObtenido() { return puntajeObtenido; }
    public String getEstadoCuestionario() { return estadoCuestionario; }
    public Boolean getSuperoMinimos() { return superoMinimos; }
}
