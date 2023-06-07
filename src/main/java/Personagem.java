class Personagem {
	private String nome;
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

	public void toggleStatus() {
		this.status = !this.status;
	}

	public boolean subVida(double dano) {
		this.vida -= dano;
		return this.vida <= 0;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}
}
