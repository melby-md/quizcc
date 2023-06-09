class Personagem {
	private String nome = null;
	private Alternativa resposta;
	private double vida = 1;
	private boolean status;

	public Personagem(boolean status) {
		this.status = status;
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

	public Alternativa getResposta() {
		return this.resposta;
	}

	public void setResposta(Alternativa resposta) {
		this.resposta = resposta;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public void toggleStatus() {
		this.status = !this.status;
	}

	public void dano() {
		this.vida -= 0.1;
	}
}
