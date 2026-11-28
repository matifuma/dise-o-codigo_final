import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Reporte emitido para un puesto; conserva el orden de candidatos generado por GestorReportes. */
public class ReporteOrdenMerito {
    private final Date fechaEmision;
    private final Puesto puestoEvaluado;
    private final List<Candidato> candidatos = new ArrayList<>();

    public ReporteOrdenMerito(Puesto puestoEvaluado, Date fechaEmision) {
        if (puestoEvaluado == null) throw new IllegalArgumentException("El puesto evaluado es obligatorio.");
        this.puestoEvaluado = puestoEvaluado;
        this.fechaEmision = fechaEmision == null ? new Date() : new Date(fechaEmision.getTime());
    }

    public Date leerFechaEmision() { return new Date(fechaEmision.getTime()); }
    public Puesto leerPuesto() { return puestoEvaluado; }
    public List<Candidato> leerCandidatos() { return Collections.unmodifiableList(candidatos); }

    void agregarCandidato(Candidato candidato) {
        if (candidato == null) throw new IllegalArgumentException("El candidato del reporte es obligatorio.");
        candidatos.add(candidato);
    }
}
