import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

/** Escribe resultados y órdenes de mérito en formato CSV UTF-8. */
public class ExportadorCSV implements IExportadorArchivos {

    @Override
    public void exportarDatos(List<?> contenido, String destino) {
        if (contenido == null) throw new IllegalArgumentException("El contenido a exportar es obligatorio.");
        if (destino == null || destino.trim().isEmpty()) throw new IllegalArgumentException("El destino es obligatorio.");

        Path archivo = Paths.get(destino).toAbsolutePath().normalize();
        if (Files.isDirectory(archivo)) throw new IllegalArgumentException("El destino debe incluir el nombre del archivo.");
        try {
            Path carpeta = archivo.getParent();
            if (carpeta != null) Files.createDirectories(carpeta);
            try (BufferedWriter writer = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
                if (contenido.size() == 1 && contenido.get(0) instanceof ReporteOrdenMeritoDTO reporte) {
                    exportarReporte(writer, reporte);
                } else {
                    exportarFilas(writer, contenido);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir el archivo CSV en " + archivo, e);
        }
    }

    private void exportarReporte(BufferedWriter writer, ReporteOrdenMeritoDTO reporte) throws IOException {
        fila(writer, "Puesto", reporte.getNombrePuesto(), "Empresa", reporte.getNombreEmpresa());
        fila(writer, "Fecha de generación", fecha(reporte.getFechaGeneracion()));
        fila(writer, "Tipo de documento", "Nro. documento", "Apellido y nombre",
                "Puntaje obtenido", "Estado del cuestionario", "Superó mínimos");
        for (ItemOrdenMeritoDTO item : reporte.getCandidatosOrdenados()) {
            fila(writer, item.getTipoDoc(), item.getNroDoc(), item.getApellidoYNombre(),
                    item.getPuntajeObtenido(), item.getEstadoCuestionario(), item.getSuperoMinimos());
        }
    }

    private void exportarFilas(BufferedWriter writer, List<?> contenido) throws IOException {
        fila(writer, "Nro. candidato", "Apellido", "Nombre", "Puntaje obtenido",
                "Fecha de cálculo", "Estado del cuestionario");
        for (Object registro : contenido) {
            if (!(registro instanceof ResultadoExportacionDTO resultado)) {
                throw new IllegalArgumentException("La exportación de filas espera ResultadoExportacionDTO.");
            }
            fila(writer, resultado.getNroCandidato(), resultado.getApellido(), resultado.getNombre(),
                    resultado.getPuntajeObtenido(), fecha(resultado.getFechaCalculo()),
                    resultado.getEstadoCuestionario());
        }
    }

    private void fila(BufferedWriter writer, Object... valores) throws IOException {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) writer.write(',');
            writer.write(celda(valores[i] == null ? "" : String.valueOf(valores[i])));
        }
        writer.write("\r\n");
    }

    private String celda(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    private String fecha(java.util.Date fecha) {
        return fecha == null ? "" : Instant.ofEpochMilli(fecha.getTime()).toString();
    }
}
