import java.util.*;
import java.util.function.*;
import javafx.application.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class Aplicacion extends Application {
  Stage ventana;
  Sesion sesion;
  final PreguntasUI preguntas = new PreguntasUI(this);

  final EvaluacionesUI evaluaciones = new EvaluacionesUI(this);
  final ReportesUI reportes = new ReportesUI(this);
  final BorderPane marco = new BorderPane();
  VBox cuerpo;
  Label error;
  static final String NOMBRE = "Capital Humano";

  public static void main(String[] args) {
    launch(args);
  }

  public void start(Stage stage) {
    ventana = stage;
    if(getParameters().getRaw().contains("--verificar")) Postgres.esquemaPrueba();
    try { sesion = new Sesion(Postgres.url()); }
    catch(RuntimeException ex){
      if(getParameters().getRaw().contains("--verificar"))throw ex;
      var alerta=new Alert(Alert.AlertType.ERROR,ex.getMessage(),ButtonType.OK);
      alerta.setHeaderText("No se pudo abrir la base de datos");alerta.showAndWait();Platform.exit();return;
    }
    Scene escena = new Scene(marco, 1030, 740);
    escena.getStylesheets().add(getClass().getResource("pantallas.css").toExternalForm());
    stage.setScene(escena);
    stage.setTitle(NOMBRE);
    stage.setMinWidth(680);
    stage.setMinHeight(540);
    modos();
    stage.show();
    if (getParameters().getRaw().contains("--verificar"))
      Platform.runLater(
          () -> {
            try {
              new Verificacion(this).ejecutar();
              System.out.println("Búsqueda, validación, cuestionario y reportes verificados.");
              Platform.exit();
            } catch (Throwable e) {
              e.printStackTrace();
              System.exit(1);
            }
          });
  }

  @Override
  public void stop() {
    if (sesion != null) sesion.close();
  }

  void pagina(String titulo, Runnable volver) {
    ventana.setTitle(titulo + " · " + NOMBRE);
    Label h = new Label(titulo);
    h.getStyleClass().add("titulo");
    VBox header = new VBox(6, h, new Label(NOMBRE + "   /   " + titulo));
    header.getStyleClass().add("header");
    marco.setTop(header);
    cuerpo = new VBox(16);
    cuerpo.setPadding(new Insets(20, 32, 24, 32));
    if (volver != null) cuerpo.getChildren().add(boton("‹  Volver", volver, "link"));
    error = new Label();
    error.setWrapText(true);
    error.getStyleClass().add("error");
    error.setManaged(false);
    error.setVisible(false);
    cuerpo.getChildren().add(error);
    ScrollPane scroll = new ScrollPane(cuerpo);
    scroll.setFitToWidth(true);
    marco.setCenter(scroll);
    marco.setBottom(null);
  }

  void modos() {
    pagina("Selección de modo", null);
    var elegir =
        fila(
            tarjeta("Modo Consultor", "Gestión de preguntas, evaluaciones y reportes.", this::menu),
            tarjeta(
                "Modo Candidato",
                "Accedé al cuestionario con tu clave de ingreso.",
                evaluaciones::login));
    elegir.setFillHeight(true);
    cuerpo.getChildren().add(elegir);
  }

  void menu() {
    pagina("Modo Consultor", this::modos);
    cuerpo
        .getChildren()
        .addAll(
            tarjeta(
                "Gestionar preguntas",
                "Buscá, creá o editá las preguntas del catálogo.",
                preguntas::buscar),
            tarjeta(
                "Evaluar candidatos",
                "Seleccioná un puesto y generá las claves de ingreso.",
                evaluaciones::seleccion),
            tarjeta(
                "Emitir orden de mérito",
                "Consultá e imprimí los resultados de una evaluación.",
                reportes::buscar));
  }

  Button tarjeta(String texto, String detalle, Runnable ruta) {
    Button boton = boton(texto, ruta, "tarjeta");
    Label nombre = new Label(texto);
    nombre.getStyleClass().add("nombre-tarjeta");
    Label descripcion = new Label(detalle);
    descripcion.setWrapText(true);
    descripcion.getStyleClass().add("detalle-tarjeta");
    var rotulos = new VBox(6, nombre, descripcion);
    HBox.setHgrow(rotulos, Priority.ALWAYS);
    var flecha = new Label("›");
    flecha.getStyleClass().add("flecha");
    var linea = fila(rotulos, flecha);
    linea.prefWidthProperty().bind(boton.widthProperty().subtract(56));
    boton.setGraphic(linea);
    boton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    boton.setAccessibleText(texto + ". " + detalle);
    boton.setMaxWidth(Double.MAX_VALUE);
    boton.setMinWidth(0);
    boton.setPrefWidth(400);
    HBox.setHgrow(boton, Priority.ALWAYS);
    return boton;
  }

  static Label titulo(String s) {
    Label l = new Label(s);
    l.getStyleClass().add("seccion");
    return l;
  }

  static HBox fila(Node... nodes) {
    HBox box = new HBox(12, nodes);
    box.setAlignment(Pos.CENTER_LEFT);
    return box;
  }

  static VBox campo(String nombre, Control c) {
    VBox box = new VBox(5, new Label(nombre), c);
    HBox.setHgrow(box, Priority.ALWAYS);
    c.setMaxWidth(Double.MAX_VALUE);
    return box;
  }

  static TextField texto(String pista) {
    TextField t = new TextField();
    t.setPromptText(pista);
    return t;
  }

  Button boton(String s, Runnable r, String estilo) {
    Button b = new Button(s);
    b.getStyleClass().add(estilo);
    b.setOnAction(
        e -> {
          try {
            error.setVisible(false);
            error.setManaged(false);
            r.run();
          } catch (IllegalArgumentException | IllegalStateException ex) {
            fallo(ex.getMessage());
          }
        });
    return b;
  }

  Button accion(String s, Runnable r) {
    return boton(s, r, "primario");
  }

  Button secundario(String s, Runnable r) {
    return boton(s, r, "secundario");
  }

  void pie(Button... botones) {
    HBox f = fila(botones);
    f.setAlignment(Pos.CENTER_RIGHT);
    f.getStyleClass().add("pie");
    marco.setBottom(f);
  }

  void fallo(String msg) {
    error.setText(msg);
    error.setManaged(true);
    error.setVisible(true);
    java.awt.Toolkit.getDefaultToolkit().beep();
    ((ScrollPane) marco.getCenter()).setVvalue(0);
  }

  boolean confirmar(String titulo, String texto, String si, String no) {
    ButtonType aceptar = new ButtonType(si, ButtonBar.ButtonData.YES);
    Alert dialog =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            texto,
            aceptar,
            new ButtonType(no, ButtonBar.ButtonData.CANCEL_CLOSE));
    dialog.initOwner(ventana);
    dialog.setTitle(titulo);
    dialog.setHeaderText(titulo);
    return dialog.showAndWait().orElse(ButtonType.CANCEL) == aceptar;
  }

  void informar(String msg) {
    Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
    a.initOwner(ventana);
    a.setHeaderText(null);
    a.setTitle(NOMBRE);
    a.showAndWait();
  }

  static <T> ComboBox<T> combo(Collection<T> datos, Function<T, String> nombre, String pista) {
    ComboBox<T> c = new ComboBox<>(FXCollections.observableArrayList(datos));
    c.setPromptText(pista);
    c.setConverter(
        new StringConverter<>() {
          public String toString(T x) {
            return x == null ? "" : nombre.apply(x);
          }

          public T fromString(String s) {
            return null;
          }
        });
    return c;
  }

  static <T> TableView<T> tabla(Collection<T> datos) {
    TableView<T> t = new TableView<>(FXCollections.observableArrayList(datos));
    t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    t.setPrefHeight(205);
    t.setPlaceholder(new Label("Sin resultados"));
    return t;
  }

  static <T> void col(TableView<T> t, String etiqueta, Function<T, ?> valor) {
    TableColumn<T, String> c = new TableColumn<>(etiqueta);
    c.setCellValueFactory(
        x -> new ReadOnlyStringWrapper(Objects.toString(valor.apply(x.getValue()), "")));
    t.getColumns().add(c);
  }

  static <T> T elegido(TableView<T> t, String aviso) {
    T x = t.getSelectionModel().getSelectedItem();
    if (x == null) throw new IllegalArgumentException(aviso);
    return x;
  }

  static boolean contiene(String dato, String filtro) {
    return dato.toUpperCase(Locale.ROOT).contains(filtro.trim().toUpperCase(Locale.ROOT));
  }
}
