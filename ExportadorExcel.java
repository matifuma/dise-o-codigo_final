import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Exporta a SpreadsheetML (XML de Excel 2003) sin bibliotecas externas. */
public class ExportadorExcel {
    private static final String NS = "urn:schemas-microsoft-com:office:spreadsheet";

    /** El destino es la ruta completa del archivo con extensión .xls. */
    public void exportarDatos(List<?> contenido, String destino) {
        if (contenido == null) throw new IllegalArgumentException("El contenido a exportar es obligatorio.");
        if (destino == null || destino.trim().isEmpty()) throw new IllegalArgumentException("El destino es obligatorio.");
        Path archivo = Paths.get(destino).toAbsolutePath().normalize();
        if (Files.isDirectory(archivo)) throw new IllegalArgumentException("El destino debe incluir el nombre del archivo.");
        try {
            Path carpeta = archivo.getParent();
            if (carpeta != null) Files.createDirectories(carpeta);
            try (BufferedWriter writer = Files.newBufferedWriter(archivo, StandardCharsets.UTF_8)) {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                writer.write("<?mso-application progid=\"Excel.Sheet\"?>\n");
                writer.write("<Workbook xmlns=\"" + NS + "\" xmlns:ss=\"" + NS + "\">\n");
                writer.write("<Worksheet ss:Name=\"Claves de acceso\"><Table>\n");
                fila(writer, "Nro. candidato", "Apellido", "Nombre", "Tipo documento",
                        "Nro. documento", "Email", "Clave de ingreso");
                for (Object registro : contenido) {
                    if (!(registro instanceof CandidatoConClaveDTO candidato)) {
                        throw new IllegalArgumentException("La exportación espera elementos CandidatoConClaveDTO.");
                    }
                    fila(writer, candidato.getNroCandidato(), candidato.getApellido(), candidato.getNombre(),
                            candidato.getTipoDocumento(), candidato.getNroDocumento(), candidato.getEmail(),
                            candidato.getClave());
                }
                writer.write("</Table></Worksheet></Workbook>\n");
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir el archivo Excel en " + archivo, e);
        }
    }

    private void fila(BufferedWriter writer, Object... valores) throws IOException {
        writer.write("<Row>");
        for (Object valor : valores) {
            writer.write("<Cell><Data ss:Type=\"String\">");
            writer.write(escapar(valor == null ? "" : String.valueOf(valor)));
            writer.write("</Data></Cell>");
        }
        writer.write("</Row>\n");
    }

    private String escapar(String valor) {
        return valor.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
