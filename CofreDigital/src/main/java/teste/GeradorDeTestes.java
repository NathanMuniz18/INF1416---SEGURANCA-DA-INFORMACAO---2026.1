package teste;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.util.Base64; // Adicionado para decodificar a sua chave

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

        // 1. Carrega as Chaves usando a SUA lógica exata
        PrivateKey privUser01 = decriptarChave(DIR_CHAVES + "user01-pkcs8-aes.key", "user01");
        PublicKey pubUser01 = lerCertificado(DIR_CHAVES + "user01-x509.crt");

        PrivateKey privUser02 = decriptarChave(DIR_CHAVES + "user02-pkcs8-aes.key", "user02");
        PublicKey pubUser02 = lerCertificado(DIR_CHAVES + "user02-x509.crt");
        PublicKey pubAdmin = lerCertificado(DIR_CHAVES + "admin-x509.crt");
        // CENÁRIOS DE TESTE
        gerarArquivoProtegido("ARQ_PERFEITO", "Conteudo secreto e inviolado.", pubUser01, privUser01, false, false);
        gerarArquivoProtegido("ARQ_FALSO_AUTENTICIDADE", "Hacker assinou isso.", pubUser01, privUser02, false, false);
        gerarArquivoProtegido("ARQ_FALSO_INTEGRIDADE", "Arquivo que sera corrompido.", pubUser01, privUser01, true, false);
        gerarArquivoProtegido("ARQ_FALSO_SIGILO", "Semente errada.", pubUser01, privUser01, false, true);

        System.out.println("✅ Arquivos de Teste gerados com sucesso na pasta: " + DIR_PASTA_SEGURA);
  
     // 1. Carrega a chave do Admin para assinar o índice
        PrivateKey privAdmin = decriptarChave(DIR_CHAVES + "admin-pkcs8-aes.key", "admin");

        // 2. Monta o conteúdo do índice (as linhas que a JTable vai ler)
        StringBuilder sb = new StringBuilder();
        sb.append("index index admin\n");
        sb.append("ARQ_PERFEITO ARQ_PERFEITO user01\n");
        sb.append("ARQ_FALSO_AUTENTICIDADE ARQ_FALSO_AUTENTICIDADE user01\n");
        sb.append("ARQ_FALSO_INTEGRIDADE ARQ_FALSO_INTEGRIDADE user01\n");
        sb.append("ARQ_FALSO_SIGILO ARQ_FALSO_SIGILO user01\n");

        // 3. Gera o index.enc, index.env e index.asd
        // Assinado pelo Admin, mas envelopado para o User01 conseguir abrir
        gerarArquivoProtegido("index", sb.toString(), pubUser01, privAdmin, false, false);        System.out.println("✅ Ficheiro INDEX gerado com sucesso!");
    }

    private static void gerarArquivoProtegido(String nomeCodigo, String conteudo, PublicKey pubKeyDono, PrivateKey privKeyAssinatura, boolean corromperEnc, boolean corromperEnv) throws Exception {
        byte[] semente = new byte[32];
        new SecureRandom().nextBytes(semente);

        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(semente);
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256, sr);
        SecretKey aesKey = keyGen.generateKey();

        Cipher aesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encBytes = aesCipher.doFinal(conteudo.getBytes());

        if (corromperEnc) encBytes[encBytes.length / 2] ^= 0x01; 

        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, pubKeyDono);
        
        byte[] envBytes;
        if (corromperEnv) {
            byte[] sementeFalsa = new byte[32];
            new SecureRandom().nextBytes(sementeFalsa);
            envBytes = rsaCipher.doFinal(sementeFalsa);
        } else {
            envBytes = rsaCipher.doFinal(semente);
        }

        // CORRIGIDO: Usando SHA1withRSA igualzinho ao seu CryptoManager!
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privKeyAssinatura); 
        signature.update(conteudo.getBytes());
        byte[] asdBytes = signature.sign();

        salvar(DIR_PASTA_SEGURA + nomeCodigo + ".enc", encBytes);
        salvar(DIR_PASTA_SEGURA + nomeCodigo + ".env", envBytes);
        salvar(DIR_PASTA_SEGURA + nomeCodigo + ".asd", asdBytes);
    }

    private static void salvar(String path, byte[] dados) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(dados);
        }
    }

    private static PublicKey lerCertificado(String path) throws Exception {
        byte[] certBytes = Files.readAllBytes(new File(path).toPath());
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
        return cert.getPublicKey();
    }

    // CORRIGIDO: Agora copia exatamente a lógica do seu CryptoManager.restaurarChavePrivada
    private static PrivateKey decriptarChave(String path, String senha) throws Exception {
        byte[] bytesChaveCifrada = Files.readAllBytes(new File(path).toPath());
        
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(senha.getBytes("UTF-8"));
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, sr);
        SecretKey chaveAES = kg.generateKey();

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, chaveAES);
        
        // Decifra e converte para Base64 (Igual ao seu sistema)
        byte[] bytesBase64 = cipher.doFinal(bytesChaveCifrada);
        String pemStr = new String(bytesBase64, "UTF-8");
        pemStr = pemStr.replace("-----BEGIN PRIVATE KEY-----", "")
                       .replace("-----END PRIVATE KEY-----", "")
                       .replaceAll("\\s", "");
                       
        byte[] bytesChave = Base64.getDecoder().decode(pemStr);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytesChave);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
    
}