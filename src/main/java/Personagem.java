class Personagem {
	private String nome;
	private int vida = 100;
	private boolean status = false;

	public Personagem(String nome) {
		this.nome = nome;
	}

	public String getNome() {
		return this.nome;
	}

	public int getVida() {
		return this.vida;
	}

	public boolean getStatus() {
		return this.status;
	}

	public boolean subVida(int dano) {
		this.vida -= dano;
		return this.vida <= 0;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}
}
