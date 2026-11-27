import java.util.Date;

/** Entidad auxiliar del flujo futuro de ejecución del cuestionario. */
public class BloqueCuestionario {
    private Long id;
    private Integer numero;
    private Boolean completado;
    private Date fechaCompletado;

    public BloqueCuestionario(Long id, Integer numero, Boolean completado, Date fechaCompletado) {
        this.id = id;
        this.numero = numero;
        this.completado = completado;
        this.fechaCompletado = fechaCompletado == null ? null : new Date(fechaCompletado.getTime());
    }

    /** TODO fuera de CU025: depende de las respuestas y opciones de cada pregunta. */
    public Boolean validarRespuestasCompletas() {
        throw new UnsupportedOperationException("Pendiente implementar la validación del bloque.");
    }

    /** TODO fuera de CU025: definir qué respuestas se registran al completar el bloque. */
    public void marcarCompletado() {
        throw new UnsupportedOperationException("Pendiente implementar la finalización del bloque.");
    }
}
