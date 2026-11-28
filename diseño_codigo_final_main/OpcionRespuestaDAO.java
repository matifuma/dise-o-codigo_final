import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** DAO en memoria del catálogo de opciones de respuesta. */
public class OpcionRespuestaDAO {
    private final Map<Long, OpcionRespuesta> opciones = new LinkedHashMap<>();

    public OpcionRespuestaDAO() {
        this(List.of());
    }

    /** Carga inicial de opciones para la ejecución y pruebas del CU021. */
    public OpcionRespuestaDAO(Collection<OpcionRespuesta> iniciales) {
        if (iniciales == null) return;
        for (OpcionRespuesta opcion : iniciales) {
            if (opcion == null || opcion.getId() == null) {
                throw new IllegalArgumentException("Cada opción inicial debe tener un identificador.");
            }
            if (opciones.putIfAbsent(opcion.getId(), opcion) != null) {
                throw new IllegalArgumentException("Hay identificadores de opción repetidos.");
            }
        }
    }

    /** TODO: operación de mantenimiento fuera del CU021. */
    public synchronized void crearOpcionRespuesta(OpcionRespuesta opcion) {
        throw new UnsupportedOperationException("Pendiente implementar el alta de opciones.");
    }

    /** TODO: operación de mantenimiento fuera del CU021. */
    public synchronized void modificarOpcionRespuesta(OpcionRespuesta opcion) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de opciones.");
    }

    /** TODO: operación de mantenimiento fuera del CU021. */
    public synchronized void eliminarOpcionRespuesta(Long id) {
        throw new UnsupportedOperationException("Pendiente implementar la baja de opciones.");
    }

    /**
     * Busca opciones activas por texto, sin distinguir mayúsculas ni espacios externos.
     * El conjunto de opciones es la lista 1..* asociada a una Pregunta; no requiere
     * una clase separada. OpcionRespuesta no tiene atributo nombre, por eso el criterio
     * disponible para esta consulta es su texto.
     */
    public synchronized List<OpcionRespuesta> buscarOpciones(String nombre) {
        String filtro = normalizar(nombre);
        List<OpcionRespuesta> resultado = new ArrayList<>();
        for (OpcionRespuesta opcion : opciones.values()) {
            if (!Boolean.TRUE.equals(opcion.getActivo())) continue;
            if (!filtro.isEmpty() && !normalizar(opcion.getTexto()).contains(filtro)) continue;
            resultado.add(opcion);
        }
        resultado.sort(Comparator.comparing(OpcionRespuesta::getOrden,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return resultado;
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }
}
