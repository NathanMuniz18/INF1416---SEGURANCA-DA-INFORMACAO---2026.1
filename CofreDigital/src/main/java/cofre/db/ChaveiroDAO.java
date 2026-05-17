//Nathan 2212759
//Hanna 2310289


package cofre.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ChaveiroDAO {

    public static int inserir(int uid, String certificado,
                              byte[] chavePrivada) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Chaveiro (UID, CERTIFICADO, CHAVE_PRIVADA) VALUES (?, ?, ?)",
                java.sql.Statement.RETURN_GENERATED_KEYS
        );
        ps.setInt(1, uid);
        ps.setString(2, certificado);       // certificado PEM é texto
        ps.setBytes(3, chavePrivada);       // chave privada é binário
        ps.executeUpdate();

        // Pega o KID gerado automaticamente
        ResultSet rs = ps.getGeneratedKeys();
        int kid = rs.getInt(1);
        rs.close();
        ps.close();
        return kid;
    }

    public static String buscarCertificado(int uid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT CERTIFICADO FROM Chaveiro WHERE UID = ?"
        );
        ps.setInt(1, uid);
        ResultSet rs = ps.executeQuery();
        String cert = null;
        if (rs.next()) cert = rs.getString("CERTIFICADO");
        rs.close();
        ps.close();
        return cert;
    }

    public static byte[] buscarChavePrivada(int uid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT CHAVE_PRIVADA FROM Chaveiro WHERE UID = ?"
        );
        ps.setInt(1, uid);
        ResultSet rs = ps.executeQuery();
        byte[] chave = null;
        if (rs.next()) chave = rs.getBytes("CHAVE_PRIVADA");
        rs.close();
        ps.close();
        return chave;
    }
}