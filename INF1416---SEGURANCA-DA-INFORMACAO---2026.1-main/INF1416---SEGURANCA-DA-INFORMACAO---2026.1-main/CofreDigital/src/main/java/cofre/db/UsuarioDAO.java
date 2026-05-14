package cofre.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UsuarioDAO {

    private static final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Verifica se existe algum usuário cadastrado no banco.
     * Usado na partida do sistema para saber se é a primeira execução.
     */
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

    /**
     * Busca um usuário pelo e-mail (login name).
     * Retorna um ResultSet com os dados do usuário, ou vazio se não encontrar.
     * Usado na etapa 1 de autenticação.
     */
    public static ResultSet buscarPorEmail(String email) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM Usuarios WHERE EMAIL = ?"
        );
        ps.setString(1, email);
        return ps.executeQuery();
    }

    /**
     * Insere um novo usuário no banco.
     * Retorna o UID gerado automaticamente pelo banco.
     * Usado no cadastro de usuários.
     *
     * @param nome      nome extraído do certificado
     * @param email     e-mail extraído do certificado (login name)
     * @param hash      senha pessoal cifrada com BCrypt
     * @param tokenKey  chave TOTP cifrada com AES em BASE32
     * @param gid       grupo: 1=Administrador, 2=Usuário
     */
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

    /**
     * Atualiza o KEYID do usuário após salvar o certificado no Chaveiro.
     * Chamado logo após inserir o certificado na tabela Chaveiro.
     */
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

    /**
     * Incrementa o contador de acessos do usuário em 1.
     * Chamado após cada login bem-sucedido.
     */
    public static void incrementarAcessos(int uid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE Usuarios SET CT = CT + 1 WHERE UID = ?"
        );
        ps.setInt(1, uid);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Bloqueia o acesso do usuário por 2 minutos.
     * Grava o horário atual como BLK_TIME e seta BLK = 1.
     * Chamado após 3 erros consecutivos na etapa 2 ou 3.
     */
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

    /**
     * Desbloqueia o usuário (BLK = 0, BLK_TIME = null).
     * Chamado quando os 2 minutos de bloqueio já passaram.
     */
    public static void desbloquear(int uid) throws Exception {
        Connection conn = DatabaseManager.getConnection();
        PreparedStatement ps = conn.prepareStatement(
                "UPDATE Usuarios SET BLK = 0, BLK_TIME = NULL WHERE UID = ?"
        );
        ps.setInt(1, uid);
        ps.executeUpdate();
        ps.close();
    }

    /**
     * Verifica se o bloqueio do usuário já expirou (2 minutos).
     * Se sim, desbloqueia automaticamente e retorna false.
     * Se não, retorna true (ainda bloqueado).
     */
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

    /**
     * Retorna o total de usuários cadastrados no sistema.
     * Exibido na tela de cadastro.
     */
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