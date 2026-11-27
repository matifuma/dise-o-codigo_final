/** Completa los mensajes de acceso y bloques de CU026 sobre los mismos registros de CU025. */
final class GestorCuestionariosSesion extends GestorCuestionarios {
  private final Sesion.RegistroCuestionarios registro;

  GestorCuestionariosSesion(
      CandidatoDAO candidatos, Sesion.RegistroCuestionarios registro, EvaluacionDAO evaluaciones) {
    super(candidatos, registro, evaluaciones, new ExportadorExcel());
    this.registro = registro;
  }

  @Override
  public Progreso iniciarCuestionario(String tipoDoc, String nroDoc, String clave) {
    var q = registro.porClave(clave);
    if (!q.getCandidato().getTipoDocumento().equals(tipoDoc)
        || !q.getCandidato().getNroDocumento().equals(nroDoc))
      throw new IllegalArgumentException("La clave no corresponde al candidato.");
    q.iniciar();
    return q;
  }

  static final class Bloque extends BloqueCuestionario {
    final Progreso cuestionario;
    final int primero, fin;

    Bloque(Progreso q) {
      super(null, 1 + q.posicion / q.getCantidadPreguntasPorBloque(), false, null);
      cuestionario = q;
      primero = q.posicion;
      fin = q.finBloque();
    }
  }

  @Override
  public Bloque obtenerBloquePendiente(Cuestionario cuestionario) {
    if (cuestionario.getEstado() != EstadoCuestionario.EN_CURSO)
      throw new IllegalStateException("Este cuestionario ya no está en curso.");
    return new Bloque((Progreso) cuestionario);
  }

  @Override
  public void guardarBloqueCuestionario(BloqueCuestionario actual) {
    if (!(actual instanceof Bloque b) || b.primero != b.cuestionario.posicion)
      throw new IllegalStateException("El bloque ya fue confirmado.");
    b.cuestionario.confirmarBloque();
  }
}
