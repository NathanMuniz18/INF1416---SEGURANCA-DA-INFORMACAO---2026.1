package cofre.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UsuarioDAO {

    private static final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static boolean existeUsuario() throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM Usuarios"
        );
        ResultSet rs = ps.executeQuery();
        int total = rs.getInt(1);
        rs.close();
        ps.close();
        return total > 0;
    }

    public static ResultSet buscarPorEmail(String email) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM Usuarios WHERE EMAIL = ?"
        );
        ps.setString(1, email);
        return ps.executeQuery();
    }

    public static int inserir(String nome, String email, String hash,
                              String tokenKey, int gid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Usuarios (NOME, EMAIL, HASH, TOKEN_KEY, GID) VALUES (?, ?, ?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS // pede para retornar o UID gerado
        );
        ps.setString(1, nome);
        ps.setString(2, email);
        ps.setString(3, hash);
        ps.setString(4, tokenKey);
        ps.setInt(5, gid);
        ps.executeUpdate();

        // Pega o UID que foi gerado automaticamente
        ResultSet rs = ps.getGeneratedKeys();
        int uid = rs.getInt(1);
        rs.close();
        ps.close();
        return uid;
    }

    public static void atualizarKeyId(int uid, int kid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE Usuarios SET KEYID = ? WHERE UID = ?"
        );
        ps.setInt(1, kid);
        ps.setInt(2, uid);
        ps.executeUpdate();
        ps.close();
    }

    public static void incrementarAcessos(int uid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE Usuarios SET CT = CT + 1 WHERE UID = ?"
        );
        ps.setInt(1, uid);
        ps.executeUpdate();
        ps.close();
    }

    public static void bloquear(int uid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE Usuarios SET BLK = 1, BLK_TIME = ? WHERE UID = ?"
        );
        ps.setString(1, LocalDateTime.now().format(fmt));
        ps.setInt(2, uid);
        ps.executeUpdate();
        ps.close();
    }

    public static void desbloquear(int uid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE Usuarios SET BLK = 0, BLK_TIME = NULL WHERE UID = ?"
        );
        ps.setInt(1, uid);
        ps.executeUpdate();
        ps.close();
    }

    public static boolean estaBloqueado(int uid, String blkTime) throws Exception {
        if (blkTime == null) return false;

        // Calcula quanto tempo passou desde o bloqueio
        LocalDateTime horaBloqueo = LocalDateTime.parse(blkTime, fmt);
        LocalDateTime agora = LocalDateTime.now();
        long segundos = java.time.Duration.between(horaBloqueo, agora).getSeconds();

        if (segundos >= 120) {
            // 2 minutos passaram, desbloqueia
            desbloquear(uid);
            return false;
        }
        return true;
    }

    public static int totalUsuarios() throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM Usuarios"
        );
        ResultSet rs = ps.executeQuery();
        int total = rs.getInt(1);
        rs.close();
        ps.close();
        return total;
    }
}