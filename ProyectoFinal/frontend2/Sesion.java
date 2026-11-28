import java.util.*;
import java.util.function.Function;

final class Sesion implements AutoCloseable {
  final BaseSQL db;
  final GestorPreguntas preguntas;
  final GestorFactores gestorFactores;
  final GestorCompetencias catalogo;
  final GestorPuestos puestos;
  final GestorCandidatos candidatos;
  final GestorCuestionariosSesion cuestionarios;
  final GestorOpcionesRespuesta respuestas;
  final GestorReportes reportes;
  final RegistroCuestionarios registro = new RegistroCuestionarios();
  final EvaluacionDAO evaluaciones =
      new EvaluacionDAO() {
        @Override
        public synchronized List<Evaluacion> obtenerEvaluacionesPorPuesto(String codigo) {
          return todos().stream().filter(e -> e.getPuesto().getCodigo().equals(codigo)).toList();
        }

        @Override
        public synchronized void guardar(Evaluacion ev) {
          if (ev.getId() == null) {
            ev.setId(db.insertar("INSERT INTO evaluacion(puesto,fecha) VALUES(?,?)",
                ev.getPuesto().getCodigo(), ev.getFecha().getTime()));
          }
          super.guardar(ev);
        }
      };
  private final Map<TipoPregunta, Set<Long>> conjuntos = new EnumMap<>(TipoPregunta.class);

  private static <K, T> Map<K, T> indice(List<T> valores, Function<T, K> clave) {
    Map<K, T> map = new LinkedHashMap<>();
    for (T v : valores) map.put(clave.apply(v), v);
    return map;
  }

  Sesion(String url) {
    db = new BaseSQL(url);
    try {
      var comps =
          indice(
              db.leer(
                  "SELECT * FROM competencia ORDER BY id",
                  r ->
                      new Competencia(
                          r.getLong("id"),
                          r.getString("nombre"),
                          r.getString("descripcion"),
                          r.getBoolean("activo"),
                          TipoCompetencia.valueOf(r.getString("tipo")),
                          r.getString("tecnologia"),
                          r.getString("comportamiento"))),
              Competencia::getId);
      var factores =
          indice(
              db.leer(
                  "SELECT * FROM factor ORDER BY id",
                  r ->
                      new Factor(
                          r.getLong("id"),
                          r.getString("nombre"),
                          r.getString("descripcion"),
                          r.getInt("orden"),
                          r.getBoolean("activo"),
                          comps.get(r.getLong("competencia")))),
              Factor::getId);
      var opciones =
          db.leer(
              "SELECT * FROM opcion ORDER BY id",
              r -> {
                conjuntos
                    .computeIfAbsent(
                        TipoPregunta.valueOf(r.getString("tipo")), k -> new LinkedHashSet<>())
                    .add(r.getLong("id"));
                return new OpcionRespuesta(
                    r.getLong("id"),
                    r.getString("texto"),
                    r.getDouble("puntaje"),
                    r.getInt("orden"),
                    r.getBoolean("activo"));
              });
      respuestas = new GestorOpcionesRespuesta(new OpcionRespuestaDAO(opciones));
      var auditoria =
          new AuditoriaDAO() {
            @Override
            public void registrarAuditoria(RegistroAuditoria reg) {
              db.ejecutar(
                  "INSERT INTO auditoria(fecha,detalle,entidad) VALUES(?,?,?)",
                  reg.getFechaHora().getTime(),
                  reg.getDetalleOperacion(),
                  reg.getEntidadAfectada());
            }

            @Override
            public List<RegistroAuditoria> obtenerAuditorias() {
              return db.leer(
                  "SELECT * FROM auditoria ORDER BY id",
                  r ->
                      new RegistroAuditoria(
                          BaseSQL.fecha(r, "fecha"),
                          r.getString("detalle"),
                          r.getString("entidad")));
            }
          };
      preguntas =
          new GestorPreguntas(
              new PreguntasSQL(db, factores), null, new ServicioAuditoria(auditoria), "CONSULTOR") {
            @Override
            public void darDeBajaPregunta(Pregunta p) {
              db.transaccion(
                  () -> {
                    super.darDeBajaPregunta(p);
                    return null;
                  });
            }

            @Override
            public void modificarPregunta(
                Pregunta p,
                TipoPregunta tipo,
                String nombre,
                String enunciado,
                String descripcion,
                List<OpcionRespuesta> opciones) {
              var anteriorTipo = p.getTipo();
              var anteriorNombre = p.getNombre();
              var anteriorTexto = p.getEnunciado();
              var anteriorDesc = p.getDescripcion();
              var anteriores = List.copyOf(p.getOpciones());
              try {
                super.modificarPregunta(p, tipo, nombre, enunciado, descripcion, opciones);
              } catch (RuntimeException e) {
                p.setTipo(anteriorTipo);
                p.setNombre(anteriorNombre);
                p.setEnunciado(anteriorTexto);
                p.setDescripcion(anteriorDesc);
                p.reemplazarOpciones(anteriores);
                throw e;
              }
            }
          };
      var cdao = new CompetenciaDAO(comps.values());
      gestorFactores = new GestorFactores(new FactorDAO(factores.values()), cdao);
      catalogo = new GestorCompetencias(cdao, gestorFactores, preguntas);
      var puestosPorCodigo =
          indice(
              db.leer(
                  "SELECT * FROM puesto ORDER BY codigo",
                  r -> {
                    var requisitos =
                        db.leer(
                            "SELECT * FROM requisito WHERE puesto=? ORDER BY competencia",
                            m ->
                                new CompetenciaRequerida(
                                    comps.get(m.getLong("competencia")), m.getDouble("minimo")),
                            r.getString("codigo"));
                    return new Puesto(
                        r.getString("codigo"),
                        r.getString("nombre"),
                        r.getString("descripcion"),
                        r.getBoolean("activo"),
                        new Empresa(r.getString("empresa")),
                        requisitos);
                  }),
              Puesto::getCodigo);
      puestos = new GestorPuestos(new PuestoDAO(puestosPorCodigo.values()));
      var personas =
          indice(
              db.leer(
                  "SELECT * FROM candidato ORDER BY numero",
                  r ->
                      new Candidato(
                          r.getBoolean("activo"),
                          r.getInt("numero"),
                          r.getString("apellido"),
                          r.getString("nombre"),
                          r.getString("tipo_doc"),
                          r.getString("documento"),
                          BaseSQL.fecha(r, "nacimiento"),
                          r.getString("genero"),
                          r.getString("nacionalidad"),
                          r.getString("escolaridad"),
                          r.getString("email"),
                          r.getBoolean("eliminado"))),
              Candidato::getNroCandidato);
      var cands = new CandidatoDAO(personas.values());
      var evs =
          indice(
              db.leer(
                  "SELECT * FROM evaluacion ORDER BY id",
                  r ->
                      new Evaluacion(
                          r.getLong("id"),
                          BaseSQL.fecha(r, "fecha"),
                          puestosPorCodigo.get(r.getString("puesto")))),
              Evaluacion::getId);
      evs.values().forEach(evaluaciones::guardar);
      for (Progreso q : CuestionariosSQL.cargar(db, evs, personas, factores)) registro.restaurar(q);
      candidatos = new GestorCandidatos(cands, registro);
      cuestionarios = new GestorCuestionariosSesion(cands, registro, evaluaciones, db);
      reportes = new GestorReportes(evaluaciones);
    } catch (RuntimeException e) {
      db.close();
      throw e;
    }
  }

