/** Acceso en memoria a la configuración del sistema hasta definir la persistencia final. */
public class ConfiguracionDAO {
    private ConfiguracionSistema configuracion;

    public ConfiguracionDAO() { }

    public ConfiguracionDAO(ConfiguracionSistema configuracion) {
        this.configuracion = configuracion;
    }

    public synchronized ConfiguracionSistema obtenerParametros() {
        if (configuracion == null) {
            throw new IllegalStateException("No se cargó la configuración del sistema.");
        }
        return configuracion;
    }

    public synchronized void actualizarParametros(ConfiguracionSistema configuracion) {
        if (configuracion == null) throw new IllegalArgumentException("La configuración es obligatoria.");
        this.configuracion = configuracion;
    }
}
