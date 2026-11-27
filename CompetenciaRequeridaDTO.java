/** Resumen de competencia y puntaje mínimo solicitado por el puesto. */
public class CompetenciaRequeridaDTO {
    private final Long idCompetencia;
    private final String nombreCompetencia;
    private final Double puntajeMinimo;

    public CompetenciaRequeridaDTO(Long idCompetencia, String nombreCompetencia,
                                   Double puntajeMinimo) {
        this.idCompetencia = idCompetencia;
        this.nombreCompetencia = nombreCompetencia;
        this.puntajeMinimo = puntajeMinimo;
    }

    public Long getIdCompetencia() { return idCompetencia; }
    public String getNombreCompetencia() { return nombreCompetencia; }
    public Double getPuntajeMinimo() { return puntajeMinimo; }
}
