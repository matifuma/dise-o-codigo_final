import java.util.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Window;

/** Recorre los dos modos en una sesión independiente mediante iniciar.ps1 -Verificar. */
final class Verificacion {
  final Aplicacion a;

  Verificacion(Aplicacion a) {
    this.a = a;
  }

  void exigir(boolean ok, String detalle) {
    if (!ok) throw new AssertionError(detalle);
  }

  static List<Node> nodos(Node n) {
    var r = new ArrayList<Node>();
    r.add(n);
    if (n instanceof Parent p) for (Node hijo : p.getChildrenUnmodifiable()) r.addAll(nodos(hijo));
    return r;
  }

  <T> List<T> controles(Class<T> tipo) {
    return nodos(a.cuerpo).stream().filter(tipo::isInstance).map(tipo::cast).toList();
  }

  void pulsar(String nombre) {
    var nodes = nodos(a.cuerpo);
    if (a.marco.getBottom() != null) nodes.addAll(nodos(a.marco.getBottom()));
    var b =
        nodes.stream()
            .filter(Button.class::isInstance)
            .map(Button.class::cast)
            .filter(x -> nombre.equals(x.getText()))
            .findFirst()
            .orElseThrow();
    exigir(!b.isDisabled(), nombre);
    b.fire();
  }

  void cerrarDialogo() {
    Platform.runLater(
        () ->
            Window.getWindows().stream()
                .filter(w -> w != a.ventana && w.isShowing())
                .findFirst()
                .ifPresent(Window::hide));
  }

  void ejecutar() {
    pulsar("Modo Consultor");
    pulsar("Gestionar preguntas");
    controles(TextField.class).get(2).setText("Cooperación");
    pulsar("Buscar");
    exigir(controles(TableView.class).get(0).getItems().size() == 2, "CU020");
    pulsar("Nuevo");
    pulsar("Guardar");
    exigir(a.error.isVisible(), "Validación CU021");
    var combos = controles(ComboBox.class);
    for (var c : combos) c.getSelectionModel().select(0);
    controles(TextField.class).get(0).setText("Prueba de selección");
    controles(TextArea.class).get(0).setText("¿Cómo gestionarías el encuentro?");
    cerrarDialogo();
    pulsar("Guardar");
    var prueba = a.sesion.preguntas.buscarPregunta(null, null, "Prueba de selección");
    exigir(prueba.size() == 1, "Alta real");
    a.sesion.preguntas.darDeBajaPregunta(prueba.get(0));
    exigir(
        a.sesion.preguntas.buscarPregunta(null, null, "Prueba de selección").isEmpty(),
        "Baja lógica");
    a.menu();
    pulsar("Evaluar candidatos");
    pulsar("Buscar");
    var resultados = controles(TableView.class).get(0);
    exigir(resultados.getItems().size() == 4, "Búsqueda CU025");
    a.marco.applyCss();
    a.marco.layout();
    var casillas =
        resultados.lookupAll(".check-box").stream()
            .filter(CheckBox.class::isInstance)
            .map(CheckBox.class::cast)
            .filter(b -> b.getGraphic() == null && !b.isDisabled())
            .toList();
    var casilla = casillas.stream().filter(b -> b.isVisible()).findFirst().orElseThrow();
    casilla.fire();
    pulsar("Agregar");
    pulsar("Siguiente");
    controles(ComboBox.class).get(0).getSelectionModel().select(0);
    pulsar("Siguiente");
    var claves = controles(TableView.class).get(0).getItems();
    var dto = (CandidatoConClaveDTO) claves.get(0);
    cerrarDialogo();
    pulsar("Finalizar");
    var q = a.sesion.registro.porClave(dto.getClave());
    exigir(q.getEstado() == EstadoCuestionario.ACTIVO, "Guardado CU025");
    a.modos();
    pulsar("Modo Candidato");
    controles(TextField.class).get(0).setText(dto.getClave());
    pulsar("Ingresar");
    pulsar("Aceptar");
    pulsar("Siguiente");
    exigir(q.posicion == 0 && a.error.isVisible(), "Bloque incompleto");
    while (q.posicion < q.items.size()) {
      Set<ToggleGroup> grupos = new HashSet<>();
      for (var b : controles(RadioButton.class)) if (grupos.add(b.getToggleGroup())) b.fire();
      if (q.finBloque() == q.items.size()) {
        cerrarDialogo();
        pulsar("Finalizar");
      } else pulsar("Siguiente");
    }
    exigir(q.getEstado() == EstadoCuestionario.COMPLETADO && q.cumple(), "Resultado CU026");
    exigir(
        a.sesion.candidatos.validarCandidatos(List.<CandidatoDTO>of(dto)).isEmpty(),
        "El candidato completado queda libre");
    pulsar("Modo Consultor");
    pulsar("Emitir orden de mérito");
    controles(TextField.class).get(0).setText("PST-001");
    pulsar("Buscar");
    controles(TableView.class).get(0).getSelectionModel().select(0);
    pulsar("Emitir orden");
    pulsar("Aceptar");
    exigir(controles(TableView.class).get(0).getItems().contains(q), "Ranking CU028");
  }
}
