import java.time.*;
import java.util.*;

/** Ejecución en memoria de CU026 sobre el Cuestionario creado por CU025. */
final class Progreso extends Cuestionario {
  record Item(Factor factor, String texto, List<OpcionRespuesta> opciones) {}

  final List<Item> items;
  final Map<Integer, OpcionRespuesta> respuestas = new HashMap<>();
  int posicion;
  private int accesos;
  private Date inicio, fin, ultimo;
  private EstadoCuestionario estado = EstadoCuestionario.ACTIVO;

  Progreso(Cuestionario origen, List<Item> items) {
    super(
        null,
        origen.getFechaCreacion(),
        2,
        2,
        null,
        null,
        origen.getFechaCreacion(),
        0,
        origen.getClave(),
        EstadoCuestionario.ACTIVO,
        origen.getCandidato(),
        origen.getEvaluacion());
    this.items = items;
  }

  private static Date copia(Date d) {
    return d == null ? null : new Date(d.getTime());
  }

  @Override
  public Date getFechaInicio() {
    return copia(inicio);
  }

  @Override
  public Date getFechaFin() {
    return copia(fin);
  }

  @Override
  public Integer getCantidadAccesos() {
    return accesos;
  }

  Date ultimoIngreso() {
    return copia(ultimo);
  }

  @Override
  public EstadoCuestionario getEstado() {
    Instant ahora = Instant.now();
    if (estado == EstadoCuestionario.ACTIVO
        && ahora.isAfter(getFechaCreacion().toInstant().plus(Duration.ofDays(15))))
      estado = EstadoCuestionario.SIN_CONTESTAR;
    if (estado == EstadoCuestionario.EN_CURSO
        && ahora.isAfter(inicio.toInstant().plus(Duration.ofMinutes(60))))
      estado = EstadoCuestionario.INCOMPLETO;
    return estado;
  }

  void verificarAcceso() {
    if (getEstado() == EstadoCuestionario.EN_CURSO && accesos >= 3)
      estado = EstadoCuestionario.INCOMPLETO;
    if (getEstado() != EstadoCuestionario.ACTIVO && getEstado() != EstadoCuestionario.EN_CURSO)
      throw new IllegalArgumentException(
          "El cuestionario se encuentra " + getEstado().name().replace('_', ' ') + ".");
  }

  @Override
  public void iniciar() {
    verificarAcceso();
    ultimo = new Date();
    accesos++;
    if (inicio == null) inicio = copia(ultimo);
    estado = EstadoCuestionario.EN_CURSO;
  }

  int finBloque() {
    return Math.min(posicion + getCantidadPreguntasPorBloque(), items.size());
  }

  void confirmarBloque() {
    if (getEstado() != EstadoCuestionario.EN_CURSO)
      throw new IllegalStateException("Se agotó el tiempo del cuestionario.");
    var omitidas = new ArrayList<String>();
    int limite = finBloque();
    for (int i = posicion; i < limite; i++)
      if (!respuestas.containsKey(i)) omitidas.add(String.valueOf(i + 1));
    if (!omitidas.isEmpty())
      throw new IllegalArgumentException("Falta responder: " + String.join(", ", omitidas));
    posicion = limite;
    if (posicion == items.size()) {
      estado = EstadoCuestionario.COMPLETADO;
      fin = new Date();
    }
  }

  double puntaje(Competencia c) {
    // Cada factor aporta dos preguntas: primero se promedia el factor y luego la competencia.
    return java.util.stream.IntStream.range(0, items.size())
        .filter(i -> items.get(i).factor().getCompetencia().getId().equals(c.getId()))
        .mapToDouble(i -> respuestas.get(i).getPuntaje())
        .average()
        .orElse(0.);
  }

  boolean cumple() {
    return getEstado() == EstadoCuestionario.COMPLETADO
        && getPuesto().getCompetenciasRequeridas().stream()
            .allMatch(r -> puntaje(r.getCompetencia()) >= r.getPuntajeMinimo());
  }

  double total() {
    return getPuesto().getCompetenciasRequeridas().stream()
        .mapToDouble(r -> puntaje(r.getCompetencia()))
        .sum();
  }
}
