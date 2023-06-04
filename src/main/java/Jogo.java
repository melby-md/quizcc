import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.PreparedStatement;

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

public class Jogo extends Application {
	private Personagem player1, player2;

	private void escolher(Button b, Stage stage, Text txt) {
		Personagem p = new Personagem(b.getText());
		if (player1 == null) {
			player1 = p;
			b.setDisable(true);
			txt.setText("Escolha o segundo personagem");
		} else {
			player2 = p; 
			stage.getScene().setRoot(jogo());
		}
	}

	private StackPane jogo() {
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
		VBox vbox = new VBox(5);
		vbox.setAlignment(Pos.CENTER);
		try {
			Connection con = DataBase.getCon();
			Statement stmt = con.createStatement();
			ResultSet perguntas = stmt.executeQuery("""
				SELECT * FROM perguntas	WHERE id IN
				(SELECT id FROM perguntas
				 ORDER BY RANDOM() LIMIT 5)
			""");
		
		if (perguntas.next()) {
			Text pergunta = new Text(perguntas.getString("enunciado"));
			vbox.getChildren().add(pergunta);
			PreparedStatement ps = con.prepareStatement("""
				SELECT enunciado, status
				FROM alternativas WHERE pergunta_id = ?
			""");
			ps.setInt(1, perguntas.getInt("id"));
			ResultSet alternativas = ps.executeQuery();
			while (alternativas.next()) {
				vbox.getChildren().add(new Button(alternativas.getString("enunciado")));
			}
		}
		} catch (SQLException e) {
			System.err.println("Erro SQL: " + e.getMessage());
			System.exit(1);
		} 
		return new StackPane(hbox1, hbox2, vbox);
	}

	private StackPane telaDeSelecao(Stage stage) {
		Button personagem1 = new Button("Evandro");
		Button personagem2 = new Button("Jorge");
		Button personagem3 = new Button("Agnaldo");
		Text titulo = new Text("Escolha o primeiro personagem");
		titulo.setFont(new Font(25));
		personagem1.setOnAction(e -> escolher(personagem1, stage, titulo));
		personagem2.setOnAction(e -> escolher(personagem2, stage, titulo));
		personagem3.setOnAction(e -> escolher(personagem3, stage, titulo));

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

		Text titulo = new Text("Quiz CC");
		titulo.setFont(new Font(50));

		Text subtitulo = new Text("(CC significa Ciência da Computação)");
		subtitulo.setFont(new Font(30));

		Button novoJogo = new Button("Novo Jogo");
		novoJogo.setOnAction(e ->
			stage.getScene().setRoot(telaDeSelecao(stage))
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

		StackPane telaPrincipal = new StackPane(vbox);
		stage.setScene(new Scene(telaPrincipal));
		stage.show();
	}

	public static void main(String[] args) {
	 
			launch(args);
		
	}
}
