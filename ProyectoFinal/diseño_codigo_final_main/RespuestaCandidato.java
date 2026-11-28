import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/** Respuesta a una pregunta, asociada a la opción elegida por el candidato. */
public class RespuestaCandidato {
    private static final AtomicLong SIGUIENTE_ID = new AtomicLong(1L);

    private Long id;
    private Date fechaRespuesta;
    private final Pregunta pregunta;
    private final OpcionRespuesta opcionSeleccionada;

    public RespuestaCandidato(Pregunta pregunta, OpcionRespuesta opcionSeleccionada) {
        if (pregunta == null || opcionSeleccionada == null
                || !pregunta.getOpciones().contains(opcionSeleccionada)) {
            throw new IllegalArgumentException("La pregunta y una de sus opciones son obligatorias.");
        }
        this.pregunta = pregunta;
        this.opcionSeleccionada = opcionSeleccionada;
    }

    public Long getId() { return id; }
    public Date getFechaRespuesta() {
        return fechaRespuesta == null ? null : new Date(fechaRespuesta.getTime());
    }
    public Pregunta getPregunta() { return pregunta; }
    public OpcionRespuesta getOpcionSeleccionada() { return opcionSeleccionada; }

    /** Registra la respuesta una sola vez; el almacenamiento definitivo se resolverá con el DAO. */
    public synchronized void registrar() {
        if (id == null) id = SIGUIENTE_ID.getAndIncrement();
        if (fechaRespuesta == null) fechaRespuesta = new Date();
    }
}
