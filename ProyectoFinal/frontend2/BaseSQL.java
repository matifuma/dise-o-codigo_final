import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

final class BaseSQL implements AutoCloseable {
  private final Connection conexion;

  interface Leer<T> {
    T fila(ResultSet r) throws SQLException;
  }

  BaseSQL(String url) {
    try {
      conexion = Postgres.abrir(url);
      try {
        String schema = System.getenv("CAPITAL_TEST_SCHEMA")==null ? "public" : Postgres.esquemaPrueba();
        conexion.setSchema(schema);
        // Los gestores conservan entidades en memoria durante la sesión. Evitar dos escritores con cachés divergentes.
        if(!leer("SELECT pg_try_advisory_lock(hashtext(current_database()),hashtext(current_schema()))", r->r.getBoolean(1)).get(0))
          throw new IllegalStateException("Ya hay otra sesión de Capital Humano usando esta base.");
        transaccion(()->{
          script("esquema.sql");
          var versiones=leer("SELECT version FROM version_bd",r->r.getInt(1));
          if(versiones.isEmpty()){
            if(System.getenv("CAPITAL_TEST_SCHEMA")!=null || !MigracionH2.importarSiExiste(this)) script("inicial.sql");
            ejecutar("INSERT INTO version_bd VALUES(1)");
          } else if(!versiones.equals(List.of(1))) throw new IllegalStateException("Versión de base de datos incompatible.");
          return null;
        });
      } catch (RuntimeException e) {
        conexion.close();
        throw e;
      }
    } catch (SQLException e) {
      throw error(e);
    }
  }

  private void script(String nombre) {
    try (var in = Objects.requireNonNull(getClass().getResourceAsStream("/" + nombre))) {
      String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
      for (String sentencia : sql.split(";")) if (!sentencia.isBlank()) ejecutar(sentencia);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static IllegalStateException error(SQLException e) {
    return new IllegalStateException("Base SQL: " + e.getMessage(), e);
  }

  private static void vincular(PreparedStatement p, Object... argumentos) throws SQLException {
    for (int i = 0; i < argumentos.length; i++) p.setObject(i + 1, argumentos[i]);
  }

  int ejecutar(String sql, Object... argumentos) {
    try (var p = conexion.prepareStatement(sql)) {
      vincular(p, argumentos);
      return p.executeUpdate();
    } catch (SQLException e) {
      throw error(e);
    }
  }

  long insertar(String sql, Object... argumentos) {
    try (var p = conexion.prepareStatement(sql, new String[]{"id"})) {
      vincular(p, argumentos);
      p.executeUpdate();
      try (var r = p.getGeneratedKeys()) {
        if (r.next()) return r.getLong(1);
        throw new SQLException("Falta el ID generado.");
      }
    } catch (SQLException e) {
      throw error(e);
    }
  }

  <T> List<T> leer(String sql, Leer<T> lector, Object... argumentos) {
    try (var p = conexion.prepareStatement(sql)) {
      vincular(p, argumentos);
      try (var r = p.executeQuery()) {
        List<T> valores = new ArrayList<>();
        while (r.next()) valores.add(lector.fila(r));
        return valores;
      }
    } catch (SQLException e) {
      throw error(e);
    }
  }

  <T> T transaccion(Supplier<T> trabajo) {
    try {
      if (!conexion.getAutoCommit()) return trabajo.get();
      conexion.setAutoCommit(false);
      try {
        T valor = trabajo.get();
        conexion.commit();
        return valor;
      } catch (RuntimeException | Error | SQLException ex) {
        conexion.rollback();
        throw ex;
      } finally {
        conexion.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw error(e);
    }
  }

  static java.util.Date fecha(ResultSet r, String campo) throws SQLException {
    Long ms = r.getObject(campo, Long.class);
    return ms == null ? null : new java.util.Date(ms);
  }

  static Long milis(java.util.Date d) {
    return d == null ? null : d.getTime();
  }

  public void close() {
    try {
      conexion.close();
    } catch (SQLException e) {
      throw error(e);
    }
  }
}
