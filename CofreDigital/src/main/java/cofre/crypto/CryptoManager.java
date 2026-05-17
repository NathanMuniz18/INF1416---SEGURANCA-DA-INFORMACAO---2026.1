//Nathan 2212759
//Hanna 2310289


package cofre.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class CryptoManager {

    // Registra o BouncyCastle como provedor de criptografia
    // Deve ser chamado uma vez antes de qualquer operação criptográfica
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // GERAÇÃO DE CHAVE AES A PARTIR DE UMA FRASE SECRETA
    // Usa SHA1PRNG como gerador, conforme especificado no enunciado

    public static SecretKey gerarChaveAES(String fraseSecreta) throws Exception {
        // SHA1PRNG é um gerador pseudo-aleatório baseado em SHA1
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");

        // A frase secreta é usada como semente do gerador
        sr.setSeed(fraseSecreta.getBytes("UTF-8"));

        // Gera a chave AES de 256 bits usando a semente
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, sr);
        return kg.generateKey();
    }

    // -------------------------------------------------------
    // CRIPTOGRAFIA / DECRIPTOGRAFIA AES/ECB/PKCS5Padding
    // -------------------------------------------------------

    /**
     * Cifra um array de bytes com AES/ECB/PKCS5Padding.
     * Usado para cifrar a chave privada e a chave TOTP.
     *
     * @param dados bytes a cifrar
     * @param chave chave AES
     * @return bytes cifrados
     */
    public static byte[] cifrarAES(byte[] dados, SecretKey chave) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, chave);
        return cipher.doFinal(dados);
    }

    /**
     * Decifra um array de bytes com AES/ECB/PKCS5Padding.
     * Usado para decifrar a chave privada e a chave TOTP.
     *
     * @param dados bytes cifrados
     * @param chave chave AES
     * @return bytes decifrados
     */
    public static byte[] decifrarAES(byte[] dados, SecretKey chave) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, chave);
        return cipher.doFinal(dados);
    }

    // -------------------------------------------------------
    // RESTAURAÇÃO DA CHAVE PRIVADA (PKCS8)
    // -------------------------------------------------------

    /**
     * Restaura a chave privada a partir dos bytes cifrados e da frase secreta.
     * Fluxo: bytes cifrados → decifra AES → decodifica Base64 → PKCS8 → PrivateKey
     *
     * @param bytesChaveCifrada bytes da chave privada cifrada (vinda do banco)
     * @param fraseSecreta      frase secreta do usuário
     * @return objeto PrivateKey pronto para uso
     */
    public static PrivateKey restaurarChavePrivada(byte[] bytesChaveCifrada,
                                                   String fraseSecreta) throws Exception {
        // 1. Gera a chave AES a partir da frase secreta
        SecretKey chaveAES = gerarChaveAES(fraseSecreta);

        // 2. Decifra os bytes da chave privada
        byte[] bytesBase64 = decifrarAES(bytesChaveCifrada, chaveAES);

        // 3. O resultado é a chave privada em Base64 (formato PEM sem cabeçalho)
        //    Remove cabeçalho/rodapé PEM se existir e decodifica Base64
        String pemStr = new String(bytesBase64, "UTF-8");
        pemStr = pemStr.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytesChave = Base64.getDecoder().decode(pemStr);

        // 4. Cria o objeto PKCS8EncodedKeySpec com os bytes decodificados
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytesChave);

        // 5. Usa KeyFactory para gerar o objeto PrivateKey
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Lê e cifra a chave privada de um arquivo .key para armazenar no banco.
     * O arquivo .key é binário cifrado com AES usando a frase secreta.
     * Retorna os bytes cifrados prontos para gravar no banco.
     *
     * @param caminhoArquivo caminho do arquivo .key no disco
     * @param fraseSecreta   frase secreta do usuário
     * @return bytes cifrados da chave privada
     */
    public static byte[] lerChavePrivadaDoArquivo(String caminhoArquivo,
                                                  String fraseSecreta) throws Exception {
        // Lê o arquivo binário da chave privada
        FileInputStream fis = new FileInputStream(caminhoArquivo);
        byte[] bytesArquivo = fis.readAllBytes();
        fis.close();

        // O arquivo já vem cifrado com AES usando a frase secreta
        // Vamos decifrar para validar, e depois guardar o binário original no banco
        SecretKey chaveAES = gerarChaveAES(fraseSecreta);
        decifrarAES(bytesArquivo, chaveAES); // lança exceção se a frase estiver errada

        // Retorna os bytes originais do arquivo para guardar no banco
        return bytesArquivo;
    }

    /**
     * Restaura o certificado X.509 a partir de uma String PEM.
     * Usado para obter a chave pública do usuário.
     *
     * @param pem certificado em formato PEM (texto Base64 com cabeçalho)
     * @return objeto X509Certificate
     */
    public static X509Certificate restaurarCertificado(String pem) throws Exception {
        // Converte a String PEM para bytes
        byte[] certBytes = pem.getBytes("UTF-8");

        // Usa CertificateFactory para criar o objeto X509Certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(certBytes)
        );
    }

    /**
     * Lê um certificado X.509 de um arquivo .crt e retorna como String PEM.
     * Usado no cadastro para ler o certificado do arquivo fornecido pelo usuário.
     *
     * @param caminhoArquivo caminho do arquivo .crt no disco
     * @return certificado em formato PEM (String)
     */
    public static String lerCertificadoDoArquivo(String caminhoArquivo) throws Exception {
        FileInputStream fis = new FileInputStream(caminhoArquivo);
        byte[] bytes = fis.readAllBytes();
        fis.close();
        return new String(bytes, "UTF-8");
    }

    /**
     * Assina um array de bytes com a chave privada usando SHA1withRSA.
     * Usado para gerar o arquivo .asd dos arquivos protegidos.
     *
     * @param dados       bytes a assinar
     * @param chavePrivada chave privada do usuário
     * @return bytes da assinatura digital
     */
    public static byte[] assinar(byte[] dados, PrivateKey chavePrivada) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initSign(chavePrivada);
        sig.update(dados);
        return sig.sign();
    }

    /**
     * Verifica uma assinatura digital com a chave pública.
     * Usado para verificar a integridade dos arquivos protegidos.
     *
     * @param dados      bytes originais que foram assinados
     * @param assinatura bytes da assinatura digital
     * @param chavePublica chave pública do usuário
     * @return true se a assinatura for válida, false caso contrário
     */
    public static boolean verificarAssinatura(byte[] dados, byte[] assinatura,
                                              PublicKey chavePublica) throws Exception {
        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(chavePublica);
        sig.update(dados);
        return sig.verify(assinatura);
    }

    /**
     * Cifra uma semente com a chave pública do usuário (envelope digital).
     * Usado para proteger a semente AES dos arquivos .env
     *
     * @param semente    bytes da semente a proteger
     * @param chavePublica chave pública do destinatário
     * @return bytes cifrados (envelope digital)
     */
    public static byte[] cifrarEnvelope(byte[] semente,
                                        PublicKey chavePublica) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, chavePublica);
        return cipher.doFinal(semente);
    }

    /**
     * Decifra um envelope digital com a chave privada do usuário.
     * Recupera a semente AES para poder decifrar o arquivo .enc
     *
     * @param envelope    bytes do envelope cifrado
     * @param chavePrivada chave privada do usuário
     * @return semente AES decifrada
     */
    public static byte[] decifrarEnvelope(byte[] envelope,
                                          PrivateKey chavePrivada) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, chavePrivada);
        return cipher.doFinal(envelope);
    }
}