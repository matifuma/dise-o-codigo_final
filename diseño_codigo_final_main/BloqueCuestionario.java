import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Bloque que agrupa preguntas y las respuestas entregadas por el candidato. */
public class BloqueCuestionario {
    private static final AtomicLong SIGUIENTE_ID = new AtomicLong(1L);

    private Long id;
    private Integer numero;
    private Boolean completado;
    private Date fechaCompletado;
    private Cuestionario cuestionario;
    private final List<Pregunta> preguntas = new ArrayList<>();
    private final List<RespuestaCandidato> respuestas = new ArrayList<>();

    public BloqueCuestionario(Long id, Integer numero, Boolean completado, Date fechaCompletado) {
        this.id = id;
        this.numero = numero;
        this.completado = completado;
        this.fechaCompletado = fechaCompletado == null ? null : new Date(fechaCompletado.getTime());
    }

    BloqueCuestionario(Integer numero, List<Pregunta> preguntas) {
        this(SIGUIENTE_ID.getAndIncrement(), numero, false, null);
        if (preguntas != null) this.preguntas.addAll(preguntas);
    }

    public Long getId() { return id; }
    public Integer getNumero() { return numero; }
    public Boolean getCompletado() { return completado; }
    public Date getFechaCompletado() {
        return fechaCompletado == null ? null : new Date(fechaCompletado.getTime());
    }
    public Cuestionario getCuestionario() { return cuestionario; }
    public List<Pregunta> getPreguntas() { return Collections.unmodifiableList(preguntas); }
    public List<RespuestaCandidato> getRespuestas() { return Collections.unmodifiableList(respuestas); }

    void setCuestionario(Cuestionario cuestionario) { this.cuestionario = cuestionario; }

    /** Mantiene la asociación UML entre el bloque y las preguntas que presenta. */
    void agregarPregunta(Pregunta pregunta) {
        if (pregunta != null && !preguntas.contains(pregunta)) preguntas.add(pregunta);
    }

    /** Mantiene la asociación UML entre el bloque y las respuestas recibidas. */
    public void agregarRespuesta(RespuestaCandidato respuesta) {
        if (respuesta == null) throw new IllegalArgumentException("La respuesta es obligatoria.");
        if (!preguntas.contains(respuesta.getPregunta())) {
            throw new IllegalArgumentException("La respuesta debe corresponder a una pregunta de este bloque.");
        }
        for (RespuestaCandidato existente : respuestas) {
            if (existente.getPregunta().equals(respuesta.getPregunta())) {
                throw new IllegalArgumentException("Ya existe una respuesta para esa pregunta.");
            }
        }
        respuestas.add(respuesta);
    }

    /** Verifica que cada pregunta del bloque tenga exactamente una opción válida. */
    public Boolean validarRespuestasCompletas() {
        if (preguntas.isEmpty()) return false;
        if (respuestas.size() != preguntas.size()) return false;
        for (Pregunta pregunta : preguntas) {
            long cantidad = respuestas.stream().filter(r -> pregunta.equals(r.getPregunta())
                    && pregunta.getOpciones().contains(r.getOpcionSeleccionada())
                    && Boolean.TRUE.equals(r.getOpcionSeleccionada().getActivo())).count();
            if (cantidad != 1) return false;
        }
        return true;
    }

    /** Marca el bloque como terminado después de registrar todas sus respuestas. */
    public void marcarCompletado() {
        if (!Boolean.TRUE.equals(validarRespuestasCompletas())) {
            throw new IllegalStateException("No se puede completar un bloque con respuestas pendientes.");
        }
        completado = true;
        if (fechaCompletado == null) fechaCompletado = new Date();
    }
}
