import java.util.ArrayDeque;
import java.sql.*;

import javafx.application.Application;	
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;

public class Jogo extends Application {
	private Personagem player1, player2;
	
	private StackPane fim() {
			String s;
			if (player1.getVida() == player2.getVida())
				s = "EMPATE!";
			else if (player1.getVida() > player2.getVida())
				s = "VENCEDOR: " + player1.getNome();
			else
				s = "VENCEDOR: " + player2.getNome();
			Text txt = new Text(s);
			txt.setFont(new Font(50));
			return new StackPane(txt);
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
			jogo(b.getScene());
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
			if (!player1.getResposta().getStatus()) {
				player1.subVida(0.1);
				if (player1.getVida() <= 0)
					vbox.getScene().setRoot(fim());
			}
			if (!player2.getResposta().getStatus()) {
				player2.subVida(0.1);
				if (player1.getVida() <= 0)
					vbox.getScene().setRoot(fim());
			}
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
			vbox.getChildren().remove(confirmacao);
			for (Node b : botoes.getChildren())
					b.setStyle(null);
		});

		botoes.getChildren().clear();
		byte letra = 0;
		for (Alternativa a : pergunta.getAlternativas()) {
			Button btn = new Button((char)('a' + letra) + ") " + a.getEnunciado());
			btn.setOnAction(e -> {
				Personagem p = player1.getStatus() ? player1 : player2;
				p.setResposta(a);
				btn.setStyle("-fx-text-fill: #0000ff;");

				if (p == player2) {
					vbox.getChildren().add(confirmacao);
				}

				player1.toggleStatus();
				player2.toggleStatus();
			});
			letra = (byte)((letra + 1) % 26);
			botoes.getChildren().add(btn);
		}
	}

	private void jogo(Scene scene) {
		ProgressBar vida1 = new ProgressBar();
		ProgressBar vida2 = new ProgressBar();
		vida1.setProgress(1);
		vida2.setProgress(1);

		HBox hbox1 = new HBox(
			5,
			new Text(player1.getNome()),
			vida1
		);
		HBox hbox2 = new HBox(
			5,
			vida2,
			new Text(player2.getNome())
		);
		hbox1.setAlignment(Pos.TOP_LEFT);
		hbox2.setAlignment(Pos.TOP_RIGHT);
		hbox1.setPadding(new Insets(15));
		hbox2.setPadding(new Insets(15));

		vida1.prefWidthProperty().bind(hbox1.widthProperty().divide(2).subtract(100));
		vida2.prefWidthProperty().bind(hbox2.widthProperty().divide(2).subtract(100));

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
		scene.setRoot(new StackPane(hbox1, hbox2, vbox));
	}

	private StackPane telaDeSelecao() {
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

		VBox vbox = new VBox(
			10,
			titulo,
			hbox
		);

		vbox.setAlignment(Pos.CENTER);
		hbox.setAlignment(Pos.CENTER);

		return new StackPane(vbox);
	}

	// Esse método tem que ser público
	@Override
	public void start(Stage stage) throws SQLException {
		DataBase.connect();

		stage.setTitle("quizcc");
		stage.setMaximized(true);

		StackPane telaPrincipal = new StackPane();
		Scene scene = new Scene(telaPrincipal);
		scene.getStylesheets().add("style.css");
			  
		Text titulo = new Text("Quiz CC");
		titulo.setFont(new Font(50));
		titulo.setFill(Color.RED);

		Text subtitulo = new Text("(CC significa Ciência da Computação)");
		subtitulo.setFont(new Font(30));

		Button novoJogo = new Button("Novo Jogo");
		novoJogo.setOnAction(e ->
			scene.setRoot(telaDeSelecao())
		);
		Button leaderboard = new Button("Leaderboard");
		Button creditos = new Button("Créditos");

		VBox vbox = new VBox(
			15,
			titulo,
			subtitulo,
			novoJogo,
			leaderboard,
			creditos
		);
		vbox.setAlignment(Pos.CENTER);

		telaPrincipal.getChildren().add(vbox);

		stage.setScene(scene);
		stage.show();
	}

	public static void main(String[] args) {
			launch(args);
	}
}
