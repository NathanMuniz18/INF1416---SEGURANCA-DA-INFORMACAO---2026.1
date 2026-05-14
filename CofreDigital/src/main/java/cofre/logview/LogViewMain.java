package cofre.logview;

import cofre.crypto.CryptoManager;
import cofre.db.ChaveiroDAO;
import cofre.db.DatabaseManager;

import java.io.Console;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LogViewMain {

    public static void main(String[] args) throws Exception {

        // 1. Verifica se o caminho da chave privada foi passado na linha de comando
        if (args.length < 1) {
            System.out.println("Uso: logview <caminho_chave_privada>");
            System.exit(1);
        }

        String caminhoChave = args[0];

        // 2. Lê a frase secreta via teclado sem echo
        Console console = System.console();
        if (console == null) {
            System.out.println("Erro: terminal não suporta leitura sem echo!");
            System.exit(1);
        }

        char[] fraseArray = console.readPassword("Frase secreta: ");
        String frase = new String(fraseArray);

        // 3. Inicializa o banco de dados
        DatabaseManager.inicializar();

        // 4. Lê os bytes da chave privada do arquivo
        byte[] chaveBytes;
        try {
            FileInputStream fis = new FileInputStream(caminhoChave);
            chaveBytes = fis.readAllBytes();
            fis.close();
        } catch (Exception e) {
            System.out.println("Erro: arquivo de chave privada não encontrado!");
            System.exit(1);
            return;
        }

        // 5. Tenta restaurar a chave privada com a frase secreta
        PrivateKey chavePrivada;
        try {
            chavePrivada = CryptoManager.restaurarChavePrivada(chaveBytes, frase);
        } catch (Exception e) {
            System.out.println("Erro: frase secreta inválida!");
            System.exit(1);
            return;
        }

        // 6. Valida assinando um array aleatório de 2048 bytes
        try {
            // Busca o certificado do admin no banco
            String certPEM = ChaveiroDAO.buscarCertificado(1);
            X509Certificate cert = CryptoManager.restaurarCertificado(certPEM);

            byte[] dados = new byte[2048];
            new SecureRandom().nextBytes(dados);
            byte[] assinatura = CryptoManager.assinar(dados, chavePrivada);
            boolean valido = CryptoManager.verificarAssinatura(
                    dados, assinatura, cert.getPublicKey());

            if (!valido) {
                System.out.println("Erro: validação da chave privada falhou!");
                System.exit(1);
                return;
            }
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("\n=== LOG DO SISTEMA - COFRE DIGITAL ===\n");

        // 7. Busca e exibe os registros em ordem cronológica
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT r.RID, r.DATA_HORA, r.MID, r.UID, r.ARQ_NOME, " +
                            "m.TEXTO, u.EMAIL " +
                            "FROM Registros r " +
                            "JOIN Mensagens m ON r.MID = m.MID " +
                            "LEFT JOIN Usuarios u ON r.UID = u.UID " +
                            "ORDER BY r.DATA_HORA ASC"
            );

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String dataHora = rs.getString("DATA_HORA");
                int mid = rs.getInt("MID");
                String texto = rs.getString("TEXTO");
                String email = rs.getString("EMAIL");
                String arqNome = rs.getString("ARQ_NOME");

                // Substitui os placeholders {0} e {1} pelo valor real
                if (email != null) {
                    texto = texto.replace("{0}", email);
                }
                if (arqNome != null) {
                    texto = texto.replace("{1}", arqNome);
                }

                System.out.printf("[%s] (Código: %d) %s%n", dataHora, mid, texto);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            System.out.println("Erro ao ler logs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}