import java.util.*;
import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class PreguntasUI {
  final Aplicacion a;

  PreguntasUI(Aplicacion a) {
    this.a = a;
  }

  void buscar() {
    buscar("", "", "", false);
  }

  void buscar(String c, String f, String n, boolean ejecutar) {
    a.pagina("Gestionar preguntas", a::menu);
    var comp = Aplicacion.texto("Escribí una competencia");
    comp.setText(c);
    var factor = Aplicacion.texto("Escribí un factor");
    factor.setText(f);
    var nombre = Aplicacion.texto("Escribí una pregunta");
    nombre.setText(n);
    TableView<Pregunta> t = Aplicacion.tabla(List.of());
    Aplicacion.col(t, "Competencia", p -> p.getCompetencia().getNombre());
    Aplicacion.col(t, "Factor", p -> p.getFactor().getNombre());
    Aplicacion.col(t, "Pregunta", Pregunta::getNombre);
    Runnable filtrar =
        () -> {
          if ((comp.getText() + factor.getText() + nombre.getText()).isBlank())
            throw new IllegalArgumentException("Ingresá al menos un criterio de búsqueda.");
          List<Pregunta> hits = new ArrayList<>();
          for (Factor item : a.sesion.factoresDisponibles())
            if (Aplicacion.contiene(item.getNombre(), factor.getText())
                && Aplicacion.contiene(item.getCompetencia().getNombre(), comp.getText()))
              hits.addAll(
                  a.sesion.preguntas.buscarPregunta(item.getCompetencia(), item, nombre.getText()));
          t.getItems().setAll(hits);
        };
    Runnable volver =
        () ->
            buscar(
                comp.getText(),
                factor.getText(),
                nombre.getText(),
                !(comp.getText() + factor.getText() + nombre.getText()).isBlank());
    Button modificar =
        a.secundario("Modificar", () -> editar(t.getSelectionModel().getSelectedItem(), volver));
    Button eliminar =
        a.secundario(
            "Eliminar",
            () -> {
              var p = t.getSelectionModel().getSelectedItem();
              if (a.confirmar(
                  "Eliminar pregunta",
                  "¿Está seguro que desea eliminar la pregunta "
                      + p.getNombre()
                      + "? Esta acción es definitiva y no se puede deshacer.",
                  "Eliminar",
                  "Cancelar")) {
                a.sesion.preguntas.darDeBajaPregunta(p);
                filtrar.run();
                a.informar("Pregunta eliminada con éxito.");
              }
            });
    modificar.disableProperty().bind(t.getSelectionModel().selectedItemProperty().isNull());
    eliminar.disableProperty().bind(modificar.disableProperty());
    a.cuerpo
        .getChildren()
        .addAll(
            Aplicacion.fila(
                Aplicacion.campo("Competencia", comp),
                Aplicacion.campo("Factor", factor),
                Aplicacion.campo("Pregunta", nombre)),
            Aplicacion.fila(
                a.accion("Buscar", filtrar), a.accion("Nuevo", () -> editar(null, volver))),
            Aplicacion.titulo("Resultados"),
            t);
    a.pie(modificar, eliminar);
    if (ejecutar) filtrar.run();
  }

  void editar(Pregunta p) {
    editar(p, this::buscar);
  }

  private VBox campoContado(String etiqueta, TextInputControl entrada, int maximo) {
    VBox box = Aplicacion.campo(etiqueta, entrada);
    Label cuenta = new Label();
    cuenta.textProperty().bind(entrada.lengthProperty().asString("%d / " + maximo));
    cuenta.getStyleClass().add("contador");
    box.getChildren().add(cuenta);
    return box;
  }

  void editar(Pregunta p, Runnable volver) {
    a.pagina(p == null ? "Dar de alta pregunta" : "Modificar pregunta", volver);
    var competencia =
        Aplicacion.combo(
            a.sesion.catalogo.buscarCompetencia(""),
            Competencia::getNombre,
            "Seleccione una competencia");
    var factor = Aplicacion.combo(List.<Factor>of(), Factor::getNombre, "Seleccione un factor");
    competencia
        .valueProperty()
        .addListener(
            (observable, anterior, nuevo) -> {
              factor
                  .getItems()
                  .setAll(
                      competencia.getValue() == null
                          ? List.of()
                          : a.sesion.gestorFactores.buscarFactor(competencia.getValue()));
              factor.setValue(null);
            });
    var nombre = Aplicacion.texto("Nombre de la pregunta");
    var enunciado = new TextArea();
    enunciado.setPrefRowCount(2);
    enunciado.setWrapText(true);
    var desc = new TextArea();
    desc.setPrefRowCount(2);
    desc.setWrapText(true);
    var tipo =
        Aplicacion.combo(
            List.of(TipoPregunta.values()),
            x -> x.name().replace('_', ' '),
            "Seleccione una respuesta");
    VBox opciones = new VBox(6);
    List<OpcionRespuesta> originales = new ArrayList<>();
    List<TextField> pesos = new ArrayList<>();
    java.util.function.Consumer<List<OpcionRespuesta>> pintar =
        lista -> {
          originales.clear();
          originales.addAll(lista);
          pesos.clear();
          opciones.getChildren().clear();
          opciones
              .getChildren()
              .add(
                  Aplicacion.titulo(
                      "Orden     Descripción                                        Ponderación"));
          for (var o : lista) {
            var l = new Label(o.getOrden() + "       " + o.getTexto());
            l.setMinWidth(310);
            var t = Aplicacion.texto("0");
            t.setPrefWidth(85);
            t.setText(String.valueOf(o.getPuntaje()));
            pesos.add(t);
            opciones.getChildren().add(Aplicacion.fila(l, t));
          }
        };
    tipo.valueProperty()
        .addListener(
            (observable, anterior, nuevo) -> {
              if (tipo.getValue() != null) pintar.accept(a.sesion.opciones(tipo.getValue()));
            });
    if (p != null) {
      competencia.setValue(p.getCompetencia());
      factor.getItems().setAll(p.getFactor());
      factor.setValue(p.getFactor());
      competencia.setDisable(true);
      factor.setDisable(true);
      nombre.setText(p.getNombre());
      enunciado.setText(p.getEnunciado());
      desc.setText(p.getDescripcion());
      tipo.setValue(p.getTipo());
      pintar.accept(p.getOpciones());
    }
    a.cuerpo
        .getChildren()
        .addAll(
            Aplicacion.fila(
                Aplicacion.campo("Competencia *", competencia),
                Aplicacion.campo("Factor *", factor),
                campoContado("Nombre *", nombre, 50)),
            Aplicacion.fila(
                campoContado("Pregunta *", enunciado, 250), campoContado("Descripción", desc, 500)),
            Aplicacion.campo("Respuesta *", tipo),
            opciones);
    Button guardar =
        a.accion(
            "Guardar",
            () -> {
              List<String> faltas = new ArrayList<>();
              if (competencia.getValue() == null) faltas.add("Competencia");
              if (factor.getValue() == null) faltas.add("Factor");
              if (nombre.getText().isBlank()) faltas.add("Nombre");
              if (enunciado.getText().isBlank()) faltas.add("Pregunta");
              if (tipo.getValue() == null) faltas.add("Respuesta");
              if (!faltas.isEmpty())
                throw new IllegalArgumentException(
                    "Completá los campos: " + String.join(", ", faltas));
              List<String> largos = new ArrayList<>();
              if (nombre.getLength() > 50) largos.add("Nombre (50)");
              if (enunciado.getLength() > 250) largos.add("Pregunta (250)");
              if (desc.getLength() > 500) largos.add("Descripción (500)");
              if (!largos.isEmpty())
                throw new IllegalArgumentException(
                    "Longitud excedida: " + String.join(", ", largos));
              List<OpcionRespuesta> seleccion = new ArrayList<>();
              for (int i = 0; i < pesos.size(); i++) {
                var o = originales.get(i);
                double peso;
                try {
                  peso = Double.parseDouble(pesos.get(i).getText().replace(',', '.'));
                } catch (NumberFormatException ex) {
                  throw new IllegalArgumentException(
                      "Ingresá una ponderación numérica por opción.");
                }
                if (!Double.isFinite(peso))
                  throw new IllegalArgumentException("Ponderación inválida.");
                seleccion.add(
                    new OpcionRespuesta(o.getId(), o.getTexto(), peso, o.getOrden(), true));
              }
              try {
                if (p == null) {
                  a.sesion.preguntas.darDeAltaPregunta(
                      competencia.getValue(),
                      factor.getValue(),
                      nombre.getText(),
                      enunciado.getText(),
                      desc.getText(),
                      seleccion,
                      tipo.getValue());
                  if (a.confirmar(
                      "Pregunta guardada",
                      "La pregunta "
                          + nombre.getText()
                          + " se creó correctamente. ¿Desea cargar otra?",
                      "Sí",
                      "No")) editar(null, volver);
                  else volver.run();
                } else {
                  a.sesion.preguntas.modificarPregunta(
                      p,
                      tipo.getValue(),
                      nombre.getText(),
                      enunciado.getText(),
                      desc.getText(),
                      seleccion);
                  a.informar("Pregunta modificada con éxito.");
                  volver.run();
                }
              } catch (IllegalArgumentException ex) {
                if (!ex.getMessage().startsWith("Ya existe")) throw ex;
                if (a.confirmar(
                    "Pregunta duplicada", ex.getMessage(), "Seguir editando", "Salir sin guardar"))
                  nombre.requestFocus();
                else volver.run();
              }
            });
    Button cancelar =
        a.secundario(
            "Cancelar",
            () -> {
              if (a.confirmar(
                  "Cancelar edición",
                  "¿Desea salir sin guardar los cambios?",
                  "Salir sin guardar",
                  "Seguir editando")) volver.run();
            });
    if (p == null)
      a.pie(
          cancelar,
          a.secundario(
              "Generar con IA",
              () ->
                  generar(
                      competencia.getValue(), factor.getValue(), desc.getText(), tipo.getValue())),
          guardar);
    else a.pie(cancelar, guardar);
  }

  private void generar(
      Competencia actual, Factor seleccionado, String descripcion, TipoPregunta respuesta) {
    Dialog<Void> modal = new Dialog<>();
    modal.initOwner(a.ventana);
    modal.setTitle("Generar con IA");
    var c =
        Aplicacion.combo(
            a.sesion.catalogo.buscarCompetencia(""),
            Competencia::getNombre,
            "Seleccione una competencia");
    var f = Aplicacion.combo(List.<Factor>of(), Factor::getNombre, "Seleccione un factor");
    c.valueProperty()
        .addListener(
            (ob, anterior, nuevo) -> {
              f.setValue(null);
              f.getItems()
                  .setAll(nuevo == null ? List.of() : a.sesion.gestorFactores.buscarFactor(nuevo));
            });
    c.setValue(actual);
    f.setValue(seleccionado);
    var detalle = new TextArea(descripcion);
    detalle.setPrefRowCount(2);
    var opciones =
        Aplicacion.combo(
            List.of(TipoPregunta.values()),
            t -> t.name().replace('_', ' '),
            "Seleccione una respuesta");
    opciones.setValue(respuesta);
    Label aviso = new Label();
    aviso.setWrapText(true);
    var contenido =
        new VBox(
            12,
            Aplicacion.fila(Aplicacion.campo("Competencia *", c), Aplicacion.campo("Factor *", f)),
            Aplicacion.campo("Descripción *", detalle),
            Aplicacion.campo("Respuesta *", opciones),
            aviso);
    modal.getDialogPane().setContent(contenido);
    var generar = new ButtonType("Generar pregunta", ButtonBar.ButtonData.OK_DONE);
    modal
        .getDialogPane()
        .getButtonTypes()
        .addAll(new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE), generar);
    modal
        .getDialogPane()
        .lookupButton(generar)
        .addEventFilter(
            javafx.event.ActionEvent.ACTION,
            event -> {
              event.consume();
              List<String> faltan = new ArrayList<>();
              if (c.getValue() == null) faltan.add("Competencia");
              if (f.getValue() == null) faltan.add("Factor");
              if (detalle.getText().isBlank()) faltan.add("Descripción");
              if (opciones.getValue() == null) faltan.add("Respuesta");
              if (!faltan.isEmpty()) {
                aviso.setText("Completá: " + String.join(", ", faltan));
                return;
              }
              try {
                a.sesion.preguntas.generarPreguntaIA(c.getValue(), f.getValue());
              } catch (UnsupportedOperationException ex) {
                aviso.setText(
                    "El servicio de IA está pendiente de conexión. Podés cancelar y completar la"
                        + " pregunta manualmente.");
              }
            });
    modal.showAndWait();
  }
}
