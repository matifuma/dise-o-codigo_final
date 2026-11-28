/** Vista de transporte de candidato usada por las pantallas de CU025. */
public class CandidatoDTO {
    private final Integer nroCandidato;
    private final String apellido;
    private final String nombre;
    private final String tipoDocumento;
    private final String nroDocumento;
    private final String email;
    private final Integer claveAcceso;

    public CandidatoDTO(Candidato candidato) {
        this(candidato, null);
    }

    /** DTO con clave de acceso para mostrar/exportar la selección de CU025. */
    public CandidatoDTO(Candidato candidato, Integer claveAcceso) {
        if (candidato == null) throw new IllegalArgumentException("El candidato es obligatorio.");
        this.nroCandidato = candidato.getNroCandidato();
        this.apellido = candidato.getApellido();
        this.nombre = candidato.getNombre();
        this.tipoDocumento = candidato.getTipoDocumento();
        this.nroDocumento = candidato.getNroDocumento();
        this.email = candidato.getEmail();
        this.claveAcceso = claveAcceso;
    }

    public Integer getNroCandidato() { return nroCandidato; }
    public String getApellido() { return apellido; }
    public String getNombre() { return nombre; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNroDocumento() { return nroDocumento; }
    public String getEmail() { return email; }
    public Integer getClaveAcceso() { return claveAcceso; }
}
