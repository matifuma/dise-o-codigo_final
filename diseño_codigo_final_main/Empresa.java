/** Empresa asociada a un puesto; el diagrama solo define el atributo nombre. */
public class Empresa {
    private final String nombre;

    public Empresa(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() { return nombre; }
}
