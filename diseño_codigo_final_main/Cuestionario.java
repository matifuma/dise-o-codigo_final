import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final List<BloqueCuestionario> bloques = new ArrayList<>();
    private final List<Resultado> resultados = new ArrayList<>();

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
    public List<BloqueCuestionario> getBloques() { return Collections.unmodifiableList(bloques); }
    public List<Resultado> getResultados() { return Collections.unmodifiableList(resultados); }

    void setId(Long id) { this.id = id; }

    void setCantidadPreguntasPorFactor(Integer cantidad) { this.cantidadPreguntasPorFactor = cantidad; }
    void setCantidadPreguntasPorBloque(Integer cantidad) { this.cantidadPreguntasPorBloque = cantidad; }

    void agregarBloque(BloqueCuestionario bloque) {
        if (bloque == null) throw new IllegalArgumentException("El bloque es obligatorio.");
        if (!bloques.contains(bloque)) {
            bloque.setCuestionario(this);
            bloques.add(bloque);
        }
    }

    void agregarResultado(Resultado resultado) {
        if (resultado != null && !resultados.contains(resultado)) resultados.add(resultado);
    }

    void registrarAcceso() {
        cantidadAccesos = (cantidadAccesos == null ? 0 : cantidadAccesos) + 1;
    }

    /** Registra la fecha de inicio al aceptar las instrucciones por primera vez. */
    public void iniciar() {
        if (fechaInicio == null) fechaInicio = new Date();
    }

    /**
     * Devuelve true cuando el plazo ya se agotó o superó el máximo y false si aún no venció.
     * En ACTIVO cuenta desde la activación; en EN_CURSO desde el primer inicio.
     */
    public boolean validarTiempoMaximo(Integer tiempoMaximoMinutos) {
        if (tiempoMaximoMinutos == null || tiempoMaximoMinutos <= 0) {
            throw new IllegalArgumentException("El tiempo máximo debe ser positivo.");
        }
        if (estado != EstadoCuestionario.ACTIVO && estado != EstadoCuestionario.EN_CURSO) return false;
        Date inicioDelPlazo;
        if (estado == EstadoCuestionario.ACTIVO) {
            // La secuencia contempla el vencimiento antes del primer ingreso al cuestionario.
            inicioDelPlazo = fechaCambioEstado != null ? fechaCambioEstado : fechaCreacion;
        } else {
            inicioDelPlazo = fechaInicio != null ? fechaInicio : fechaCambioEstado;
        }
        if (inicioDelPlazo == null) return false;
        long milisegundosMaximos = tiempoMaximoMinutos.longValue() * 60_000L;
        return System.currentTimeMillis() - inicioDelPlazo.getTime() >= milisegundosMaximos;
    }

    /** Registra el nuevo estado y la fecha del cambio correspondiente. */
    public void cambiarEstado(EstadoCuestionario estado) {
        if (estado == null) throw new IllegalArgumentException("El estado es obligatorio.");
        if (this.estado == estado) return;
        boolean valido = this.estado == EstadoCuestionario.PENDIENTE && estado == EstadoCuestionario.ACTIVO
                || this.estado == EstadoCuestionario.ACTIVO
                && (estado == EstadoCuestionario.EN_CURSO || estado == EstadoCuestionario.INCOMPLETO
                || estado == EstadoCuestionario.SIN_CONTESTAR)
                || this.estado == EstadoCuestionario.EN_CURSO
                && (estado == EstadoCuestionario.COMPLETADO || estado == EstadoCuestionario.INCOMPLETO);
        if (!valido) {
            throw new IllegalStateException("Transición de estado no permitida: " + this.estado + " -> " + estado);
        }
        this.estado = estado;
        fechaCambioEstado = new Date();
    }

    /** Registra el instante en el que se completó el último bloque. */
    public void finalizar() {
        if (estado != EstadoCuestionario.EN_CURSO) {
            throw new IllegalStateException("Solo se puede finalizar un cuestionario EN_CURSO.");
        }
        if (fechaFin == null) fechaFin = new Date();
    }

    private Date copia(Date fecha) { return fecha == null ? null : new Date(fecha.getTime()); }
}
