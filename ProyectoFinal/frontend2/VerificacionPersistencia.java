import java.nio.file.*;
import java.util.*;

/** Se ejecuta en tres procesos separados para verificar cierres reales de la conexión. */
final class VerificacionPersistencia {
  private static void comprobar(boolean ok, String detalle) {
    if (!ok) throw new AssertionError(detalle);
  }

  public static void main(String[] args) throws Exception {
    Postgres.esquemaPrueba();
    try (var s = new Sesion(Postgres.url())) {
      var puesto = s.puestos.buscarPuestos("Analista", "").get(0);
      var candidato = s.candidatos.buscarCandidatos("Gómez", "", null).get(0);
      switch (args[1]) {
        case "crear" -> {
          var c = s.catalogo.buscarCompetencia("").get(0);
          var f = s.gestorFactores.buscarFactor(c).get(0);
          s.preguntas.darDeAltaPregunta(
              c,
              f,
              "Persistencia",
              "¿Se conserva esta pregunta?",
              "Inicial",
              s.opciones(TipoPregunta.ESCALA),
              TipoPregunta.ESCALA);
          var p = s.preguntas.buscarPregunta(c, f, "Persistencia").get(0);
          s.preguntas.modificarPregunta(
              p,
              TipoPregunta.ESCALA,
              "Persistencia",
              "¿Se conserva esta pregunta?",
              "Editada",
              s.opciones(TipoPregunta.ESCALA));
          var claves = s.cuestionarios.generarClavesAcceso(List.<CandidatoDTO>of(candidato));
          s.cuestionarios.evaluarCandidatos(claves, puesto);
          var q = s.registro.porClave(claves.get(0).getClave());
          q.iniciar();
          for (int i = 0; i < q.finBloque(); i++) q.responder(i, q.items.get(i).opciones().get(0));
          q.confirmarBloque();
          q.responder(q.posicion, q.items.get(q.posicion).opciones().get(0));
          Files.writeString(
              Path.of(args[0] + ".control"),
              q.getClave()
                  + "\n"
                  + q.items.stream()
                      .map(Progreso.Item::texto)
                      .reduce((a, b) -> a + "\n" + b)
                      .orElseThrow());
        }
        case "retomar" -> {
          var lineas = Files.readAllLines(Path.of(args[0] + ".control"));
          var q = s.registro.porClave(lineas.get(0));
          comprobar(
              q.posicion == 2 && q.respuestas.size() == 3 && q.getCantidadAccesos() == 1,
              "Bloque, borrador y accesos");
          comprobar(
              q.items.stream()
                  .map(Progreso.Item::texto)
                  .toList()
                  .equals(lineas.subList(1, lineas.size())),
              "Misma selección de preguntas");
          var p = s.preguntas.buscarPregunta(null, null, "persistencia").get(0);
          comprobar("EDITADA".equals(p.getDescripcion()), "Edición de pregunta");
          s.preguntas.darDeBajaPregunta(p);
          q.iniciar();
          while (q.posicion < q.items.size()) {
            for (int i = q.posicion; i < q.finBloque(); i++)
              q.responder(i, q.items.get(i).opciones().get(0));
            q.confirmarBloque();
          }
        }
        case "reporte" -> {
          var q = s.registro.porClave(Files.readAllLines(Path.of(args[0] + ".control")).get(0));
          comprobar(
              q.getEstado() == EstadoCuestionario.COMPLETADO
                  && q.getCantidadAccesos() == 2
                  && q.cumple(),
              "Cuestionario finalizado");
          comprobar(
              s.reportes
                  .emitirOrdenMerito(puesto, List.of(q.getEvaluacion().getId()))
                  .leerCuestionarios()
                  .contains(q),
              "Reporte reconstruido");
          comprobar(
              s.preguntas.buscarPregunta(null, null, "persistencia").isEmpty(), "Baja lógica");
          comprobar(
              s.db.leer("SELECT COUNT(*) FROM app_auditoria", r -> r.getInt(1)).get(0) == 1,
              "Auditoría persistida");
          comprobar(s.db.leer("""
              SELECT (SELECT count(*) FROM "Resultado" WHERE cumplimiento AND "puntajeObtenido"=10)=1
                AND (SELECT count(*) FROM "ResultadoCompetencia" WHERE "cumpleMinimo")=3
                AND (SELECT count(*) FROM "ResultadoFactor" WHERE "puntajeObtenido"=10)=3
                AND (SELECT count(*) FROM "BloqueCuestionario" WHERE completado AND "fechaCompletado" IS NOT NULL)=3
                AND (SELECT count(*) FROM "RespuestaCandidato" WHERE "fechaRespuesta" IS NOT NULL
                     AND "idPregunta" IS NOT NULL AND "idOpcionRespuesta" IS NOT NULL)=6
                AND EXISTS (SELECT 1 FROM "RegistroEjecucion" WHERE accion='Confirmar bloque')
              """, r -> r.getBoolean(1)).get(0), "Relaciones del DER después de reabrir");
          var claves = s.cuestionarios.generarClavesAcceso(List.<CandidatoDTO>of(candidato));
          s.cuestionarios.evaluarCandidatos(claves, puesto);
          comprobar(
              s.registro.porClave(claves.get(0).getClave()).getId() > q.getId(),
              "Nuevo cuestionario después de reabrir");
        }
        default -> throw new IllegalArgumentException("Fase desconocida");
      }
    }
  }
}
