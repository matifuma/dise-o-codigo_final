/** Parámetros del sistema utilizados por la ejecución de cuestionarios. */
public class ConfiguracionSistema {
    private final Long id;
    private String instruccionesCuestionario;
    private Integer tiempoMaximoMinutos;
    private final Integer cantidadAccesosMaximos;

    public ConfiguracionSistema(Long id, String instruccionesCuestionario,
                                Integer tiempoMaximoMinutos, Integer cantidadAccesosMaximos) {
        if (tiempoMaximoMinutos == null || tiempoMaximoMinutos <= 0
                || cantidadAccesosMaximos == null || cantidadAccesosMaximos <= 0) {
            throw new IllegalArgumentException("El tiempo y la cantidad máxima de accesos deben ser positivos.");
        }
        this.id = id;
        this.instruccionesCuestionario = instruccionesCuestionario == null ? "" : instruccionesCuestionario;
        this.tiempoMaximoMinutos = tiempoMaximoMinutos;
        this.cantidadAccesosMaximos = cantidadAccesosMaximos;
    }

    public Long getId() { return id; }
    public String getInstruccionesCuestionario() { return instruccionesCuestionario; }
    public Integer getTiempoMaximoMinutos() { return tiempoMaximoMinutos; }
    public Integer getCantidadAccesosMaximos() { return cantidadAccesosMaximos; }

    /** Operación del modelo; la configuración ya está disponible en esta entidad. */
    public ConfiguracionSistema obtenerParametros() { return this; }

    public void modificarParametros(String instrucciones, Integer tiempoMaximo) {
        if (tiempoMaximo == null || tiempoMaximo <= 0) {
            throw new IllegalArgumentException("El tiempo máximo debe ser positivo.");
        }
        instruccionesCuestionario = instrucciones == null ? "" : instrucciones;
        tiempoMaximoMinutos = tiempoMaximo;
    }
}
