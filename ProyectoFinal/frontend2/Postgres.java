import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/** Conexión externa configurable o servidor PostgreSQL local de este usuario. */
final class Postgres {
  static final String LOCAL = "jdbc:postgresql://127.0.0.1:55432/capital_humano";
  static final Path RAIZ =
      Path.of(
          System.getenv().getOrDefault("LOCALAPPDATA", System.getProperty("user.home")),
          "CapitalHumano");
  private static final Pattern PRUEBA = Pattern.compile("prueba_[a-f0-9]{32}");

  static String url() {
    return System.getenv().getOrDefault("CAPITAL_DB_URL", LOCAL);
  }

  static boolean local() {
    return System.getenv("CAPITAL_DB_URL") == null;
  }

  static Connection abrir(String url) throws SQLException {
    Properties credenciales = new Properties();
    if (local()) {
      if (!LOCAL.equals(url)) throw new SQLException("URL local inesperada.");
      prepararLocal();
      credenciales.setProperty("user", "capital_app");
      credenciales.setProperty("password", clave("app"));
    } else {
      if (!url.startsWith("jdbc:postgresql:"))
        throw new SQLException("Se requiere una URL JDBC de PostgreSQL 18.");
      credenciales.setProperty(
          "user", Objects.requireNonNullElse(System.getenv("CAPITAL_DB_USER"), "capital_app"));
      if (System.getenv("CAPITAL_DB_PASSWORD") != null)
        credenciales.setProperty("password", System.getenv("CAPITAL_DB_PASSWORD"));
    }
    var conexion = DriverManager.getConnection(url, credenciales);
    if (conexion.getMetaData().getDatabaseMajorVersion() != 18) {
      conexion.close();
      throw new SQLException("Este proyecto requiere PostgreSQL 18.");
    }
    return conexion;
  }

  private static String clave(String nombre) throws SQLException {
    try {
      return Files.readString(RAIZ.resolve("postgres-local").resolve(nombre + ".key")).trim();
    } catch (IOException e) {
      throw new SQLException("No se pudo leer la configuración local de PostgreSQL.", e);
    }
  }

  private static boolean preparado;

  private static synchronized void prepararLocal() throws SQLException {
    if (preparado) return;
    try {
      Path script = Files.createTempFile("capital-db-", ".ps1");
      try (var in = Postgres.class.getResourceAsStream("/postgres-local.ps1")) {
        Files.copy(Objects.requireNonNull(in), script, StandardCopyOption.REPLACE_EXISTING);
        var proceso =
            new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    script.toString())
                .redirectErrorStream(true)
                .start();
        String salida = new String(proceso.getInputStream().readAllBytes());
        if (proceso.waitFor() != 0)
          throw new SQLException("No se pudo iniciar PostgreSQL local: " + salida);
      } finally {
        Files.deleteIfExists(script);
      }
    } catch (IOException e) {
      throw new SQLException("Error preparando PostgreSQL local.", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SQLException(e);
    }
    try (var c =
            DriverManager.getConnection(
                LOCAL.replace("/capital_humano", "/postgres"), "capital_admin", clave("admin"));
        var st = c.createStatement()) {
      st.execute("SELECT pg_advisory_lock(180020025)");
      try (var r = st.executeQuery("SELECT 1 FROM pg_roles WHERE rolname='capital_app'")) {
        if (!r.next()) {
          String pw = clave("app");
          if (!pw.matches("[A-F0-9]{64}")) throw new SQLException("Credencial local inválida.");
          try (var alta = c.createStatement()) {
            alta.execute("CREATE ROLE capital_app LOGIN PASSWORD '" + pw + "'");
          }
        }
      }
      try (var r = st.executeQuery("SELECT 1 FROM pg_database WHERE datname='capital_humano'")) {
        if (!r.next())
          try (var alta = c.createStatement()) {
            alta.execute("CREATE DATABASE capital_humano OWNER capital_app ENCODING 'UTF8'");
          }
      }
    }
    preparado = true;
  }

  static String esquemaPrueba() {
    String schema = System.getenv("CAPITAL_TEST_SCHEMA");
    if (schema == null || !PRUEBA.matcher(schema).matches())
      throw new IllegalStateException("La verificación requiere un esquema de prueba aislado.");
    return schema;
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) throw new IllegalArgumentException();
    if (args[0].equals("inicializar")) {
      try (var sesion = new Sesion(url())) {
        System.out.println("PostgreSQL 18: base de la aplicación lista.");
      }
      return;
    }
    String schema = esquemaPrueba();
    try (var c = abrir(url());
        var s = c.createStatement()) {
      switch (args[0]) {
        case "crear-prueba" -> s.execute("CREATE SCHEMA " + schema);
        case "limpiar-prueba" -> s.execute("DROP SCHEMA " + schema + " CASCADE");
        default -> throw new IllegalArgumentException();
      }
    }
  }
}
