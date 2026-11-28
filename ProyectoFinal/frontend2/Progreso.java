import java.time.*;
import java.util.*;

/** Ejecución con persistencia SQL de CU026 sobre el Cuestionario creado por CU025. */
final class Progreso extends Cuestionario {
  record Item(Factor factor, String texto, List<OpcionRespuesta> opciones) {}

  final List<Item> items;
  final Map<Integer, OpcionRespuesta> respuestas = new HashMap<>();
  int posicion;
  private int accesos;
  private Date inicio, fin, ultimo;
  private EstadoCuestionario estado = EstadoCuestionario.ACTIVO;

  private final BaseSQL db;

  Progreso(Cuestionario origen, List<Item> items, BaseSQL db) {
    super(
        origen.getId(),
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
    this.db = db;
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

  void restaurar(
      EstadoCuestionario estado, Date inicio, Date fin, Date ultimo, int accesos, int posicion) {
    this.estado = estado;
    this.inicio = inicio;
    this.fin = fin;
    this.ultimo = ultimo;
    this.accesos = accesos;
    this.posicion = posicion;
  }

  private void actualizar(
      EstadoCuestionario nuevo, Date desde, Date hasta, Date ingreso, int cantidad, int siguiente) {
    db.ejecutar(
        "UPDATE cuestionario SET estado=?,inicio=?,fin=?,ultimo=?,accesos=?,posicion=? WHERE id=?",
        nuevo.name(),
        BaseSQL.milis(desde),
        BaseSQL.milis(hasta),
        BaseSQL.milis(ingreso),
        cantidad,
        siguiente,
        getId());
    restaurar(nuevo, desde, hasta, ingreso, cantidad, siguiente);
  }

  @Override
  public EstadoCuestionario getEstado() {
    Instant ahora = Instant.now();
    if (estado == EstadoCuestionario.ACTIVO
        && ahora.isAfter(getFechaCreacion().toInstant().plus(Duration.ofDays(15))))
      actualizar(EstadoCuestionario.SIN_CONTESTAR, inicio, fin, ultimo, accesos, posicion);
    if (estado == EstadoCuestionario.EN_CURSO
        && ahora.isAfter(inicio.toInstant().plus(Duration.ofMinutes(60))))
      actualizar(EstadoCuestionario.INCOMPLETO, inicio, fin, ultimo, accesos, posicion);
    return estado;
  }

  void verificarAcceso() {
    if (getEstado() == EstadoCuestionario.EN_CURSO && accesos >= 3)
      actualizar(EstadoCuestionario.INCOMPLETO, inicio, fin, ultimo, accesos, posicion);
    if (getEstado() != EstadoCuestionario.ACTIVO && estado != EstadoCuestionario.EN_CURSO)
      throw new IllegalArgumentException(
          "El cuestionario se encuentra " + estado.toString().replace('_', ' ') + ".");
  }

  @Override
  public void iniciar() {
    verificarAcceso();
    Date hoy = new Date();
    actualizar(
        EstadoCuestionario.EN_CURSO,
        inicio == null ? hoy : inicio,
        fin,
        hoy,
        accesos + 1,
        posicion);
  }

  void responder(int indice, OpcionRespuesta opcion) {
    if (getEstado() != EstadoCuestionario.EN_CURSO
        || indice < posicion
        || indice >= finBloque()
        || !items.get(indice).opciones().contains(opcion))
      throw new IllegalStateException("Este bloque ya no admite respuestas.");
    db.ejecutar(
        "INSERT INTO respuesta(cuestionario,item,opcion) VALUES(?,?,?) ON CONFLICT(cuestionario,item) DO UPDATE SET opcion=EXCLUDED.opcion",
        getId(),
        indice,
        opcion.getId());
    respuestas.put(indice, opcion);
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
    boolean completo = limite == items.size();
    actualizar(
        completo ? EstadoCuestionario.COMPLETADO : estado,
        inicio,
        completo ? new Date() : fin,
        ultimo,
        accesos,
        limite);
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
