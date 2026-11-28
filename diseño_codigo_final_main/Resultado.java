import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Resultado de una competencia requerida para el puesto evaluado. */
public class Resultado {
    private static final AtomicLong SIGUIENTE_ID = new AtomicLong(1L);

    private Long id;
    private Double puntajeObtenido;
    private final Double puntajeRequerido;
    private Date fechaCalculo;
    private final CompetenciaRequerida competenciaRequerida;
    private final List<ResultadoFactor> resultadosFactor = new ArrayList<>();

    public Resultado(CompetenciaRequerida competenciaRequerida) {
        if (competenciaRequerida == null) {
            throw new IllegalArgumentException("La competencia requerida es obligatoria.");
        }
        this.competenciaRequerida = competenciaRequerida;
        this.puntajeRequerido = competenciaRequerida.getPuntajeMinimo();
    }

    public Long getId() { return id; }
    public Double getPuntajeObtenido() { return puntajeObtenido; }
    public Double getPuntajeRequerido() { return puntajeRequerido; }
    public Date getFechaCalculo() {
        return fechaCalculo == null ? null : new Date(fechaCalculo.getTime());
    }
    public CompetenciaRequerida getCompetenciaRequerida() { return competenciaRequerida; }
    public List<ResultadoFactor> getResultadosFactor() {
        return Collections.unmodifiableList(resultadosFactor);
    }

    void agregarResultadoFactor(ResultadoFactor resultadoFactor) {
        if (resultadoFactor != null && !resultadosFactor.contains(resultadoFactor)) {
            resultadosFactor.add(resultadoFactor);
        }
    }

    /** Promedia los puntajes de los factores incluidos en esta competencia. */
    public Double calcularPuntaje() {
        if (resultadosFactor.isEmpty()) {
            puntajeObtenido = 0.0;
        } else {
            double suma = 0.0;
            for (ResultadoFactor factor : resultadosFactor) {
                Double puntaje = factor.getPuntajeObtenido();
                if (puntaje == null) puntaje = factor.calcularPuntajeFactor();
                suma += puntaje;
            }
            puntajeObtenido = suma / resultadosFactor.size();
        }
        if (id == null) id = SIGUIENTE_ID.getAndIncrement();
        fechaCalculo = new Date();
        return puntajeObtenido;
    }

    public Boolean cumpleMinimo() {
        if (puntajeObtenido == null) calcularPuntaje();
        return puntajeObtenido >= puntajeRequerido;
    }
}
