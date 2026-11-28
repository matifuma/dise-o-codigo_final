import java.util.*;

final class PreguntasSQL extends PreguntaDAO {
  private final BaseSQL db;

  PreguntasSQL(BaseSQL db, Map<Long, Factor> factores) {
    this.db = db;
    for (Pregunta p :
        db.leer(
            "SELECT * FROM app_pregunta ORDER BY id",
            r -> {
              long id = r.getLong("id");
              Factor f = factores.get(r.getLong("factor"));
              var opciones =
                  db.leer(
                      """
                      SELECT "idPlantilla",texto,puntaje,orden,activo FROM "OpcionRespuesta"
                      WHERE "idPregunta"=? AND activo ORDER BY orden
                      """,
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
      db.ejecutar("""
          INSERT INTO "OpcionRespuesta" ("idPregunta","idPlantilla",texto,puntaje,orden,activo)
          VALUES(?,?,?,?,?,true) ON CONFLICT ("idPregunta","idPlantilla")
          DO UPDATE SET texto=EXCLUDED.texto,puntaje=EXCLUDED.puntaje,orden=EXCLUDED.orden,activo=true
          """, id, o.getId(), o.getTexto(), o.getPuntaje(), o.getOrden());
  }

  @Override
  public synchronized void crearPregunta(Pregunta p) {
    long id =
        db.transaccion(
            () -> {
              long clave =
                  db.insertar(
                      "INSERT INTO app_pregunta(factor,tipo,nombre,enunciado,descripcion,activo)"
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
                  "UPDATE app_pregunta SET tipo=?,nombre=?,enunciado=?,descripcion=? WHERE id=?",
                  p.getTipo().name(),
                  p.getNombre(),
                  p.getEnunciado(),
                  p.getDescripcion(),
                  p.getId())
              != 1) throw new IllegalStateException("Pregunta inexistente.");
          db.ejecutar("UPDATE \"OpcionRespuesta\" SET activo=false WHERE \"idPregunta\"=?", p.getId());
          guardarOpciones(p, p.getId());
          return null;
        });
    super.modificarPregunta(p);
  }

  @Override
  public synchronized void eliminarPregunta(Long id) {
    db.ejecutar("UPDATE app_pregunta SET activo=FALSE WHERE id=?", id);
    super.eliminarPregunta(id);
  }
}
