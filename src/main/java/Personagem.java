class Personagem {
	private String nome;
	private Alternativa resposta;
	private double vida = 1;
	private boolean status;

	public Personagem(String nome) {
		this.nome = nome;
	}

	public String getNome() {
		return this.nome;
	}

	public double getVida() {
		return this.vida;
	}

	public boolean getStatus() {
		return this.status;
	}

	public void setResposta(Alternativa resposta) {
		this.resposta = resposta;
	}

	public Alternativa getResposta() {
		return this.resposta;
	}

	public void toggleStatus() {
		this.status = !this.status;
	}

	public void subVida(double dano) {
		this.vida -= dano;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}
}
