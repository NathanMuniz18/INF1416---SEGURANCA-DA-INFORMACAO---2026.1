package teste;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeradorDeTestes {

    private static final String DIR_CHAVES = "C:/Users/hanna/Downloads/Pacote-T3/Pacote-T3/Keys/";
    private static final String DIR_PASTA_SEGURA = "C:/Users/hanna/Downloads/CofreTestes/";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Iniciando geração de Testes Intrusivos...");
        File dir = new File(DIR_PASTA_SEGURA);
        if (!dir.exists()) dir.mkdirs();

        // 1. Carrega as chaves e certificados
        PrivateKey privUser01 = decriptarChave(DIR_CHAVES + "user01-pkcs8-aes.key", "user01");
        PublicKey pubUser01 = lerCertificado(DIR_CHAVES + "user01-x509.crt");
        String emailUser01 = extrairEmailDoCertificado(DIR_CHAVES + "user01-x509.crt");

        PrivateKey privUser02 = decriptarChave(DIR_CHAVES + "user02-pkcs8-aes.key", "user02");
        PublicKey pubUser02 = lerCertificado(DIR_CHAVES + "user02-x509.crt");
        String emailUser02 = extrairEmailDoCertificado(DIR_CHAVES + "user02-x509.crt");

        PrivateKey privAdmin = decriptarChave(DIR_CHAVES + "admin-pkcs8-aes.key", "admin");
        PublicKey pubAdmin = lerCertificado(DIR_CHAVES + "admin-x509.crt");
        String emailAdmin = extrairEmailDoCertificado(DIR_CHAVES + "admin-x509.crt");

        System.out.println("Emails extraídos:");
        System.out.println("  user01 -> " + emailUser01);
        System.out.println("  user02 -> " + emailUser02);
        System.out.println("  admin  -> " + emailAdmin);

        // 2. Gera arquivos de teste (os .enc, .env, .asd)
        gerarArquivoProtegido("ARQ_PERFEITO", "Conteudo secreto e inviolado.", pubUser01, privUser01, false, false);
        gerarArquivoProtegido("ARQ_FALSO_AUTENTICIDADE", "Hacker assinou isso.", pubUser01, privUser02, false, false);
        gerarArquivoProtegido("ARQ_FALSO_INTEGRIDADE", "Arquivo que sera corrompido.", pubUser01, privUser01, true, false);
        gerarArquivoProtegido("ARQ_FALSO_SIGILO", "Semente errada.", pubUser01, privUser01, false, true);

        System.out.println("✅ Arquivos individuais gerados.");

        // 3. Gera o arquivo de índice (index.enc, index.env, index.asd)
        StringBuilder sb = new StringBuilder();
        // Formato: nomeCodigo nomeSecreto emailDono grupo
        // Grupo deve ser exatamente "Administrador" ou "Usuario" (maiúsculo, sem acento)
        sb.append("ARQ_PERFEITO ARQ_PERFEITO ").append(emailUser01).append(" Usuario\n");
        sb.append("ARQ_FALSO_AUTENTICIDADE ARQ_FALSO_AUTENTICIDADE ").append(emailUser01).append(" Usuario\n");
        sb.append("ARQ_FALSO_INTEGRIDADE ARQ_FALSO_INTEGRIDADE ").append(emailUser01).append(" Usuario\n");
        sb.append("ARQ_FALSO_SIGILO ARQ_FALSO_SIGILO ").append(emailUser01).append(" Usuario\n");

        // O envelope do índice é cifrado com a chave pública do ADMIN (pubAdmin)
        // A assinatura é feita com a chave privada do ADMIN (privAdmin)
        gerarArquivoProtegido("index", sb.toString(), pubAdmin, privAdmin, false, false);

        System.out.println("✅ Arquivo de índice gerado com sucesso na pasta: " + DIR_PASTA_SEGURA);
        System.out.println("Pronto! Agora você pode testar o cofre com esses arquivos.");
    }

    /**
     * Gera um arquivo protegido (nomeCodigo.enc, .env, .asd) na pasta segura.
     *
     * @param nomeCodigo        nome base do arquivo (ex: "ARQ_PERFEITO")
     * @param conteudo          conteúdo em texto plano
     * @param pubKeyDono        chave pública do dono (para cifrar a semente no envelope)
     * @param privKeyAssinatura chave privada que irá assinar o conteúdo (pode ser do admin ou do próprio dono)
     * @param corromperEnc      se true, corrompe o arquivo .enc para simular falha de integridade
     * @param corromperEnv      se true, usa semente falsa no envelope para simular falha de sigilo
     */
    private static void gerarArquivoProtegido(String nomeCodigo, String conteudo,
                                              PublicKey pubKeyDono, PrivateKey privKeyAssinatura,
                                              boolean corromperEnc, boolean corromperEnv) throws Exception {
        // Gera uma semente aleatória de 32 bytes
        byte[] semente = new byte[32];
        new SecureRandom().nextBytes(semente);

        // Deriva uma chave AES de 256 bits a partir da semente usando SHA1PRNG (igual ao sistema)
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(semente);
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, sr);
        SecretKey aesKey = keyGen.generateKey();

        // Cifra o conteúdo com AES/ECB/PKCS5Padding
        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encBytes = aesCipher.doFinal(conteudo.getBytes());

        if (corromperEnc) {
            // Corrompe um byte no meio do arquivo cifrado
            encBytes[encBytes.length / 2] ^= 0x01;
        }

        // Cria o envelope digital: cifra a semente com a chave pública do dono
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, pubKeyDono);
        byte[] envBytes;
        if (corromperEnv) {
            // Usa uma semente falsa (diferente da usada para cifrar)
            byte[] sementeFalsa = new byte[32];
            new SecureRandom().nextBytes(sementeFalsa);
            envBytes = rsaCipher.doFinal(sementeFalsa);
        } else {
            envBytes = rsaCipher.doFinal(semente);
        }

        // Assina o conteúdo (texto plano) com a chave privada fornecida (SHA1withRSA)
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privKeyAssinatura);
        signature.update(conteudo.getBytes());
        byte[] asdBytes = signature.sign();

        // Salva os três arquivos na pasta segura
        salvar(DIR_PASTA_SEGURA + nomeCodigo + ".enc", encBytes);
        salvar(DIR_PASTA_SEGURA + nomeCodigo + ".env", envBytes);
        salvar(DIR_PASTA_SEGURA + nomeCodigo + ".asd", asdBytes);
    }

    private static void salvar(String path, byte[] dados) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(dados);
        }
    }

    /**
     * Lê um certificado X.509 e retorna sua chave pública.
     */
    private static PublicKey lerCertificado(String path) throws Exception {
        try (FileInputStream fis = new FileInputStream(path)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
            return cert.getPublicKey();
        }
    }

    /**
     * Extrai o e-mail (EMAILADDRESS ou E) do campo SubjectDN do certificado.
     * Exatamente como o sistema faz no TelaCadastro.
     */
    private static String extrairEmailDoCertificado(String pathCert) throws Exception {
        try (FileInputStream fis = new FileInputStream(pathCert)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
            String dn = cert.getSubjectDN().getName();

            Pattern pattern = Pattern.compile("EMAILADDRESS=([^,]+)");
            Matcher matcher = pattern.matcher(dn);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            pattern = Pattern.compile("E=([^,]+)");
            matcher = pattern.matcher(dn);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return "nao_encontrado";
        }
    }

    /**
     * Decifra uma chave privada PKCS8 que está cifrada com AES usando uma frase secreta.
     * Esta lógica é idêntica à do CryptoManager.restaurarChavePrivada do sistema principal.
     */
    private static PrivateKey decriptarChave(String path, String fraseSecreta) throws Exception {
        byte[] bytesChaveCifrada = Files.readAllBytes(new File(path).toPath());

        // Gera a chave AES a partir da frase secreta (SHA1PRNG)
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(fraseSecreta.getBytes("UTF-8"));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, sr);
        SecretKey chaveAES = kg.generateKey();

        // Decifra os bytes da chave privada
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, chaveAES);
        byte[] bytesBase64 = cipher.doFinal(bytesChaveCifrada);

        // Converte de Base64 (formato PEM sem cabeçalho)
        String pemStr = new String(bytesBase64, "UTF-8");
        pemStr = pemStr.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytesChave = Base64.getDecoder().decode(pemStr);

        // Cria o objeto PrivateKey
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytesChave);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}