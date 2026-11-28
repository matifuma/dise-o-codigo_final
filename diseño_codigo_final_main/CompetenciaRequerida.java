/** Asociación Puesto-Competencia con la ponderación mínima exigida. */
public class CompetenciaRequerida {
    private final Competencia competencia;
    private final Double puntajeMinimo;

    public CompetenciaRequerida(Competencia competencia, Double puntajeMinimo) {
        if (competencia == null || puntajeMinimo == null || puntajeMinimo < 0) {
            throw new IllegalArgumentException("La competencia y su puntaje mínimo son obligatorios.");
        }
        this.competencia = competencia;
        this.puntajeMinimo = puntajeMinimo;
    }

    public Competencia getCompetencia() { return competencia; }
    public Double getPuntajeMinimo() { return puntajeMinimo; }
}
