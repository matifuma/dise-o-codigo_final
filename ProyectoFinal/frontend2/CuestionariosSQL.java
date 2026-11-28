import java.util.*;

final class CuestionariosSQL {
  static void insertar(BaseSQL db, Progreso q) {
    long id = db.insertar(
        "INSERT INTO cuestionario(evaluacion,candidato,clave,creado,estado,accesos,posicion) VALUES(?,?,?,?,'ACTIVO',0,0)",
        q.getEvaluacion().getId(),
        q.getCandidato().getNroCandidato(),
        q.getClave(),
        q.getFechaCreacion().getTime());
    for (int i = 0; i < q.items.size(); i++) {
      var item = q.items.get(i);
      db.ejecutar("INSERT INTO item VALUES(?,?,?,?)", id, i, item.factor().getId(), item.texto());
      for (var o : item.opciones())
        db.ejecutar(
            "INSERT INTO alternativa VALUES(?,?,?,?,?,?)",
            id,
            i,
            o.getId(),
            o.getTexto(),
            o.getPuntaje(),
            o.getOrden());
    }
    q.setId(id);
  }

  static List<Progreso> cargar(
      BaseSQL db,
      Map<Long, Evaluacion> evaluaciones,
      Map<Integer, Candidato> personas,
      Map<Long, Factor> factores) {
    return db.leer(
        "SELECT * FROM cuestionario ORDER BY id",
        r -> {
          long id = r.getLong("id");
          var estado = EstadoCuestionario.valueOf(r.getString("estado"));
          var original =
              new Cuestionario(
                  id,
                  BaseSQL.fecha(r, "creado"),
                  2,
                  2,
                  BaseSQL.fecha(r, "inicio"),
                  BaseSQL.fecha(r, "fin"),
                  null,
                  r.getInt("accesos"),
                  r.getString("clave"),
                  estado,
                  personas.get(r.getInt("candidato")),
                  evaluaciones.get(r.getLong("evaluacion")));
          var items =
              db.leer(
                  "SELECT * FROM item WHERE cuestionario=? ORDER BY orden",
                  it -> {
                    var ops =
                        db.leer(
                            "SELECT * FROM alternativa WHERE cuestionario=? AND item=? ORDER BY"
                                + " orden",
                            o ->
                                new OpcionRespuesta(
                                    o.getLong("opcion"),
                                    o.getString("texto"),
                                    o.getDouble("puntaje"),
                                    o.getInt("orden"),
                                    true),
                            id,
                            it.getInt("orden"));
                    return new Progreso.Item(
                        factores.get(it.getLong("factor")), it.getString("enunciado"), ops);
                  },
                  id);
          var q = new Progreso(original, items, db);
          q.restaurar(
              estado,
              original.getFechaInicio(),
              original.getFechaFin(),
              BaseSQL.fecha(r, "ultimo"),
              original.getCantidadAccesos(),
              r.getInt("posicion"));
          for (var respuesta :
              db.leer(
                  "SELECT item,opcion FROM respuesta WHERE cuestionario=?",
                  a -> Map.entry(a.getInt(1), a.getLong(2)),
                  id)) {
            int indice = respuesta.getKey();
            q.respuestas.put(
                indice,
                items.get(indice).opciones().stream()
                    .filter(o -> o.getId().equals(respuesta.getValue()))
                    .findFirst()
                    .orElseThrow());
          }
          return q;
        });
  }
}
