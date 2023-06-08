import java.util.ArrayDeque;
import java.sql.*;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.event.*;

public class Jogo extends Application {

	private static class MsgEvent extends Event {
		private String msg = null;
		public static final EventType<MsgEvent> ALTERA_MSG = new EventType<>("ALTERA_MSG");

		public MsgEvent () {
			super(ALTERA_MSG);
		}

		public MsgEvent (String msg) {
			super(ALTERA_MSG);
			this.msg = msg;
		}

		public String getMsg() {
			return this.msg;
		}
	}

	private static final EventType<Event> TELA_INICIAL = new EventType<>("TELA_INICIAL");

	private Personagem player1, player2;

	
	private BorderPane fim() {
		String s;
		if (player1.getVida() == player2.getVida())
			s = "EMPATE!";
		else if (player1.getVida() > player2.getVida())
			s = "VENCEDOR: " + player1.getNome();
		else
			s = "VENCEDOR: " + player2.getNome();
		player1 = null;
		player2 = null;

		Text txt = new Text(s);
		txt.setFont(new Font(50));

		Button voltar = new Button("Voltar");
		voltar.setOnAction(e -> voltar.fireEvent(new Event(TELA_INICIAL)));
		BorderPane.setMargin(voltar, new Insets(15));

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(txt);
		borderPane.setTop(voltar);

		return borderPane;
	}

	private void escolher(Button b, Text txt) {
		Personagem p = new Personagem(b.getText());
		if (player1 == null) {
			player1 = p;
			p.setStatus(true);
			b.setDisable(true);
			txt.setText("Escolha o segundo personagem");
		} else {
			player2 = p; 
			p.setStatus(false);
			b.getScene().setRoot(jogo());
		}
	}

	private void displayPerguntas(ArrayDeque<Pergunta> perguntas,
								  VBox vbox,
								  VBox botoes, 
								  Text enunciado,
								  ProgressBar vida1,
								  ProgressBar vida2)
	{
		if (perguntas.isEmpty()) {
			botoes.getScene().setRoot(fim());
			return;
		}

		Pergunta pergunta = perguntas.pop();

		enunciado.setText(pergunta.getEnunciado());

		Button next = new Button("Próxima pergunta");
		next.setOnAction(e -> {
			next.fireEvent(new MsgEvent());
			vbox.getChildren().remove(next);
			displayPerguntas(perguntas, vbox, botoes, enunciado, vida1, vida2);
		});

		Button sim = new Button("sim");
		Button nao = new Button("nao");
		HBox hbox = new HBox(20, sim, nao);
		VBox confirmacao = new VBox(20, new Text("Você tem certeza de suas respostas?"), hbox);
		hbox.setAlignment(Pos.CENTER);
		confirmacao.setAlignment(Pos.CENTER);
		sim.setOnAction(e -> {
			boolean status1 = player1.getResposta().getStatus();
			boolean status2 = player2.getResposta().getStatus();
			if (!status1) {
				player1.subVida(0.1);
				if (player1.getVida() <= 0)
					vbox.getScene().setRoot(fim());
			}
			if (!status2) {
				player2.subVida(0.1);
				if (player1.getVida() <= 0)
					vbox.getScene().setRoot(fim());
			}

			String msg;
			if (!status1 && !status2)
				msg = "Ambos os jogadores erraram!";
			else
				msg = "Jogador " + (status1 ? 1 : 2) + " acertou!";
			sim.fireEvent(new MsgEvent(msg));

			vida1.setProgress(player1.getVida());
			vida2.setProgress(player2.getVida());
			vbox.getChildren().remove(confirmacao);

			for (int i = 0; i < pergunta.getAlternativas().size(); ++i) {
				Node b = botoes.getChildren().get(i);
				b.setMouseTransparent(true);
				b.setFocusTraversable(false);
				Alternativa a = pergunta.getAlternativas().get(i);
				if (a.getStatus())
					b.setStyle("-fx-text-fill: #00ff00;");
				else
					b.setStyle("-fx-text-fill: #ff0000;");
			}
			vbox.getChildren().add(next);
		});

		nao.setOnAction(e -> {
			nao.fireEvent(new MsgEvent());
			for (Node b : botoes.getChildren())
				b.setStyle(null);
			vbox.getChildren().remove(confirmacao);
		});

		byte letra = 0;
		botoes.getChildren().clear();
		for (Alternativa a : pergunta.getAlternativas()) {
			Button btn = new Button((char)('a' + letra) + ") " + a.getEnunciado());
			btn.setMinWidth(500);
			btn.setOnAction(e -> {
				Personagem p = player1.getStatus() ? player1 : player2;
				p.setResposta(a);
				btn.setStyle("-fx-text-fill: #0000ff;");

				player1.toggleStatus();
				player2.toggleStatus();

				if (p == player2) {
					vbox.getChildren().add(confirmacao);
				} else {
					btn.fireEvent(new MsgEvent());
				}
			});
			letra = (byte)((letra + 1) % 26);
			botoes.getChildren().add(btn);
		}
	}

