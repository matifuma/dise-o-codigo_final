/** DTO compatible con la pantalla de confirmación del CU025. */
public class CandidatoConClaveDTO extends CandidatoDTO {
    private final Candidato candidato;
    private final String clave;

    public CandidatoConClaveDTO(Candidato candidato, String clave) {
        super(candidato);
        if (clave == null || clave.length() != 8) {
            throw new IllegalArgumentException("La clave de ingreso debe tener 8 caracteres.");
        }
        this.candidato = candidato;
        this.clave = clave;
    }

    public Candidato getCandidato() { return candidato; }
    public String getClave() { return clave; }
}