  public void close() {
    db.close();
  }

  List<OpcionRespuesta> opciones(TipoPregunta tipo) {
    return respuestas.buscarOpciones("").stream()
        .filter(o -> conjuntos.get(tipo).contains(o.getId()))
        .sorted(Comparator.comparing(OpcionRespuesta::getOrden))
        .toList();
  }

  List<Factor> factoresDisponibles() {
    return catalogo.buscarCompetencia("").stream()
        .flatMap(c -> gestorFactores.buscarFactor(c).stream())
        .toList();
  }

  List<Progreso.Item> sortear(Puesto puesto) {
    List<Progreso.Item> items = new ArrayList<>();
    for (var requisito : puesto.getCompetenciasRequeridas())
      for (var factor : gestorFactores.buscarFactor(requisito.getCompetencia())) {
        var grupo = new ArrayList<>(preguntas.buscarPregunta(factor));
        Collections.shuffle(grupo);
        if (grupo.size() < 2)
          throw new IllegalStateException("Faltan preguntas para " + factor.getNombre());
        grupo.stream()
            .limit(2)
            .forEach(
                p ->
                    items.add(
                        new Progreso.Item(factor, p.getEnunciado(), List.copyOf(p.getOpciones()))));
      }
    Collections.shuffle(items);
    return List.copyOf(items);
  }

  /** Extiende el guardado de CU025 con los bloques que requiere CU026. */
  final class RegistroCuestionarios extends CuestionarioDAO {
    @Override
    public synchronized void guardar(Cuestionario original) {
      Progreso q = new Progreso(original, sortear(original.getPuesto()), db);
      CuestionariosSQL.insertar(db, q);
      super.guardar(q);
      original.setId(q.getId());
    }

    void restaurar(Progreso q) {
      super.guardar(q);
    }

    @Override
    synchronized void eliminar(Cuestionario original) {
      todos().stream()
          .filter(q -> Objects.equals(q.getId(), original.getId()))
          .findFirst()
          .ifPresent(super::eliminar);
    }

    Progreso porClave(String clave) {
      return todos().stream()
          .filter(q -> q.getClave().equalsIgnoreCase(clave.trim()))
          .map(Progreso.class::cast)
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("La clave ingresada no existe."));
    }
  }
}
