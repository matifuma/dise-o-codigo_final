/** Opción posible de respuesta a una pregunta. */
public class OpcionRespuesta {
    private Long id;
    private String texto;
    private Double puntaje;
    private Integer orden;
    private Boolean activo;

    public OpcionRespuesta(Long id, String texto, Double puntaje,
                           Integer orden, Boolean activo) {
        this.id = id;
        this.texto = texto;
        this.puntaje = puntaje;
        this.orden = orden;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public String getTexto() {
        return texto;
    }

    public Double getPuntaje() {
        return puntaje;
    }

    public Integer getOrden() {
        return orden;
    }

    public Boolean getActivo() {
        return activo;
    }
}
