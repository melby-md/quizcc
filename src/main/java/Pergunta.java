import java.util.ArrayList;

class Pergunta extends Enunciado {
	private ArrayList<Alternativa> alternativas = 
		new ArrayList<Alternativa>();

	public Pergunta(String enunciado) {
		super(enunciado);
	}

	public ArrayList<Alternativa> getAlternativas() {
		return this.alternativas;
	}

	public void addAlternativa(Alternativa a) {
		this.alternativas.add(a);
	}
}
