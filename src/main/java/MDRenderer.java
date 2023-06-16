import java.util.ArrayList;
import java.util.regex.*;
import java.io.*;

import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

// Por algum motivo eu decidi criar de última hora um parser de markdown para
// renderizar as telas de créditos e como jogar, ele é péssimo e não suporta a
// especificação completa, não tente entender o código.
class MDRenderer extends ScrollPane {

	private static class Token {
		private final byte type;
		private String content;

		public Token(byte type, String content) {
			this.type = type;
			this.content = content;
		}
		
		public byte getType() {
			return this.type;
		}

		public String getContent() {
			return this.content;
		}

		public void append(String s) {
			this.content += s;
		}
	}

	private static final byte
	NIL = 0,
	TITULO = 1,
	SUBTITULO = 2,
	SUBSUBTITULO = 3,
	LITERAL = 4,
	LINK = 5,
	TEXTO = 6,
	NL = 7;

	private static final Pattern
	titulo = Pattern.compile("^#{1,3}[ \t]+"),
	subTitulo = Pattern.compile("^##[\s\t]+"),
	subSubTitulo = Pattern.compile("^###[\s\t]+"),
	literal = Pattern.compile("^    "),
	link = Pattern.compile("(.*)<(.*)>(.*)");

	public MDRenderer(String fileName) {
		super();
		setFitToWidth(true);
		ArrayList<Token> tokens = new ArrayList<>();
		tokens.add(new Token(NIL, ""));

		Matcher m;

		try (
			InputStream file = 
			DataBase.class.getResourceAsStream(fileName);
			BufferedReader br = 
			new BufferedReader(new InputStreamReader(file))
		) {
			int el = 0;
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				Token last = tokens.get(tokens.size()-1);

				if (line.trim().length() == 0) {
					++el;
				} else if (line.startsWith("    ")) {
					line = line.replace("    ", "") + '\n';

					if (last.getType() == TEXTO)
						last.append("\n\n");

					if (last.getType() == LITERAL)
						last.append("\n".repeat(el) + line);
					else
						tokens.add(new Token(LITERAL, line));
					el = 0;
				} else if ((m = titulo.matcher(line)).find()) {
					if (last.getType() == LITERAL)
						last.append("\n");
					else if (last.getType() == TEXTO)
						last.append("\n\n");

					el = 0;
					byte type;
					if (line.startsWith("##"))
						type = SUBTITULO;
					else if (line.startsWith("###"))
						type = SUBSUBTITULO;
					else
						type = TITULO;

					tokens.add(new Token(type, m.replaceFirst("").trim() + "\n"));
				} else {
					boolean nl = line.endsWith("  ");
					line = line
						.trim()
						.replaceAll(" +", " ")
						.replaceAll("\t+", "\t");

					while ((m = link.matcher(line)).find()) {
						line = m.group(1).trim();
						System.out.println("line: " + line + "<-");
						if (last.getType() == TEXTO && !line.equals(" "))
							last.append(' ' + line);
						else
							tokens.add(new Token(TEXTO, line));
						tokens.add((last = new Token(LINK, m.group(2))));
						line = m.group(3).trim();
					}

					if (nl || el != 0)
						line += "\n\n";	

					if (last.getType() == TEXTO)
						last.append(' ' + line);
					else
						tokens.add(new Token(TEXTO, line));
					el = 0;
				}

			}
		} catch (IOException e) {
			System.exit(1);	
		}

		TextFlow tf = new TextFlow();
		tf.setMaxWidth(800);

		Text t;
		for (Token token : tokens) {
			switch (token.getType()) {
			case TITULO:
				t = new Text(token.getContent());
				t.setFont(new Font(40));
				tf.getChildren().addAll(t, new Text("\n"));
				break;

			case SUBTITULO:
				t = new Text(token.getContent());
				t.setFont(new Font(30));
				tf.getChildren().addAll(t, new Text("\n"));
				break;

			case SUBSUBTITULO:
				t = new Text(token.getContent());
				t.setFont(new Font(20));
				tf.getChildren().addAll(t, new Text("\n"));
				break;

			case LITERAL:
				t = new Text(token.getContent());
				t.setStyle("-fx-font-family:monospaced");
				tf.getChildren().add(t);
				break;

			case LINK:
				Hyperlink l = new Hyperlink(token.getContent());
				tf.getChildren().add(l);
				break;

			case TEXTO:
				t = new Text(token.getContent());
				tf.getChildren().add(t);
			}
		}

		Button voltar = Jogo.botaoVoltar();

		BorderPane bp = new BorderPane();
		bp.setTop(voltar);
		bp.setCenter(tf);
		bp.setMargin(voltar, new Insets(20, 20, 0, 0));

		setContent(bp);
	}
}
