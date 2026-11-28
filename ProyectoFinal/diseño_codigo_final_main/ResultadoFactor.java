import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Puntaje de un factor calculado a partir de las respuestas registradas. */
public class ResultadoFactor {
    private static final AtomicLong SIGUIENTE_ID = new AtomicLong(1L);

    private Long id;
    private Double puntajeObtenido;
    private final Factor factor;
    private final List<RespuestaCandidato> respuestas;

    public ResultadoFactor(Factor factor, List<RespuestaCandidato> respuestas) {
        if (factor == null) throw new IllegalArgumentException("El factor es obligatorio.");
        this.factor = factor;
        this.respuestas = new ArrayList<>(respuestas == null ? List.of() : respuestas);
    }

    public Long getId() { return id; }
    public Double getPuntajeObtenido() { return puntajeObtenido; }
    public Factor getFactor() { return factor; }
    public List<RespuestaCandidato> getRespuestas() { return Collections.unmodifiableList(respuestas); }

    /** Promedia las ponderaciones elegidas para las preguntas de este factor. */
    public Double calcularPuntajeFactor() {
        if (respuestas.isEmpty()) {
            puntajeObtenido = 0.0;
        } else {
            double suma = 0.0;
            for (RespuestaCandidato respuesta : respuestas) {
                if (respuesta == null || respuesta.getOpcionSeleccionada() == null
                        || respuesta.getOpcionSeleccionada().getPuntaje() == null) {
                    throw new IllegalStateException("No se puede calcular el factor sin una ponderación registrada.");
                }
                suma += respuesta.getOpcionSeleccionada().getPuntaje();
            }
            puntajeObtenido = suma / respuestas.size();
        }
        if (id == null) id = SIGUIENTE_ID.getAndIncrement();
        return puntajeObtenido;
    }
}
