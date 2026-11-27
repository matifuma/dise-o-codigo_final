import java.util.Date;

/** Cuestionario de un candidato, asociado a la evaluación y al puesto seleccionados en CU025. */
public class Cuestionario {
    private Long id;
    private Date fechaCreacion;
    private Integer cantidadPreguntasPorFactor;
    private Integer cantidadPreguntasPorBloque;
    private Date fechaInicio;
    private Date fechaFin;
    private Date fechaCambioEstado;
    private Integer cantidadAccesos;
    private String clave;
    private EstadoCuestionario estado;
    private Candidato candidato;
    private Evaluacion evaluacion;

    public Cuestionario(Long id, Date fechaCreacion, Integer cantidadPreguntasPorFactor,
                        Integer cantidadPreguntasPorBloque, Date fechaInicio, Date fechaFin,
                        Date fechaCambioEstado, Integer cantidadAccesos, String clave,
                        EstadoCuestionario estado, Candidato candidato, Evaluacion evaluacion) {
        this.id = id;
        this.fechaCreacion = copia(fechaCreacion);
        this.cantidadPreguntasPorFactor = cantidadPreguntasPorFactor;
        this.cantidadPreguntasPorBloque = cantidadPreguntasPorBloque;
        this.fechaInicio = copia(fechaInicio);
        this.fechaFin = copia(fechaFin);
        this.fechaCambioEstado = copia(fechaCambioEstado);
        this.cantidadAccesos = cantidadAccesos;
        this.clave = clave;
        this.estado = estado;
        this.candidato = candidato;
        this.evaluacion = evaluacion;
    }

    /** Constructor compacto que aparece en la secuencia corregida de CU025. */
    public Cuestionario(EstadoCuestionario estado, String clave,
                        Candidato candidato, Evaluacion evaluacion) {
        this(null, new Date(), null, null, null, null, new Date(), 0,
                clave, estado, candidato, evaluacion);
    }

    public Long getId() { return id; }
    public Date getFechaCreacion() { return copia(fechaCreacion); }
    public Integer getCantidadPreguntasPorFactor() { return cantidadPreguntasPorFactor; }
    public Integer getCantidadPreguntasPorBloque() { return cantidadPreguntasPorBloque; }
    public Date getFechaInicio() { return copia(fechaInicio); }
    public Date getFechaFin() { return copia(fechaFin); }
    public Date getFechaCambioEstado() { return copia(fechaCambioEstado); }
    public Integer getCantidadAccesos() { return cantidadAccesos; }
    public String getClave() { return clave; }
    public EstadoCuestionario getEstado() { return estado; }
    public Candidato getCandidato() { return candidato; }
    public Evaluacion getEvaluacion() { return evaluacion; }
    public Puesto getPuesto() { return evaluacion == null ? null : evaluacion.getPuesto(); }

    void setId(Long id) { this.id = id; }

    /** TODO fuera de CU025: iniciar y controlar los límites de ejecución del cuestionario. */
    public void iniciar() {
        throw new UnsupportedOperationException("Pendiente implementar el inicio del cuestionario.");
    }

    /** TODO fuera de CU025: requiere la configuración temporal definida por el sistema. */
    public Boolean validarTiempoMaximo() {
        throw new UnsupportedOperationException("Pendiente definir la regla de tiempo máximo.");
    }

    /** TODO fuera de CU025: falta acordar transiciones válidas entre estados. */
    public void cambiarEstado(EstadoCuestionario estado) {
        throw new UnsupportedOperationException("Pendiente definir las transiciones de estado.");
    }

    /** TODO fuera de CU025: la finalización depende del flujo de respuestas. */
    public void finalizar() {
        throw new UnsupportedOperationException("Pendiente implementar la finalización del cuestionario.");
    }

    private Date copia(Date fecha) { return fecha == null ? null : new Date(fecha.getTime()); }
}
