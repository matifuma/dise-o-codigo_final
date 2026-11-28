import java.util.*;

final class PreguntasSQL extends PreguntaDAO {
  private final BaseSQL db;

  PreguntasSQL(BaseSQL db, Map<Long, Factor> factores) {
    this.db = db;
    for (Pregunta p :
        db.leer(
            "SELECT * FROM pregunta ORDER BY id",
            r -> {
              long id = r.getLong("id");
              Factor f = factores.get(r.getLong("factor"));
              var opciones =
                  db.leer(
                      "SELECT o.id,o.texto,po.puntaje,o.orden,o.activo FROM pregunta_opcion po JOIN"
                          + " opcion o ON o.id=po.opcion WHERE po.pregunta=? ORDER BY o.orden",
                      o ->
                          new OpcionRespuesta(
                              o.getLong(1),
                              o.getString(2),
                              o.getDouble(3),
                              o.getInt(4),
                              o.getBoolean(5)),
                      id);
              return new Pregunta(
                  id,
                  r.getString("nombre"),
                  r.getString("enunciado"),
                  r.getString("descripcion"),
                  r.getBoolean("activo"),
                  TipoPregunta.valueOf(r.getString("tipo")),
                  f.getCompetencia(),
                  f,
                  opciones);
            })) super.crearPregunta(p);
  }

  private void guardarOpciones(Pregunta p, long id) {
    for (var o : p.getOpciones())
      db.ejecutar("INSERT INTO pregunta_opcion VALUES(?,?,?)", id, o.getId(), o.getPuntaje());
  }

  @Override
  public synchronized void crearPregunta(Pregunta p) {
    long id =
        db.transaccion(
            () -> {
              long clave =
                  db.insertar(
                      "INSERT INTO pregunta(factor,tipo,nombre,enunciado,descripcion,activo)"
                          + " VALUES(?,?,?,?,?,?)",
                      p.getFactor().getId(),
                      p.getTipo().name(),
                      p.getNombre(),
                      p.getEnunciado(),
                      p.getDescripcion(),
                      p.getActivo());
              guardarOpciones(p, clave);
              return clave;
            });
    p.setId(id);
    super.crearPregunta(p);
  }

  @Override
  public synchronized void modificarPregunta(Pregunta p) {
    db.transaccion(
        () -> {
          if (db.ejecutar(
                  "UPDATE pregunta SET tipo=?,nombre=?,enunciado=?,descripcion=? WHERE id=?",
                  p.getTipo().name(),
                  p.getNombre(),
                  p.getEnunciado(),
                  p.getDescripcion(),
                  p.getId())
              != 1) throw new IllegalStateException("Pregunta inexistente.");
          db.ejecutar("DELETE FROM pregunta_opcion WHERE pregunta=?", p.getId());
          guardarOpciones(p, p.getId());
          return null;
        });
    super.modificarPregunta(p);
  }

  @Override
  public synchronized void eliminarPregunta(Long id) {
    db.ejecutar("UPDATE pregunta SET activo=FALSE WHERE id=?", id);
    super.eliminarPregunta(id);
  }
}
