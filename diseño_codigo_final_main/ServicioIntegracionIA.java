import java.util.Map;

/** Adaptador de integración definido por el diagrama de clases. */
public class ServicioIntegracionIA implements IProveedorIA {
    private final API_Externa_IA apiExterna;

    public ServicioIntegracionIA(API_Externa_IA apiExterna) {
        if (apiExterna == null) {
            throw new IllegalArgumentException("La API externa es obligatoria.");
        }
        this.apiExterna = apiExterna;
    }

    public API_Externa_IA getApiExterna() {
        return apiExterna;
    }

    @Override
    public Map<String, Object> solicitarPreguntaAutogenerada(String competencia, String factor) {
        // TODO: implementar petición, manejo de errores y adaptación de respuesta.
        throw new UnsupportedOperationException("Pendiente integrar el proveedor externo de IA.");
    }

    public Map<String, Object> adaptarFormato() {
        // TODO: el UML no define entrada/salida detallada para la transformación.
        throw new UnsupportedOperationException("Pendiente definir el formato de la respuesta de IA.");
    }
}
