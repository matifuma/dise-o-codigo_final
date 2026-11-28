import java.time.*;
import java.util.*;

/** Ejecución con persistencia SQL de CU026 sobre el Cuestionario creado por CU025. */
final class Progreso extends Cuestionario {
  record Item(Long idPregunta, Factor factor, String texto, List<OpcionRespuesta> opciones) {}

  final List<Item> items;
  final Map<Integer, OpcionRespuesta> respuestas = new HashMap<>();
  int posicion;
  private int accesos;
  private Date inicio, fin, ultimo;
  private EstadoCuestionario estado = EstadoCuestionario.ACTIVO;

  private final BaseSQL db;

  final ConfiguracionSistema configuracion;

  Progreso(Cuestionario origen, List<Item> items, BaseSQL db) {
    this(origen, items, db, 1L);
  }

  Progreso(Cuestionario origen, List<Item> items, BaseSQL db, long configuracionId) {
    super(
        origen.getId(),
        origen.getFechaCreacion(),
        Objects.requireNonNullElse(origen.getCantidadPreguntasPorFactor(), 2),
        Objects.requireNonNullElse(origen.getCantidadPreguntasPorBloque(), 2),
        null,
        null,
        origen.getFechaCreacion(),
        0,
        origen.getClave(),
        EstadoCuestionario.ACTIVO,
        origen.getCandidato(),
        origen.getEvaluacion());
    configuracion = db.leer("""
        SELECT "instruccionesCuestionario","tiempoMaximoMinutos","cantidadAccesosMaximos"
        FROM "ConfiguracionSistema" WHERE "idConfiguracionSistema"=?
        """, r -> new ConfiguracionSistema(configuracionId, r.getString(1), r.getInt(2), r.getInt(3)), configuracionId).get(0);
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
    if (estado == EstadoCuestionario.COMPLETADO && getResultados().isEmpty()) cargarResultados();
  }

  private void actualizar(
      EstadoCuestionario nuevo, Date desde, Date hasta, Date ingreso, int cantidad, int siguiente) {
    db.ejecutar(
        "UPDATE app_cuestionario SET estado=?,inicio=?,fin=?,ultimo=?,accesos=?,posicion=? WHERE id=?",
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
        && ahora.isAfter(inicio.toInstant().plus(Duration.ofMinutes(configuracion.getTiempoMaximoMinutos()))))
      actualizar(EstadoCuestionario.INCOMPLETO, inicio, fin, ultimo, accesos, posicion);
    return estado;
  }

  void verificarAcceso() {
    if (getEstado() == EstadoCuestionario.EN_CURSO && accesos >= configuracion.getCantidadAccesosMaximos())
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
        """
        INSERT INTO "RespuestaCandidato" ("idCuestionario","ordenGlobal","idPlantilla","fechaRespuesta","idBloqueCuestionario","idPregunta","idOpcionRespuesta")
        SELECT i."idCuestionario",i."ordenGlobal",a."idPlantilla",(extract(epoch FROM clock_timestamp())*1000)::bigint,
               i."idBloqueCuestionario",i."idPregunta",a."idOpcionRespuesta"
        FROM incluye i JOIN "AlternativaEmitida" a USING ("idCuestionario","ordenGlobal")
        WHERE i."idCuestionario"=? AND i."ordenGlobal"=? AND a."idPlantilla"=?
        ON CONFLICT ("idCuestionario","ordenGlobal") DO UPDATE SET
          "idPlantilla"=EXCLUDED."idPlantilla","idOpcionRespuesta"=EXCLUDED."idOpcionRespuesta","fechaRespuesta"=EXCLUDED."fechaRespuesta"
        """,
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

  private void cargarResultados() {
    var requeridas = new HashMap<Long,CompetenciaRequerida>();
    for (var req : getPuesto().getCompetenciasRequeridas()) requeridas.put(req.getCompetencia().getId(),req);
    db.leer("""
        SELECT c."idResultadoCompetencia",c."idCompetencia",c."puntajeObtenido",r."fechaCalculo"
        FROM "ResultadoCompetencia" c JOIN "Resultado" r USING ("idResultado") WHERE r."idCuestionario"=?
        """, r -> Resultado.restaurar(requeridas.get(r.getLong(2)),r.getLong(1),r.getDouble(3),BaseSQL.fecha(r,"fechaCalculo")),getId())
      .forEach(this::agregarResultado);
  }

  boolean cumple() {
    return getEstado() == EstadoCuestionario.COMPLETADO && !getResultados().isEmpty()
        && getResultados().stream().allMatch(Resultado::cumpleMinimo);
  }

  double total() {
    return getResultados().stream().mapToDouble(Resultado::getPuntajeObtenido).average().orElse(0);
  }
}
