import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Estructura de reporte comparativo definida en el diagrama; su generación no pertenece al CU028. */
public class ReporteComparativo {
    private final Date fechaEmision;
    private final Puesto puestoEvaluado;
    private final Candidato candidatoEvaluado;
    private final List<Candidato> candidatos = new ArrayList<>();

    public ReporteComparativo(Date fechaEmision, Puesto puestoEvaluado, Candidato candidatoEvaluado) {
        if (puestoEvaluado == null || candidatoEvaluado == null) {
            throw new IllegalArgumentException("El puesto y el candidato son obligatorios.");
        }
        this.fechaEmision = fechaEmision == null ? new Date() : new Date(fechaEmision.getTime());
        this.puestoEvaluado = puestoEvaluado;
        this.candidatoEvaluado = candidatoEvaluado;
        this.candidatos.add(candidatoEvaluado);
    }

    public Date leerFechaEmision() { return new Date(fechaEmision.getTime()); }
    public Puesto leerPuesto() { return puestoEvaluado; }
    public List<Candidato> leerCandidatos() { return Collections.unmodifiableList(candidatos); }
    public Candidato leerCandidato() { return candidatoEvaluado; }
}
