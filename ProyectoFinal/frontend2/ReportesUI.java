import java.text.SimpleDateFormat;
import java.util.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class ReportesUI {
  final Aplicacion a;

  ReportesUI(Aplicacion a) {
    this.a = a;
  }

  void buscar() {
    buscar("", "", "", false);
  }

  void buscar(String codigo, String nombre, String empresa, boolean conservar) {
    a.pagina("Emitir orden de mérito", a::menu);
    var cod = Aplicacion.texto("Ej. PST-001");
    cod.setText(codigo);
    var nom = Aplicacion.texto("Nombre del puesto");
    nom.setText(nombre);
    List<String> empresas = new ArrayList<>(List.of(""));
    a.sesion.puestos.buscarPuestos("", "").stream()
        .map(p -> p.getEmpresa().getNombre())
        .distinct()
        .forEach(empresas::add);
    var emp =
        Aplicacion.combo(
            empresas, x -> x.isEmpty() ? "Todas las empresas" : x, "Seleccione una empresa");
    emp.setValue(empresa);
    TableView<Puesto> tabla = Aplicacion.tabla(List.of());
    Aplicacion.col(tabla, "Código", Puesto::getCodigo);
    Aplicacion.col(tabla, "Nombre del puesto", Puesto::getNombre);
    Aplicacion.col(tabla, "Empresa", p -> p.getEmpresa().getNombre());
    Aplicacion.col(
        tabla,
        "Candidatos",
        p ->
            participantes(p).stream()
                .map(q -> q.getCandidato().getNroCandidato())
                .distinct()
                .count());
    Aplicacion.col(
        tabla,
        "Evaluaciones completas",
        p -> {
          var qs = participantes(p);
          return qs.stream().filter(q -> q.getEstado() == EstadoCuestionario.COMPLETADO).count()
              + " / "
              + qs.size();
        });
    Runnable filtrar =
        () -> {
          if ((cod.getText() + nom.getText() + emp.getValue()).isBlank())
            throw new IllegalArgumentException("Ingresá al menos un criterio de búsqueda.");
          tabla
              .getItems()
              .setAll(
                  a.sesion.puestos.buscarPuestos(nom.getText(), emp.getValue()).stream()
                      .filter(p -> Aplicacion.contiene(p.getCodigo(), cod.getText()))
                      .toList());
        };
    a.cuerpo
        .getChildren()
        .addAll(
            Aplicacion.titulo("Buscar puesto"),
            Aplicacion.fila(Aplicacion.campo("Código", cod), Aplicacion.campo("Empresa", emp)),
            Aplicacion.campo("Nombre del puesto", nom),
            a.accion("Buscar", filtrar),
            Aplicacion.titulo("Resultados de la búsqueda"),
            tabla);
    a.pie(
        a.secundario("Cancelar", a::menu),
        a.accion(
            "Emitir orden",
            () -> {
              Puesto p = Aplicacion.elegido(tabla, "Seleccioná un puesto.");
              var lista = a.sesion.evaluaciones.obtenerEvaluacionesPorPuesto(p.getCodigo());
              if (lista.isEmpty()) {
                a.informar("El puesto seleccionado no posee evaluaciones.");
                return;
              }
              elegir(p, lista, () -> buscar(cod.getText(), nom.getText(), emp.getValue(), true));
            }));
    if (conservar) filtrar.run();
  }

  private List<Cuestionario> participantes(Puesto p) {
    return a.sesion.evaluaciones.obtenerEvaluacionesPorPuesto(p.getCodigo()).stream()
        .flatMap(e -> e.obtenerCuestionarios().stream())
        .toList();
  }

  static String fecha(Date d) {
    return d == null ? "—" : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(d);
  }

  void elegir(Puesto p, List<Evaluacion> lista, Runnable volver) {
    a.pagina("Seleccionar evaluaciones", volver);
    var todas = new CheckBox("TODAS");
    todas.setSelected(true);
    var vista = new ListView<Evaluacion>(FXCollections.observableArrayList(lista));
    vista.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    vista.setCellFactory(
        v ->
            new ListCell<>() {
              protected void updateItem(Evaluacion e, boolean vacia) {
                super.updateItem(e, vacia);
                setText(vacia ? null : "Evaluación " + e.getId() + " · " + fecha(e.getFecha()));
              }
            });
    vista.disableProperty().bind(todas.selectedProperty());
    vista.setPrefHeight(170);
    a.cuerpo
        .getChildren()
        .addAll(
            Aplicacion.titulo(p.getNombre() + " · " + p.getEmpresa().getNombre()), todas, vista);
    a.pie(
        a.secundario("Cancelar", volver),
        a.accion(
            "Aceptar",
            () -> {
              var elegidas =
                  todas.isSelected()
                      ? lista
                      : List.copyOf(vista.getSelectionModel().getSelectedItems());
              if (elegidas.isEmpty())
                throw new IllegalArgumentException("Seleccioná una o más evaluaciones.");
              mostrar(p, elegidas, () -> elegir(p, lista, volver));
            }));
  }

  void mostrar(Puesto p, List<Evaluacion> evaluaciones, Runnable volver) {
    a.pagina("Orden de mérito", volver);
    String cabecera =
        p.getNombre() + " · " + p.getEmpresa().getNombre() + " · " + fecha(new Date());
    a.cuerpo.getChildren().add(Aplicacion.titulo(cabecera));
    var lineas = new ArrayList<String>();
    lineas.add(cabecera);
    a.sesion
        .reportes
        .emitirOrdenMerito(p, evaluaciones.stream().map(Evaluacion::getId).toList())
        .forEach(
            (grupo, filas) -> {
              var t = Aplicacion.tabla(filas);
              t.setPrefHeight(Math.min(240, Math.max(78, 34 + filas.size() * 30)));
              Aplicacion.col(t, "Tipo", q -> q.getCandidato().getTipoDocumento());
              Aplicacion.col(t, "Documento", q -> q.getCandidato().getNroDocumento());
              Aplicacion.col(t, "Apellido", q -> q.getCandidato().getApellido());
              Aplicacion.col(t, "Nombre", q -> q.getCandidato().getNombre());
              Aplicacion.col(t, "Puntaje", this::puntaje);
              Aplicacion.col(t, "Inicio", q -> fecha(q.getFechaInicio()));
              Aplicacion.col(t, "Fin / último ingreso", q -> fecha(fin(q)));
              Aplicacion.col(t, "Accesos", Progreso::getCantidadAccesos);
              a.cuerpo
                  .getChildren()
                  .addAll(Aplicacion.titulo(grupo + " (" + filas.size() + ")"), t);
              lineas.add(grupo);
              for (var q : filas) {
                var c = q.getCandidato();
                lineas.add(
                    c.getTipoDocumento()
                        + " "
                        + c.getNroDocumento()
                        + " · "
                        + c.getApellido()
                        + ", "
                        + c.getNombre()
                        + " · Puntaje "
                        + puntaje(q)
                        + " · Inicio "
                        + fecha(q.getFechaInicio())
                        + " · Fin / último ingreso "
                        + fecha(fin(q))
                        + " · Accesos "
                        + q.getCantidadAccesos());
              }
            });
    a.pie(
        a.secundario("Volver al menú", a::menu),
        a.accion("Imprimir reporte", () -> imprimir(lineas)));
  }

  private Date fin(Progreso q) {
    return q.getFechaFin() != null ? q.getFechaFin() : q.ultimoIngreso();
  }

  private String puntaje(Progreso q) {
    return q.getEstado() == EstadoCuestionario.COMPLETADO
        ? String.format(Locale.forLanguageTag("es-AR"), "%.1f", q.total())
        : "—";
  }

  private void imprimir(List<String> filas) {
    var job = PrinterJob.createPrinterJob();
    if (job == null) {
      a.fallo("No hay impresoras disponibles.");
      return;
    }
    if (!job.showPrintDialog(a.ventana)) return;
    var layout = job.getJobSettings().getPageLayout();
    for (int i = 0; i < filas.size(); ) {
      VBox hoja = new VBox(8, new Label("Orden de mérito"));
      hoja.setPadding(new Insets(8));
      hoja.setPrefWidth(layout.getPrintableWidth());
      do {
        Label l = new Label(filas.get(i));
        l.setWrapText(true);
        hoja.getChildren().add(l);
        hoja.applyCss();
        if (hoja.prefHeight(layout.getPrintableWidth()) > layout.getPrintableHeight()
            && hoja.getChildren().size() > 2) {
          hoja.getChildren().remove(l);
          break;
        }
        i++;
      } while (i < filas.size());
      hoja.resize(layout.getPrintableWidth(), hoja.prefHeight(layout.getPrintableWidth()));
      hoja.layout();
      if (!job.printPage(hoja)) {
        job.cancelJob();
        a.fallo("No se pudo imprimir el reporte.");
        return;
      }
    }
    if (!job.endJob()) a.fallo("No se pudo finalizar la impresión.");
  }
}
