import java.util.List;

/** Contrato común de exportación de archivos del módulo de reportes. */
public interface IExportadorArchivos {
    void exportarDatos(List<?> contenido, String destino);
}
