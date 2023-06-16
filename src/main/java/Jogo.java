import java.io.*;
import java.sql.*;
import java.util.ArrayDeque;
import java.util.regex.*;

import javafx.application.*;
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

	// Essa classe é um wrapper para que nodes possam enviar uma string junto
	// com um evento para outro node.
	private static class MsgEvent extends Event {
		private String msg = null;

		public MsgEvent(EventType<MsgEvent> e) {
			super(e);
		}

		public MsgEvent(EventType<MsgEvent> e, String msg) {
			super(e);
			this.msg = msg;
		}

		public String getMsg() {
			return this.msg;
		}
	}

	private static final EventType<Event> TELA_INICIAL = new EventType<>("TELA_INICIAL");

	public static Button botaoVoltar() {
		Button voltar = new Button("Voltar");
		voltar.setCancelButton(true);
		voltar.setOnAction(e -> voltar.fireEvent(new Event(TELA_INICIAL)));

		return voltar;
	}

	private BorderPane fim(Personagem player1, Personagem player2) {
		String s;
		if (player1.getVida() == player2.getVida())
			s = "EMPATE!";
		else if (player1.getVida() > player2.getVida())
			s = "VENCEDOR: " + player1.getNome();
		else
			s = "VENCEDOR: " + player2.getNome();

		Text txt = new Text(s);
		txt.setFont(new Font(50));

		Button voltar = botaoVoltar();
		BorderPane.setMargin(voltar, new Insets(15));

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(txt);
		borderPane.setTop(voltar);

		return borderPane;
	}

	private BorderPane jogo(Personagem player1, Personagem player2) {
		final EventType<Event> FIM = new EventType<>("FIM");
		final EventType<Event> DISPLAY_PERGUNTA = new EventType<>("DISPLAY_PERGUNTA");
		final EventType<MsgEvent> UPDATE_MSG = new EventType<>("UPDATE_MSG");

		ProgressBar vida1 = new ProgressBar(1);
		ProgressBar vida2 = new ProgressBar(1);

		// inverte uma das barras de vida para ficar que nem um jogo de luta
		vida2.setScaleX(-1);

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

		Text rodada = new Text();
		GridPane.setHalignment(rodada, HPos.CENTER);

		GridPane topo = new GridPane();
		BorderPane.setMargin(topo, new Insets(10, 0, 0, 0));

		// coordenadas esotéricas
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

		BorderPane borderPane = new BorderPane();
		borderPane.setTop(topo);
		borderPane.setCenter(vbox);

		// Os elementos criado abaixo são adicionados posteriormente na tela
		Button sim = new Button("sim");
		Button nao = new Button("nao");
		HBox hbox = new HBox(20, sim, nao);
		hbox.setAlignment(Pos.CENTER);
		VBox confirmacao = new VBox(20, new Text("Você tem certeza de suas respostas?"), hbox);
		confirmacao.setAlignment(Pos.CENTER);

		Button next = new Button("Próxima pergunta");

		ArrayDeque<Pergunta> perguntas = DataBase.fetchPerguntas();

		// CALLBACKS PARA A FAMÍLIA INTEIRA

		borderPane.addEventHandler(FIM, e -> {
			borderPane.fireEvent(new MsgEvent(UPDATE_MSG, "Fim de jogo!"));
			borderPane.setCenter(fim(player1, player2));
		});

		borderPane.addEventHandler(UPDATE_MSG, e -> {
			String msg = e.getMsg();
			if (msg == null)
				msg = "Vez de " + (player1.getStatus() ? player1 : player2).getNome();
			rodada.setText(msg);
		});

		borderPane.addEventHandler(DISPLAY_PERGUNTA, e -> {
			if (perguntas.isEmpty()) {
				borderPane.fireEvent(new Event(FIM));
				return;
			}

			Pergunta pergunta = perguntas.pop();

			enunciado.setText(pergunta.getEnunciado());

			byte letra = 0;
			botoes.getChildren().clear();
			for (Alternativa a : pergunta.getAlternativas()) {
				Button btn = new Button((char)('a' + letra) + ") " + a.getEnunciado());
				btn.setMinWidth(700);
				btn.setOnAction(f -> {
					Personagem p = player1.getStatus() ? player1 : player2;
					p.setResposta(a);
					btn.setStyle("-fx-text-fill: #0000ff;");

					player1.toggleStatus();
					player2.toggleStatus();

					if (p == player2) {
						vbox.getChildren().add(confirmacao);
					} else {
						btn.fireEvent(new MsgEvent(UPDATE_MSG));
					}
				});
				letra = (byte)((letra + 1) % 26);
				botoes.getChildren().add(btn);
			}

			sim.setOnAction(f -> {
				boolean status1 = player1.getResposta().getStatus();
				boolean status2 = player2.getResposta().getStatus();

				if (!status1) {
					player1.dano();
					if (player1.getVida() <= 0) {
						sim.fireEvent(new Event(FIM));
						return;
					}
					vida1.setProgress(player1.getVida());
				}

				if (!status2) {
					player2.dano();
					if (player2.getVida() <= 0) {
						sim.fireEvent(new Event(FIM));
						return;
					}
					vida2.setProgress(player2.getVida());
				}

				String msg;
				if (!status1 && !status2)
					msg = "Ambos os jogadores erraram!";
				else if (status1 && status2)
					msg = "Ambos os jogadores acertaram!";
				else
					msg = (status1 ? player1 : player2).getNome() + " acertou!";
				sim.fireEvent(new MsgEvent(UPDATE_MSG, msg));

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

				vbox.getChildren().remove(confirmacao);
				vbox.getChildren().add(next);
			});
		});

		next.setOnAction(e -> {
			next.fireEvent(new MsgEvent(UPDATE_MSG));
			next.fireEvent(new Event(DISPLAY_PERGUNTA));
			vbox.getChildren().remove(next);
		});

		nao.setOnAction(e -> {
			for (Node b : botoes.getChildren())
				b.setStyle(null);
			vbox.getChildren().remove(confirmacao);
		});

		borderPane.fireEvent(new MsgEvent(UPDATE_MSG));
		borderPane.fireEvent(new Event(DISPLAY_PERGUNTA));

		return borderPane;
	}

	private Button botaoPersonagem(String nome) {
		ImageView img = new ImageView(nome.toLowerCase() + ".jpg");
		Button b = new Button(nome, img);
		b.setContentDisplay(ContentDisplay.TOP);
		return b;
	}

	private BorderPane telaDeSelecao() {
		Personagem player1 = new Personagem(true);
		Personagem player2 = new Personagem(false);

		Text titulo = new Text("Escolha o primeiro personagem");
		titulo.setFont(new Font(25));
		
		EventHandler<ActionEvent> escolher = e -> {
			Button b = (Button)e.getSource();
			if (player1.getNome() == null) {
				player1.setNome(b.getText());
				b.setDisable(true);
				titulo.setText("Escolha o segundo personagem");
			} else {
				player2.setNome(b.getText());
				b.getScene().setRoot(jogo(player1, player2));
			}
		};

		Button personagem1 = botaoPersonagem("Evandro");
		Button personagem2 = botaoPersonagem("Jorge");
		Button personagem3 = botaoPersonagem("Agnaldo");

		personagem1.setOnAction(escolher);
		personagem2.setOnAction(escolher);
		personagem3.setOnAction(escolher);

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

		Button voltar = botaoVoltar();
		BorderPane.setMargin(voltar, new Insets(15));

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(vbox);
		borderPane.setTop(voltar);

		return borderPane;
	}

	private Button botaoMenu(String text) {
			Button b = new Button(text);
			b.setMinWidth(150);
			return b;
	}

	// Esse método tem que ser público
	@Override
	public void start(Stage stage) {
		DataBase.connect();

		stage.setTitle("quizcc");
		stage.setFullScreen(true);
		// muda o ícone na barra de tarefas
		stage.getIcons().add(new Image("jorge.jpg"));

		Text titulo = new Text("Quiz CC");
		titulo.setFont(new Font(50));
		titulo.setFill(Color.RED);

		Text subTitulo = new Text("(CC significa Ciência da Computação)");
		subTitulo.setFont(new Font(30));

		Button novoJogo = botaoMenu("Novo Jogo");
		Button comoJogar = botaoMenu("Como Jogar");
		Button creditos = botaoMenu("Créditos");
		Button sair = botaoMenu("Sair");

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

		Scene scene = new Scene(telaPrincipal);
		scene.getStylesheets().add("style.css");
		scene.addEventHandler(TELA_INICIAL, e -> scene.setRoot(telaPrincipal));

		novoJogo.setOnAction(e ->
			scene.setRoot(telaDeSelecao())
		);

		creditos.setOnAction(e ->
			scene.setRoot(new MDRenderer("creditos.md"))
		);

		comoJogar.setOnAction(e ->
			scene.setRoot(new MDRenderer("como_jogar.md"))
		);

		sair.setOnAction(e -> Platform.exit());

		stage.setScene(scene);
		stage.show();
	}

	// Esse também
	@Override
	public void stop() {
		DataBase.close();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
