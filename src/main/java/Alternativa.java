class Alternativa extends Enunciado {
	private final boolean status;

	public Alternativa(String enunciado, boolean status) {
		super(enunciado);
		this.status = status;
	}

	public boolean getStatus() {
		return this.status;
	}
}
