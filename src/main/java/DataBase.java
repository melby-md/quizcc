import java.io.*;
import java.sql.*;
import java.util.ArrayDeque;

// Essa classe é só uma variavel global glorificada e algumas funções
class DataBase {
	private static Connection con;

	public static void connect() throws SQLException {
		try {
			con = DriverManager.getConnection("jdbc:sqlite:quizcc.sqlite3");
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			close();
			System.exit(1);
		}

		try (
			// Cria as tabelas necessárias caso o banco seja recem-criado
			Statement stmt = con.createStatement();

			// Essa query é longa e ultrapassa 80 colunas, por isso quebrar
			// ela em várias linhas, o java suporta esse tipo de string des
			// do 15, mas, alguns editores ainda não fazem o syntax hilight
			// direito, incluindo o meu :P
			//
			// Essa query usa uma funão específica do sqlite para listar
			// todas as tabelas
			ResultSet rs = stmt.executeQuery("""
				SELECT name
				FROM sqlite_master
				WHERE type='table'
			""");
		) {

			if (rs.next())
				return;

			// Caso não aja tabelas...
			stmt.executeUpdate("""
				CREATE TABLE perguntas (
					id INTEGER PRIMARY KEY,
					enunciado TEXT
				)
			""");

			stmt.executeUpdate("""
				CREATE TABLE alternativas (
					id INTEGER PRIMARY KEY,
					enunciado TEXT,
					status INTEGER,
					pergunta_id INTEGER,
					FOREIGN KEY(pergunta_id) 
					REFERENCES perguntas(id)
				)
			""");
		} catch (Exception e) {
			System.err.println("Erro SQL: " + e.getMessage());
			System.exit(1);
		}
		
		try (
			InputStream file = 
			DataBase.class.getResourceAsStream("perguntas.txt");
			BufferedReader br = 
			new BufferedReader(new InputStreamReader(file, "UTF-8"))
		) {
			int id = 0;
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				if (line.startsWith(" ")) {
					PreparedStatement ps = con.prepareStatement("""
						INSERT INTO alternativas
						(enunciado, status, pergunta_id)
						VALUES (?, ?, ?)
					""");
					ps.setString(1, line.trim().replace(":",""));
					ps.setBoolean(2, line.startsWith(" :"));
					ps.setInt(3, id);
					ps.executeUpdate();
				} else if (line.length() > 0) {
					PreparedStatement ps = con.prepareStatement("INSERT INTO perguntas (enunciado) VALUES (?)");
					ps.setString(1, line);
					ps.executeUpdate();
					id++;
				}
			}
		} catch (Exception e) {
			System.err.println("Erro SQL: " + e.getMessage());
			System.exit(1);
		}
	}

	public static ArrayDeque<Pergunta> fetchPerguntas() {
		ArrayDeque<Pergunta> perguntas = new ArrayDeque<>();	

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

		return perguntas;
	}

	public static Connection getCon() {
		return con;
	}

	public static void close() {
		if (con != null)
			try {
				con.close();
			} catch (SQLException e) {}
	}
}
