import java.time.*;
import java.util.*;
import javafx.beans.property.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class EvaluacionesUI {
  final Aplicacion a;

  EvaluacionesUI(Aplicacion a) {
    this.a = a;
  }

  static TableView<CandidatoDTO> candidatos(
      List<CandidatoDTO> datos, Set<CandidatoDTO> marcas, Set<CandidatoDTO> bloqueados) {
    var t = Aplicacion.tabla(datos);
    t.setEditable(true);
    TableColumn<CandidatoDTO, Boolean> check = new TableColumn<>("✓");
    check.setMaxWidth(46);
    check.setCellFactory(
        col ->
            new TableCell<>() {
              final CheckBox caja = new CheckBox();

              {
                caja.setOnAction(
                    e -> {
                      var c = getTableRow().getItem();
                      if (caja.isSelected()) marcas.add(c);
                      else marcas.remove(c);
                    });
              }

              protected void updateItem(Boolean value, boolean vacio) {
                super.updateItem(value, vacio);
                var c = getTableRow() == null ? null : getTableRow().getItem();
                if (vacio || c == null) {
                  setGraphic(null);
                  return;
                }
                caja.setSelected(marcas.contains(c));
                caja.setDisable(bloqueados.contains(c));
                setGraphic(caja);
              }
            });
    check.setCellValueFactory(row -> new SimpleBooleanProperty(marcas.contains(row.getValue())));
    t.getColumns().add(check);
    Aplicacion.col(t, "Apellido", CandidatoDTO::getApellido);
    Aplicacion.col(t, "Nombre", CandidatoDTO::getNombre);
    Aplicacion.col(t, "Número de candidato", CandidatoDTO::getNroCandidato);
    t.setPrefHeight(145);
    return t;
  }

  private final Map<Integer, CandidatoDTO> visibles = new HashMap<>();

  void seleccion() {
    visibles.clear();
    seleccion(new LinkedHashSet<>());
  }

  void seleccion(Set<CandidatoDTO> seleccion) {
    a.pagina("Evaluar candidatos", a::menu);
    var apellido = Aplicacion.texto("Buscá por apellido");
    var nombre = Aplicacion.texto("Buscá por nombre");
    var numero = Aplicacion.texto("Número de candidato");
    Set<CandidatoDTO> agregar = new LinkedHashSet<>(), quitar = new LinkedHashSet<>();
    var resultados = candidatos(List.of(), agregar, seleccion);
    var elegidos = candidatos(List.copyOf(seleccion), quitar, Set.of());
    Runnable refrescar =
        () -> {
          elegidos.getItems().setAll(seleccion);
          agregar.clear();
          quitar.clear();
          resultados.refresh();
          elegidos.refresh();
        };
    var mas =
        a.accion(
            "Agregar",
            () -> {
              if (agregar.isEmpty())
                throw new IllegalArgumentException("Marcá al menos un candidato.");
              seleccion.addAll(agregar);
              refrescar.run();
            });
    var menos =
        a.secundario(
            "Quitar",
            () -> {
              if (quitar.isEmpty())
                throw new IllegalArgumentException("Marcá los candidatos que querés quitar.");
              seleccion.removeAll(quitar);
              refrescar.run();
            });
    mas.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(resultados.getItems()));
    menos.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(elegidos.getItems()));
    a.cuerpo
        .getChildren()
        .addAll(
            Aplicacion.titulo("Búsqueda de candidatos"),
            Aplicacion.fila(
                Aplicacion.campo("Apellido", apellido),
                Aplicacion.campo("Nombre", nombre),
                Aplicacion.campo("Número de candidato", numero),
                a.accion(
                    "Buscar",
                    () -> {
                      if (!numero.getText().isBlank()
                          && !numero.getText().trim().matches("[0-9]{1,9}"))
                        throw new IllegalArgumentException(
                            "Ingresá un número de candidato válido.");
                      agregar.clear();
                      resultados
                          .getItems()
                          .setAll(
                              a
                                  .sesion
                                  .candidatos
                                  .buscarCandidatos(
                                      apellido.getText(),
                                      nombre.getText(),
                                      numero.getText().isBlank()
                                          ? null
                                          : Integer.valueOf(numero.getText().trim()))
                                  .stream()
                                  .map(c -> visibles.computeIfAbsent(c.getNroCandidato(), id -> c))
                                  .toList());
                    })),
            Aplicacion.titulo("Resultados de búsqueda"),
            resultados,
            mas,
            Aplicacion.titulo("Candidatos a evaluar"),
            elegidos,
            menos);
    a.pie(
        a.secundario("Cancelar", a::menu),
        a.accion(
            "Siguiente",
            () -> {
              if (seleccion.isEmpty())
                throw new IllegalArgumentException("Seleccioná uno o más candidatos.");
              var activos =
                  a.sesion.candidatos.validarCandidatos(seleccion).stream()
                      .map(c -> c.getNombre() + " " + c.getApellido())
                      .toList();
              if (!activos.isEmpty())
                throw new IllegalArgumentException(
                    "Ya tienen cuestionarios activos o en proceso: " + String.join(", ", activos));
              puesto(seleccion, null);
            }));
  }

  void puesto(Set<CandidatoDTO> seleccion, Puesto anterior) {
    a.pagina("Evaluar candidatos", () -> seleccion(seleccion));
    var combo =
        Aplicacion.combo(
            a.sesion.puestos.buscarPuestos("", ""), Puesto::getNombre, "Seleccione un puesto");
    TextField emp = new TextField(), codigo = new TextField();
    emp.setEditable(false);
    codigo.setEditable(false);
    TableView<CompetenciaRequeridaDTO> t = Aplicacion.tabla(List.of());
    Aplicacion.col(t, "Competencia", CompetenciaRequeridaDTO::getNombreCompetencia);
    Aplicacion.col(t, "Ponderación mínima", CompetenciaRequeridaDTO::getPuntajeMinimo);
    combo
        .valueProperty()
        .addListener(
            (ob, v, p) -> {
              if (p != null) {
                emp.setText(p.getEmpresa().getNombre());
                codigo.setText(p.getCodigo());
                t.getItems().setAll(a.sesion.catalogo.buscarTodasLasCompetenciasPorPuesto(p));
              }
            });
    combo.setValue(anterior);
    a.cuerpo
        .getChildren()
        .addAll(
            Aplicacion.titulo("Selección de puesto"),
            Aplicacion.fila(
                Aplicacion.campo("Puesto a evaluar", combo),
                Aplicacion.campo("Empresa", emp),
                Aplicacion.campo("Código del puesto", codigo)),
            Aplicacion.titulo("Competencias requeridas"),
            t);
    a.pie(
        a.secundario("Cancelar", a::menu),
        a.secundario("Atrás", () -> seleccion(seleccion)),
        a.accion(
            "Siguiente",
            () -> {
              if (combo.getValue() == null)
                throw new IllegalArgumentException("Seleccioná un puesto.");
              try {
                var puesto = combo.getValue();
                var invalidas = a.sesion.catalogo.validarCompetencia(puesto);
                if (!invalidas.isEmpty())
                  throw new IllegalArgumentException(
                      "Competencias sin suficientes preguntas: "
                          + String.join(
                              ", ",
                              invalidas.stream()
                                  .map(CompetenciaRequeridaDTO::getNombreCompetencia)
                                  .toList()));
                confirmacion(
                    seleccion, puesto, a.sesion.cuestionarios.generarClavesAcceso(seleccion));
              } catch (IllegalArgumentException e) {
                a.informar(e.getMessage());
                a.menu();
              }
            }));
  }

  void confirmacion(Set<CandidatoDTO> seleccion, Puesto puesto, List<CandidatoConClaveDTO> claves) {
    a.pagina("Candidatos a evaluar", () -> puesto(seleccion, puesto));
    var t = Aplicacion.tabla(claves);
    Aplicacion.col(t, "Apellido", CandidatoDTO::getApellido);
    Aplicacion.col(t, "Nombre", CandidatoDTO::getNombre);
    Aplicacion.col(t, "Tipo de documento", CandidatoDTO::getTipoDocumento);
    Aplicacion.col(t, "Nro. documento", CandidatoDTO::getNroDocumento);
    Aplicacion.col(t, "Clave de ingreso", CandidatoConClaveDTO::getClave);
    a.cuerpo
        .getChildren()
        .addAll(Aplicacion.titulo(puesto.getNombre() + " · " + puesto.getEmpresa().getNombre()), t);
    a.pie(
        a.secundario("Cancelar", a::menu),
        a.secundario("Atrás", () -> puesto(seleccion, puesto)),
        a.accion(
            "Finalizar",
            () -> {
              a.sesion.cuestionarios.evaluarCandidatos(claves, puesto);
              while (a.confirmar(
                  "Evaluación guardada",
                  "¿Desea exportar las claves de ingreso a Excel?",
                  "Sí",
                  "No")) {
                var dialogo = new javafx.stage.DirectoryChooser();
                dialogo.setTitle("Guardar claves de candidatos");
                var carpeta = dialogo.showDialog(a.ventana);
                if (carpeta == null) continue;
                try {
                  a.sesion.cuestionarios.exportarResultado(
                      puesto, claves, carpeta.getAbsolutePath());
                  break;
                } catch (IllegalStateException ex) {
                  a.informar(ex.getMessage());
                }
              }
              a.menu();
            }));
  }

  void login() {
    a.pagina("Completar cuestionario", a::modos);
    var clave = Aplicacion.texto("Clave de 8 caracteres");
    clave.setMaxWidth(310);
    a.cuerpo.getChildren().addAll(Aplicacion.titulo("Ingrese su clave de acceso"), clave);
    var ingresar =
        a.accion(
            "Ingresar",
            () -> {
              var q = a.sesion.registro.porClave(clave.getText());
              q.verificarAcceso();
              if (q.getFechaInicio() == null) instrucciones(q);
              else iniciar(q);
            });
    ingresar.setDefaultButton(true);
    a.pie(a.secundario("Cancelar", a::modos), ingresar);
  }

  void instrucciones(Progreso q) {
    a.pagina("Completar cuestionario", a::modos);
    var info =
        new Label(
            "Respondé todas las preguntas de cada bloque antes de avanzar. Los bloques confirmados"
                + " quedan cerrados. Podés salir y retomar con la misma clave. Disponés de 60"
                + " minutos y hasta tres ingresos.");
    info.setWrapText(true);
    a.cuerpo.getChildren().addAll(Aplicacion.titulo("Instrucciones del cuestionario"), info);
    a.pie(a.secundario("Cancelar", a::modos), a.accion("Aceptar", () -> iniciar(q)));
  }

  void iniciar(Progreso q) {
    bloque(
        a.sesion.cuestionarios.iniciarCuestionario(
            q.getCandidato().getTipoDocumento(), q.getCandidato().getNroDocumento(), q.getClave()));
  }

  void bloque(Progreso q) {
    Runnable salir =
        () -> {
          if (a.confirmar(
              "Salir del cuestionario", "¿Desea guardar el progreso y salir?", "Sí", "No"))
            a.modos();
        };
    a.pagina("Completar cuestionario", salir);
    var bloque = a.sesion.cuestionarios.obtenerBloquePendiente(q);
    int fin = bloque.fin;
    ProgressBar progreso = new ProgressBar((double) q.posicion / q.items.size());
    progreso.setMaxWidth(Double.MAX_VALUE);
    a.cuerpo
        .getChildren()
        .addAll(
            Aplicacion.titulo(
                "Bloque " + (q.posicion / 2 + 1) + " de " + ((q.items.size() + 1) / 2)),
            progreso);
    for (int i = q.posicion; i < fin; i++) {
      int indice = i;
      var item = q.items.get(i);
      VBox box = new VBox(7, Aplicacion.titulo((i + 1) + ". " + item.texto()));
      ToggleGroup grupo = new ToggleGroup();
      for (var op : item.opciones()) {
        var b = new RadioButton(op.getTexto());
        b.setToggleGroup(grupo);
        b.setSelected(q.respuestas.get(i) == op);
        b.setOnAction(e -> q.respuestas.put(indice, op));
        box.getChildren().add(b);
      }
      a.cuerpo.getChildren().add(box);
    }
    a.pie(
        a.secundario("Cancelar", salir),
        a.accion(
            fin == q.items.size() ? "Finalizar" : "Siguiente",
            () -> {
              if (q.getEstado() != EstadoCuestionario.EN_CURSO) {
                a.informar("Tiempo agotado.");
                a.modos();
                return;
              }
              a.sesion.cuestionarios.guardarBloqueCuestionario(bloque);
              if (q.getEstado() == EstadoCuestionario.COMPLETADO) {
                a.informar("Cuestionario completado con éxito.");
                a.modos();
              } else bloque(q);
            }));
  }
}
