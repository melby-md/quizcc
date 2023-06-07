import java.util.ArrayDeque;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javafx.scene.Node;
import javafx.application.Application;	
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;

public class Jogo extends Application {
	private Personagem player1, player2;
	
	private StackPane fim() {
			Text txt = new Text("FIM");
			txt.setFont(new Font(50));
			return new StackPane(txt);
	}

	private void escolher(Button b, Text txt) {
		Personagem p = new Personagem(b.getText());
		if (player1 == null) {
			player1 = p;
			player1.setStatus(true);
			b.setDisable(true);
			txt.setText("Escolha o segundo personagem");
		} else {
			player2 = p; 
			player2.setStatus(false);
			jogo(b.getScene());
		}
	}

	private boolean responder(Pergunta p, boolean status, VBox botoes, Button btn) {
		boolean ret;

		if (player1.getStatus()) {
			if (!status)
				if (player1.subVida(0.5))
						botoes.getScene().setRoot(fim());
			btn.setStyle("-fx-text-fill: #0000ff;");
			ret = false;
		} else {
			if (!status)
				if (player2.subVida(0.5))
						botoes.getScene().setRoot(fim());
			for (int i = 0; i < p.getAlternativas().size(); ++i) {
				Node b = botoes.getChildren().get(i);
				b.setMouseTransparent(true);
				b.setFocusTraversable(false);
				Alternativa a = p.getAlternativas().get(i);
				if (a.getStatus())
					b.setStyle("-fx-text-fill: #00ff00;");
				else
					b.setStyle("-fx-text-fill: #ff0000;");
			}
			ret = true;

		}
		player1.toggleStatus();
		player2.toggleStatus();
		return ret;
	}

	private void displayPerguntas(ArrayDeque<Pergunta> perguntas,
								  VBox vbox,
							      VBox botoes, 
								  Text enunciado,
							      ProgressBar vida1,
							      ProgressBar vida2)
	{

		if (perguntas.isEmpty())
				botoes.getScene().setRoot(fim());

		Pergunta pergunta = perguntas.pop();

		enunciado.setText(pergunta.getEnunciado());

		Button next = new Button("Próxima pergunta");
		next.setOnAction(e -> {
			vbox.getChildren().remove(next);
			displayPerguntas(perguntas, vbox, botoes, enunciado, vida1, vida2);
		});

		botoes.getChildren().clear();
		for (Alternativa a : pergunta.getAlternativas()) {
			Button btn = new Button(a.getEnunciado());
			btn.setOnAction(e -> {
				if (responder(pergunta, a.getStatus(), botoes, btn)) {
					vida1.setProgress(player1.getVida());
					vida2.setProgress(player2.getVida());
					vbox.getChildren().add(next);
				}
			});
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
				SELECT * FROM perguntas	WHERE id IN
				(SELECT id FROM perguntas
				 ORDER BY RANDOM() LIMIT 5)
			""");
			PreparedStatement ps = con.prepareStatement("""
				SELECT enunciado, status
				FROM alternativas WHERE pergunta_id = ?
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
							System.out.println(ars.getBoolean("status"));
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
