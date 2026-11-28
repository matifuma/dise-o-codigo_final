import java.util.*;

final class GestorReportes {
  private final EvaluacionDAO evaluaciones;

  GestorReportes(EvaluacionDAO evaluaciones) {
    this.evaluaciones = evaluaciones;
  }

  Map<String, List<Progreso>> emitirOrdenMerito(Puesto puesto, List<Long> idsEvaluaciones) {
    Map<String, List<Progreso>> grupos = new LinkedHashMap<>();
    grupos.put("Orden de mérito", new ArrayList<>());
    for (var ev : evaluaciones.obtenerEvaluacionesPorPuesto(puesto.getCodigo()))
      if (idsEvaluaciones.contains(ev.getId()))
        for (Cuestionario base : ev.obtenerCuestionarios()) {
          var q = (Progreso) base;
          String nombre =
              q.cumple()
                  ? "Orden de mérito"
                  : q.getEstado() == EstadoCuestionario.COMPLETADO
                      ? "Completados sin alcanzar los mínimos"
                      : q.getEstado().name().replace('_', ' ');
          grupos.computeIfAbsent(nombre, k -> new ArrayList<>()).add(q);
        }
    grupos.get("Orden de mérito").sort(Comparator.comparingDouble(Progreso::total).reversed());
    return grupos;
  }
}
