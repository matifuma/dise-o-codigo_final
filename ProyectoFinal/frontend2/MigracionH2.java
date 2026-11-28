import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Importación de la base anterior, solo en la primera inicialización y en una transacción. */
final class MigracionH2 {
  private static final List<String> TABLAS =
      List.of(
          "competencia",
          "factor",
          "opcion",
          "pregunta",
          "pregunta_opcion",
          "empresa",
          "puesto",
          "requisito",
          "candidato",
          "evaluacion",
          "cuestionario",
          "item",
          "alternativa",
          "respuesta",
          "auditoria");

  static boolean importarSiExiste(BaseSQL destino) {
    String origen = System.getenv("CAPITAL_H2_ORIGEN");
    if (origen == null && !Postgres.local()) return false;
    Path archivo =
        origen == null ? Postgres.RAIZ.resolve("datos/evaluaciones.mv.db") : Path.of(origen);
    if (!Files.exists(archivo)) {
      if (origen != null) throw new IllegalArgumentException("No se encontró la base H2 indicada.");
      return false;
    }
    try {
      if (Files.size(archivo) == 0) return false;
      String ruta = archivo.toAbsolutePath().toString();
      if (!ruta.endsWith(".mv.db"))
        throw new IllegalArgumentException("El origen debe ser un archivo .mv.db.");
      String url =
          "jdbc:h2:file:"
              + ruta.substring(0, ruta.length() - 6)
              + ";IFEXISTS=TRUE;ACCESS_MODE_DATA=r";
      try (var h2 = DriverManager.getConnection(url, "sa", "");
          var st = h2.createStatement()) {
        try (var v = st.executeQuery("SELECT version FROM version_bd")) {
          if (!v.next() || v.getInt(1) != 1 || v.next())
            throw new SQLException("Versión de origen H2 desconocida.");
        }
        for (String tabla : TABLAS) {
          try (var filas = st.executeQuery("SELECT * FROM " + tabla)) {
            var meta = filas.getMetaData();
            int n = meta.getColumnCount();
            List<String> columnas = new ArrayList<>();
            for (int i = 1; i <= n; i++) {
              String nombre = meta.getColumnName(i).toLowerCase(Locale.ROOT);
              if (!nombre.matches("[a-z_]+"))
                throw new SQLException("Columna de origen desconocida.");
              columnas.add(nombre);
            }
            String insertar =
                "INSERT INTO "
                    + tabla
                    + " ("
                    + String.join(",", columnas)
                    + ") VALUES ("
                    + String.join(",", Collections.nCopies(n, "?"))
                    + ")";
            long total = 0;
            while (filas.next()) {
              Object[] datos = new Object[n];
              for (int i = 0; i < n; i++) datos[i] = filas.getObject(i + 1);
              destino.ejecutar(insertar, datos);
              total++;
            }
            long almacenadas =
                destino.leer("SELECT count(*) FROM " + tabla, r -> r.getLong(1)).get(0);
            if (almacenadas != total)
              throw new SQLException("Cantidad de filas distinta en " + tabla);
          }
        }
        for (String tabla : List.of("pregunta", "evaluacion", "cuestionario", "auditoria")) {
          destino.leer(
              "SELECT setval(pg_get_serial_sequence('"
                  + tabla
                  + "','id'), COALESCE(MAX(id),1), COUNT(*)>0) FROM "
                  + tabla,
              r -> r.getLong(1));
        }
        return true;
      }
    } catch (Exception e) {
      throw new IllegalStateException(
          "No se pudo migrar H2. Cerrá la versión anterior y volvé a abrir Capital Humano. "
              + e.getMessage(),
          e);
    }
  }
}
