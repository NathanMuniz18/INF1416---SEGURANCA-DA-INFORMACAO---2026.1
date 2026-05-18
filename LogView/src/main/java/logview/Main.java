//Nathan 2212759
//Hanna 2310289
package logview;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.Console;
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.*;
import java.util.Base64;

public class Main {

    public static void main(String[] args) throws Exception {

        Security.addProvider(new BouncyCastleProvider());

        // 1. Verifica se os argumentos foram passados
        if (args.length < 1) {
            System.out.println("Uso: logview <caminho_chave_privada>");
            System.exit(1);
        }

        String caminhoChave = args[0];
        String caminhoBanco = "../CofreDigital/cofre.db";

        // 2. Lê a frase secreta via teclado sem echo
        Console console = System.console();
        if (console == null) {
            System.out.println("Erro: execute pelo CMD, não pelo Eclipse/IntelliJ!");
            System.exit(1);
        }

        char[] fraseArray = console.readPassword("Frase secreta: ");
        String frase = new String(fraseArray);

        // 3. Lê os bytes do arquivo .key
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

        // 4. Restaura a chave privada
        PrivateKey chavePrivada;
        try {
            chavePrivada = restaurarChavePrivada(chaveBytes, frase);
        } catch (Exception e) {
            System.out.println("Erro: frase secreta inválida!");
            System.exit(1);
            return;
        }

        // 5. Valida assinando 2048 bytes aleatórios (conforme enunciado)
        try {
            String certPEM = buscarCertificadoAdmin(caminhoBanco);
            if (certPEM == null) {
                System.out.println("Erro: certificado do administrador não encontrado no banco!");
                System.exit(1);
                return;
            }

            X509Certificate cert = restaurarCertificado(certPEM);
            byte[] dados = new byte[2048];
            new SecureRandom().nextBytes(dados);
            byte[] assinatura = assinar(dados, chavePrivada);

            if (!verificarAssinatura(dados, assinatura, cert.getPublicKey())) {
                System.out.println("Erro: validação da chave privada falhou!");
                System.exit(1);
                return;
            }
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
            System.exit(1);
            return;
        }

        // 6. Exibe os logs em ordem cronológica
        System.out.println("\n=== LOG DO SISTEMA - COFRE DIGITAL ===\n");
        exibirLogs(caminhoBanco);
    }

    private static void exibirLogs(String caminhoBanco) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + caminhoBanco);

            // JOIN com Mensagens (coluna TEXTO) e Usuarios (coluna EMAIL)
            // ARQ_NOME é a coluna de arquivo na tabela Registros
            PreparedStatement ps = conn.prepareStatement(
                "SELECT r.DATA_HORA, r.MID, r.ARQ_NOME, u.EMAIL, m.TEXTO " +
                "FROM Registros r " +
                "LEFT JOIN Usuarios u ON r.UID = u.UID " +
                "LEFT JOIN Mensagens m ON r.MID = m.MID " +
                "ORDER BY r.DATA_HORA ASC"
            );

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dataHora = rs.getString("DATA_HORA");
                int    mid      = rs.getInt("MID");
                String arqNome  = rs.getString("ARQ_NOME"); // null para registros sem arquivo
                String email    = rs.getString("EMAIL");    // null para registros sem usuário
                String texto    = rs.getString("TEXTO");    // template: usa {0} e {1}

                // Interpola os placeholders:
                // {0} = login_name do usuário
                // {1} = nome do arquivo
                if (texto != null) {
                    texto = texto.replace("{0}", email   != null ? email   : "");
                    texto = texto.replace("{1}", arqNome != null ? arqNome : "");
                } else {
                    // Fallback: mensagem não cadastrada no banco
                    texto = "(mensagem não encontrada para código " + mid + ")";
                }

                System.out.printf("[%s] %d %s%n", dataHora, mid, texto);
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            System.out.println("Erro ao ler logs: " + e.getMessage());
        }
    }

    private static String buscarCertificadoAdmin(String caminhoBanco) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:" + caminhoBanco);
            // Admin é sempre UID = 1
            PreparedStatement ps = conn.prepareStatement(
                "SELECT CERTIFICADO FROM Chaveiro WHERE UID = 1"
            );
            ResultSet rs = ps.executeQuery();
            String cert = null;
            if (rs.next()) cert = rs.getString("CERTIFICADO");
            rs.close();
            ps.close();
            conn.close();
            return cert;
        } catch (Exception e) {
            return null;
        }
    }

    private static PrivateKey restaurarChavePrivada(byte[] bytesChaveCifrada,
                                                    String fraseSecreta) throws Exception {
        // Gera chave AES a partir da frase secreta com SHA1PRNG — idêntico ao sistema principal
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(fraseSecreta.getBytes("UTF-8"));
        javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance("AES");
        kg.init(256, sr);
        javax.crypto.SecretKey chaveAES = kg.generateKey();

        // Decifra os bytes da chave privada
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, chaveAES);
        byte[] bytesBase64 = cipher.doFinal(bytesChaveCifrada);

        // Remove cabeçalho/rodapé PEM e decodifica Base64
        String pemStr = new String(bytesBase64, "UTF-8");
        pemStr = pemStr.replace("-----BEGIN PRIVATE KEY-----", "")
                       .replace("-----END PRIVATE KEY-----", "")
                       .replaceAll("\\s", "");
        byte[] bytesChave = Base64.getDecoder().decode(pemStr);

        // Reconstrói o objeto PrivateKey
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytesChave);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private static X509Certificate restaurarCertificado(String pem) throws Exception {
        byte[] certBytes = pem.getBytes("UTF-8");
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
                new java.io.ByteArrayInputStream(certBytes));
    }

    private static byte[] assinar(byte[] dados, PrivateKey chavePrivada) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initSign(chavePrivada);
        sig.update(dados);
        return sig.sign();
    }

    private static boolean verificarAssinatura(byte[] dados, byte[] assinatura,
                                               PublicKey chavePublica) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(chavePublica);
        sig.update(dados);
        return sig.verify(assinatura);
    }
}
