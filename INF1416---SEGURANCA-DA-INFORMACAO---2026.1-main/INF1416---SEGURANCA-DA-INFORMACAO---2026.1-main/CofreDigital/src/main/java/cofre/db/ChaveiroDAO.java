package cofre.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ChaveiroDAO {

    /**
     * Insere um par (certificado, chave privada) no Chaveiro.
     * Retorna o KID gerado automaticamente pelo banco.
     *
     * @param uid          UID do usuário dono do par
     * @param certificado  certificado X.509 em formato PEM (texto)
     * @param chavePrivada chave privada PKCS8 cifrada com AES (bytes)
     */
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

    /**
     * Busca o certificado PEM de um usuário pelo seu UID.
     * Usado para restaurar a chave pública na autenticação.
     */
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

    /**
     * Busca a chave privada cifrada de um usuário pelo seu UID.
     * Retorna os bytes crus — ainda precisam ser decriptados
     * com a frase secreta do usuário para usar.
     */
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