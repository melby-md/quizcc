import java.util.ArrayDeque;
import java.sql.*;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
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
	private static final EventType<Event> FIM = new EventType<>("FIM");

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
			vbox.fireEvent(new Event(FIM));
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
					vbox.fireEvent(new Event(FIM));
			}
			if (!status2) {
				player2.subVida(0.1);
				if (player1.getVida() <= 0)
					vbox.fireEvent(new Event(FIM));
			}

			String msg;
			if (!status1 && !status2)
				msg = "Ambos os jogadores erraram!";
			else if (status1 && status2)
				msg = "Ambos os jogadores acertaram!";
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

		vida1.setProgress(1);
		vida2.setProgress(1);

		GridPane.setHgrow(vida1, Priority.ALWAYS);
		GridPane.setHgrow(vida2, Priority.ALWAYS);

		GridPane.setHalignment(vida1, HPos.CENTER);
		GridPane.setHalignment(vida2, HPos.CENTER);

		ImageView img1 = new ImageView(player1.getNome().toLowerCase() + ".jpg");
		ImageView img2 = new ImageView(player2.getNome().toLowerCase() + ".jpg");

		img1.setFitHeight(80);
		img1.setFitWidth(80);

		img2.setFitHeight(80);
		img2.setFitWidth(80);

		GridPane.setMargin(img1, new Insets(0, 10, 10, 10));
		GridPane.setMargin(img2, new Insets(0, 10, 10, 10));

		Text nome1 = new Text(player1.getNome());
		Text nome2 = new Text(player2.getNome());

		GridPane.setHalignment(nome1, HPos.LEFT);
		GridPane.setHalignment(nome2, HPos.RIGHT);

		Text rodada = new Text("Vez do jogador 1");
		GridPane.setHalignment(rodada, HPos.CENTER);

		GridPane topo = new GridPane();
		BorderPane.setMargin(topo, new Insets(10, 0, 0, 0));

		topo.add(img1, 0, 0, 1, 2);
		topo.add(img2, 3, 0, 1, 2);

		topo.add(nome1, 1, 0);
		topo.add(nome2, 2, 0);

		topo.add(vida1, 1, 1);
		topo.add(vida2, 2, 1);

		topo.add(rodada, 0, 2, 4, 1);

		ReadOnlyDoubleProperty tamanho = topo.widthProperty();
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
		borderPane.setTop(topo);
		borderPane.setCenter(vbox);

		borderPane.addEventHandler(MsgEvent.ALTERA_MSG, e -> {
			String msg = e.getMsg();
			if (msg == null) {
				msg = "Vez do jogador " + (player1.getStatus() ? 1 : 2);
			}
			rodada.setText(msg);
		});

		borderPane.addEventHandler(FIM, e -> {
			borderPane.fireEvent(new MsgEvent("Fim de jogo!"));
			borderPane.setCenter(fim());
		});

		return borderPane;
	}

	private Button botaoPersonagem(String nome) {
		ImageView img = new ImageView(nome.toLowerCase() + ".jpg");
		Button b = new Button(nome, img);
		b.setContentDisplay(ContentDisplay.TOP);
		return b;
	}

	private BorderPane telaDeSelecao() {
		Button personagem1 = botaoPersonagem("Evandro");
		Button personagem2 = botaoPersonagem("Jorge");
		Button personagem3 = botaoPersonagem("Agnaldo");

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
		stage.setTitle("quizcc");
		stage.setMaximized(true);

		Text titulo = new Text("Quiz CC");
		titulo.setFont(new Font(50));
		titulo.setFill(Color.RED);

		Text subTitulo = new Text("(CC significa Ciência da Computação)");
		subTitulo.setFont(new Font(30));

		Button novoJogo = new Button("Novo Jogo");
		Button comoJogar = new Button("Como Jogar");
		Button creditos = new Button("Créditos");
		Button sair = new Button("Sair");

		VBox telaPrincipal = new VBox(
			5,
			titulo,
			subTitulo,
			novoJogo,
			comoJogar,
			creditos,
			sair
		);
		telaPrincipal.setAlignment(Pos.CENTER);

		telaPrincipal.setMargin(subTitulo, new Insets(0, 0, 25, 0));

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
