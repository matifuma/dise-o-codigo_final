import java.util.*;

/** Una sola instancia de cada DAO para ambos modos durante la sesión. */
final class Sesion {
  final GestorPreguntas preguntas =
      new GestorPreguntas(
          new PreguntaDAO(), null, new ServicioAuditoria(new AuditoriaDAO()), "CONSULTOR");
  final GestorFactores gestorFactores;
  final GestorCompetencias catalogo;
  final GestorPuestos puestos;
  final GestorCandidatos candidatos;
  final GestorCuestionariosSesion cuestionarios;
  final EvaluacionDAO evaluaciones =
      new EvaluacionDAO() {
        @Override
        public synchronized List<Evaluacion> obtenerEvaluacionesPorPuesto(String codigo) {
          return todos().stream().filter(e -> e.getPuesto().getCodigo().equals(codigo)).toList();
        }
      };
  final GestorReportes reportes = new GestorReportes(evaluaciones);
  final GestorOpcionesRespuesta respuestas;
  private final Map<TipoPregunta, Set<Long>> conjuntos = new EnumMap<>(TipoPregunta.class);
  final RegistroCuestionarios registro = new RegistroCuestionarios();

  Sesion() {
    var competencias = new ArrayList<Competencia>();
    var factores = new ArrayList<Factor>();
    var opciones = new ArrayList<OpcionRespuesta>();
    for (TipoPregunta tipo : TipoPregunta.values()) {
      List<String> textos =
          switch (tipo) {
            case VERDADERO_FALSO -> List.of("Verdadero", "Falso");
            case ESCALA -> List.of("Siempre", "A veces", "Nunca");
            case OPCION_MULTIPLE ->
                List.of(
                    "Analizo el contexto y planifico",
                    "Consulto otra perspectiva",
                    "Actúo sin revisar");
          };
      Set<Long> ids = new LinkedHashSet<>();
      for (int i = 0; i < textos.size(); i++) {
        long id = opciones.size() + 1;
        opciones.add(new OpcionRespuesta(id, textos.get(i), i == 0 ? 10. : 0., i + 1, true));
        ids.add(id);
      }
      conjuntos.put(tipo, ids);
    }
    respuestas = new GestorOpcionesRespuesta(new OpcionRespuestaDAO(opciones));
    String[][] ejemplos = {
      {
        "Trabajo en equipo",
        "Cooperación",
        "¿Cómo organizás una tarea compartida?",
        "¿Cómo resolvés un desacuerdo?"
      },
      {
        "Comunicación",
        "Claridad",
        "¿Cómo comunicarías un cambio de alcance?",
        "¿Cómo confirmarías que se entendió el mensaje?"
      },
      {
        "Planificación",
        "Prioridades",
        "¿Cómo priorizás una entrega urgente?",
        "¿Cómo estimás una tarea nueva?"
      }
    };
    for (String[] fila : ejemplos) {
      long id = competencias.size() + 1;
      var c = new Competencia(id, fila[0], "", true, TipoCompetencia.ACTITUDINAL, null, fila[0]);
      var f = new Factor(id, fila[1], "", 1, true, c);
      competencias.add(c);
      factores.add(f);
      for (int j = 2; j < 4; j++)
        preguntas.darDeAltaPregunta(
            c,
            f,
            fila[1] + " " + (j - 1),
            fila[j],
            "",
            opciones(TipoPregunta.OPCION_MULTIPLE),
            TipoPregunta.OPCION_MULTIPLE);
    }
    var cdao = new CompetenciaDAO(competencias);
    gestorFactores = new GestorFactores(new FactorDAO(factores), cdao);
    catalogo = new GestorCompetencias(cdao, gestorFactores, preguntas);
    var requisitos = competencias.stream().map(c -> new CompetenciaRequerida(c, 5.)).toList();
    puestos =
        new GestorPuestos(
            new PuestoDAO(
                List.of(
                    new Puesto(
                        "PST-001",
                        "Analista de sistemas",
                        "Análisis funcional",
                        true,
                        new Empresa("TechCorp"),
                        requisitos),
                    new Puesto(
                        "PST-002",
                        "Asistente administrativo",
                        "Administración",
                        true,
                        new Empresa("Empresa A"),
                        requisitos.subList(0, 1)))));
    var cands =
        new CandidatoDAO(
            List.of(
                candidato(101, "Pérez", "Ana", "39214560"),
                candidato(102, "López", "Juan", "40156232"),
                candidato(103, "Gómez", "Lucía", "38974211"),
                candidato(104, "Pérez", "Martín", "41245789")));
    candidatos = new GestorCandidatos(cands, registro);
    cuestionarios = new GestorCuestionariosSesion(cands, registro, evaluaciones);
  }

  private Candidato candidato(int numero, String apellido, String nombre, String documento) {
    return new Candidato(
        true, numero, apellido, nombre, "DNI", documento, null, null, null, null, "", false);
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
      Progreso q = new Progreso(original, sortear(original.getPuesto()));
      super.guardar(q);
      original.setId(q.getId());
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
