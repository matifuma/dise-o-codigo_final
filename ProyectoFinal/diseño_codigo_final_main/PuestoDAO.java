import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** DAO temporal en memoria para puestos. */
public class PuestoDAO {
    private final Map<String, Puesto> puestos = new LinkedHashMap<>();

    public PuestoDAO() { this(List.of()); }

    /** Carga inicial para el adaptador en memoria mientras no se conecte la base de datos. */
    public PuestoDAO(Collection<Puesto> puestosIniciales) {
        if (puestosIniciales == null) return;
        for (Puesto puesto : puestosIniciales) {
            validar(puesto);
            if (puestos.putIfAbsent(puesto.getCodigo(), puesto) != null) {
                throw new IllegalArgumentException("Hay códigos de puesto repetidos en la carga inicial.");
            }
        }
    }

    /** TODO fuera de CU025: persistir altas de puestos. */
    public synchronized void crearPuesto(Puesto puesto) {
        throw new UnsupportedOperationException("Pendiente implementar la persistencia de puestos.");
    }

    /** TODO fuera de CU025: persistir modificaciones de puestos. */
    public synchronized void modificarPuesto(Puesto puesto) {
        throw new UnsupportedOperationException("Pendiente implementar la modificación de puestos.");
    }

    /** TODO fuera de CU025: definir baja lógica/física en el repositorio definitivo. */
    public synchronized void eliminarPuesto(String codigo) {
        throw new UnsupportedOperationException("Pendiente implementar la baja de puestos.");
    }

    /** Búsqueda de CU028 con los tres filtros visibles en la pantalla. */
    public synchronized List<Puesto> buscarPuestos(String codigo, String nombre, String empresa) {
        List<Puesto> resultado = new ArrayList<>();
        for (Puesto puesto : puestos.values()) {
            if (!Boolean.TRUE.equals(puesto.getActivo())) continue;
            if (!contiene(puesto.getCodigo(), codigo)) continue;
            if (!contiene(puesto.getNombre(), nombre)) continue;
            String nombreEmpresa = puesto.getEmpresa() == null ? "" : puesto.getEmpresa().getNombre();
            if (!contiene(nombreEmpresa, empresa)) continue;
            resultado.add(puesto);
        }
        return resultado;
    }

    /** Conserva la firma anterior utilizada por casos de uso ya implementados. */
    public synchronized List<Puesto> buscarPuestos(String nombre, String empresa) {
        return buscarPuestos(null, nombre, empresa);
    }

    /** Recupera la entidad completa elegida desde la lista de resultados. */
    public synchronized Puesto obtenerPuesto(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("El código del puesto es obligatorio.");
        }
        return puestos.get(codigo.trim());
    }

    private boolean contiene(String valor, String filtro) {
        return filtro == null || filtro.trim().isEmpty() || normalizar(valor).contains(normalizar(filtro));
    }
    private String normalizar(String valor) { return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT); }
    private void validar(Puesto puesto) {
        if (puesto == null || puesto.getCodigo() == null) throw new IllegalArgumentException("El puesto es obligatorio.");
    }
}
