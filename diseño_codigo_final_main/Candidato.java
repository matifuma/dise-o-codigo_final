import java.util.Date;

/** Entidad Candidato según el diagrama de clases. */
public class Candidato {
    private Boolean activo;
    private Integer nroCandidato;
    private String apellido;
    private String nombre;
    private String tipoDocumento;
    private String nroDocumento;
    private Date fechaNacimiento;
    private String genero;
    private String nacionalidad;
    private String escolaridad;
    private String email;
    private Boolean eliminado;

    public Candidato(Boolean activo, Integer nroCandidato, String apellido, String nombre,
                     String tipoDocumento, String nroDocumento, Date fechaNacimiento,
                     String genero, String nacionalidad, String escolaridad,
                     String email, Boolean eliminado) {
        this.activo = activo;
        this.nroCandidato = nroCandidato;
        this.apellido = apellido;
        this.nombre = nombre;
        this.tipoDocumento = tipoDocumento;
        this.nroDocumento = nroDocumento;
        this.fechaNacimiento = fechaNacimiento == null ? null : new Date(fechaNacimiento.getTime());
        this.genero = genero;
        this.nacionalidad = nacionalidad;
        this.escolaridad = escolaridad;
        this.email = email;
        this.eliminado = eliminado;
    }

    public Boolean getActivo() { return activo; }
    public Integer getNroCandidato() { return nroCandidato; }
    public String getApellido() { return apellido; }
    public String getNombre() { return nombre; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNroDocumento() { return nroDocumento; }
    public Date getFechaNacimiento() {
        return fechaNacimiento == null ? null : new Date(fechaNacimiento.getTime());
    }
    public String getGenero() { return genero; }
    public String getNacionalidad() { return nacionalidad; }
    public String getEscolaridad() { return escolaridad; }
    public String getEmail() { return email; }
    public Boolean getEliminado() { return eliminado; }

    void setActivo(Boolean activo) { this.activo = activo; }
    void setEliminado(Boolean eliminado) { this.eliminado = eliminado; }
}
