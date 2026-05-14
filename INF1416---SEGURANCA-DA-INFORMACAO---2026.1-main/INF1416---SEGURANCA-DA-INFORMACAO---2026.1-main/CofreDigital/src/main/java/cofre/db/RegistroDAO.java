package cofre.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RegistroDAO {

    // Formato da data/hora que será gravada no banco
    private static final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Registra um evento SEM usuário associado.
     * Usado para eventos gerais do sistema, ex: 1001 (Sistema iniciado)
     */
    public static void registrar(int mid) throws Exception {
        registrar(mid, null, null);
    }

    /**
     * Registra um evento COM usuário, mas SEM arquivo.
     * Usado para eventos de autenticação, ex: 2003 (login identificado)
     */
    public static void registrar(int mid, Integer uid) throws Exception {
        registrar(mid, uid, null);
    }

    /**
     * Registra um evento COM usuário E COM arquivo.
     * Usado para eventos de acesso a arquivos, ex: 7010 (arquivo selecionado)
     * Todos os outros métodos acima chamam este no final.
     */
    public static void registrar(int mid, Integer uid, String arqNome) throws Exception {
        Connection conn = DatabaseManager.getConnection();

        // Prepara o INSERT com parâmetros (? são os valores que vamos preencher)
        // Usar PreparedStatement evita SQL injection e é mais seguro
        String sql = "INSERT INTO Registros (DATA_HORA, MID, UID, ARQ_NOME) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);

        // Preenche os parâmetros na ordem
        ps.setString(1, LocalDateTime.now().format(fmt)); // data e hora atual
        ps.setInt(2, mid);                                 // código da mensagem

        // UID pode ser nulo (eventos sem usuário)
        if (uid != null) ps.setInt(3, uid);
        else ps.setNull(3, java.sql.Types.INTEGER);

        // Nome do arquivo pode ser nulo (eventos sem arquivo)
        if (arqNome != null) ps.setString(4, arqNome);
        else ps.setNull(4, java.sql.Types.VARCHAR);

        ps.executeUpdate(); // executa o INSERT
        ps.close();
    }
}