	private BorderPane jogo() {
		ProgressBar vida1 = new ProgressBar();
		ProgressBar vida2 = new ProgressBar();
		HBox.setHgrow(vida1, Priority.ALWAYS);
		HBox.setHgrow(vida2, Priority.ALWAYS);
		vida1.setProgress(1);
		vida2.setProgress(1);

		Text rodada = new Text("Vez do jogador 1");

		HBox hbox = new HBox(
			5,
			new Text(player1.getNome()),
			vida1,
			rodada,
			vida2,
			new Text(player2.getNome())
		);
		hbox.setAlignment(Pos.TOP_CENTER);
		hbox.setPadding(new Insets(15));

		ReadOnlyDoubleProperty tamanho = hbox.widthProperty();
		vida1.prefWidthProperty().bind(tamanho);
		vida2.prefWidthProperty().bind(tamanho);

		Text enunciado = new Text();
		enunciado.setFont(new Font(30));

		VBox botoes = new VBox(5);
		botoes.setAlignment(Pos.CENTER);

		VBox vbox = new VBox(20, enunciado, botoes);
		vbox.setAlignment(Pos.CENTER);

		ArrayDeque<Pergunta> perguntas = new ArrayDeque<Pergunta>();	

		try (
			Connection con = DataBase.getCon();
			Statement stmt = con.createStatement();
			ResultSet prs = stmt.executeQuery("""
				SELECT * FROM perguntas	ORDER BY RANDOM()
			""");
			PreparedStatement ps = con.prepareStatement("""
				SELECT enunciado, status
				FROM alternativas
				WHERE pergunta_id = ?
				ORDER BY RANDOM()
			""");
		) {
			while (prs.next()) {
				Pergunta pergunta = new Pergunta(prs.getString("enunciado"));
				perguntas.add(pergunta);

				ps.setInt(1, prs.getInt("id"));
				try (
					ResultSet ars = ps.executeQuery();
				) {
					while (ars.next()) {
						Alternativa alternativa = new Alternativa(
							ars.getString("enunciado"),
							ars.getBoolean("status")
						);
						pergunta.addAlternativa(alternativa);
					}
				}
			}
		} catch (SQLException e) {
			System.err.println("Erro SQL: " + e.getMessage());
			System.exit(1);
		}

		displayPerguntas(perguntas, vbox, botoes, enunciado, vida1, vida2);

		BorderPane borderPane = new BorderPane();
		borderPane.setTop(hbox);
		borderPane.setCenter(vbox);

		borderPane.addEventHandler(MsgEvent.ALTERA_MSG, e -> {
			String msg = e.getMsg();
			if (msg == null) {
				msg = "Vez do jogador " + (player1.getStatus() ? 1 : 2);
			}
			rodada.setText(msg);
		});

		return borderPane;
	}

	private BorderPane telaDeSelecao() {
		Button personagem1 = new Button("Evandro");
		Button personagem2 = new Button("Jorge");
		Button personagem3 = new Button("Agnaldo");

		Text titulo = new Text("Escolha o primeiro personagem");
		titulo.setFont(new Font(25));

		personagem1.setOnAction(e -> escolher(personagem1, titulo));
		personagem2.setOnAction(e -> escolher(personagem2, titulo));
		personagem3.setOnAction(e -> escolher(personagem3, titulo));

		HBox hbox = new HBox(
			10,
			personagem1,
			personagem2,
			personagem3
		);
		hbox.setAlignment(Pos.CENTER);

		VBox vbox = new VBox(
			10,
			titulo,
			hbox
		);
		vbox.setAlignment(Pos.CENTER);

		Button voltar = new Button("Voltar");
		voltar.setCancelButton(true);
		voltar.setOnAction(e -> voltar.fireEvent(new Event(TELA_INICIAL)));
		BorderPane.setMargin(voltar, new Insets(15));

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(vbox);
		borderPane.setTop(voltar);

		return borderPane;
	}

	// Esse método tem que ser público
	@Override
	public void start(Stage stage) throws SQLException {
		DataBase.connect();

		stage.setTitle("quizcc");
		stage.setMaximized(true);

		Text titulo = new Text("Quiz CC");
		titulo.setFont(new Font(50));
		titulo.setFill(Color.RED);

		Text subtitulo = new Text("(CC significa Ciência da Computação)");
		subtitulo.setFont(new Font(30));

		Button novoJogo = new Button("Novo Jogo");
		Button comoJogar = new Button("Como Jogar");
		Button creditos = new Button("Créditos");
		Button sair = new Button("Sair");

		VBox telaPrincipal = new VBox(
			15,
			titulo,
			subtitulo,
			novoJogo,
			comoJogar,
			creditos,
			sair
		);
		telaPrincipal.setAlignment(Pos.CENTER);

		for (Node n : telaPrincipal.getChildren())
			if (n instanceof Button) 
				((Button)n).setMinWidth(150);

		Scene scene = new Scene(telaPrincipal);
		scene.getStylesheets().add("style.css");
		scene.addEventHandler(TELA_INICIAL, e -> scene.setRoot(telaPrincipal));

		novoJogo.setOnAction(e ->
			scene.setRoot(telaDeSelecao())
		);

		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
			launch(args);
	}
}
