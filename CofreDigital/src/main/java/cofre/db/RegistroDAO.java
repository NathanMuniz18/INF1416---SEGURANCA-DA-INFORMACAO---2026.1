//Nathan 2212759
//Hanna 2310289


package cofre.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RegistroDAO {

    private static final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void registrar(int mid) throws Exception {
        registrar(mid, null, null);
    }

    public static void registrar(int mid, Integer uid) throws Exception {
        registrar(mid, uid, null);
    }

    public static void registrar(int mid, Integer uid, String arqNome) throws Exception {
        Connection conn = DatabaseManager.getConnection();

        String sql = "INSERT INTO Registros (DATA_HORA, MID, UID, ARQ_NOME) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);

        ps.setString(1, LocalDateTime.now().format(fmt)); // data e hora atual
        ps.setInt(2, mid);

        if (uid != null) ps.setInt(3, uid);
        else ps.setNull(3, java.sql.Types.INTEGER);

        if (arqNome != null) ps.setString(4, arqNome);
        else ps.setNull(4, java.sql.Types.VARCHAR);

        ps.executeUpdate(); // executa o INSERT
        ps.close();
    }
}