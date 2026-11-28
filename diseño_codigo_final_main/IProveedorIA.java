import java.util.Map;

/** Interfaz del proveedor de preguntas autogeneradas indicada en el UML. */
public interface IProveedorIA {
    Map<String, Object> solicitarPreguntaAutogenerada(String competencia, String factor);
}
