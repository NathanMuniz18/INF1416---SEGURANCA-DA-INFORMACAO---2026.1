package cofre.crypto;
//a classe TOTP deve ser implementada 
//usando, APENAS, as classes de apoio 
//javax.crypto.Mac, javax.crypto.spec.SecretKeySpec, 
//java.util.Date e BASE32.
import cofre.util.Base32; 
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;

public class TOTP {
    private byte[] key = null;
    private long timeStepInSeconds = 30;


     // Construtor da classe. Recebe a chave secreta em BASE32 e o intervalo 
    // de tempo a ser adotado (default = 30 segundos). Deve decodificar a 
    // chave secreta e armazenar em key. Em caso de erro, gera Exception. 
    
    public TOTP(String base32EncodedSecret, long timeStepInSeconds) 
    throws Exception {
        this.timeStepInSeconds = timeStepInSeconds;

        Base32 base32 = new Base32(Base32.Alphabet.BASE32, false, false);
        this.key = base32.fromString(base32EncodedSecret);
        
        if (this.key == null) {
            throw new Exception("Erro ao decodificar a chave secreta BASE32.");
        }
    }

     // Recebe o HASH HMAC-SHA1 e determina o código TOTP de 6 algarismos 
    // decimais, prefixado com zeros quando necessário. 
    // RFC 4226 
    private String getTOTPCodeFromHash(byte[] hash) {
        // int offset   =  hmac_result[19] & 0xf ;
        // int bin_code = (hmac_result[offset]  & 0x7f) << 24
        //    | (hmac_result[offset+1] & 0xff) << 16
        //    | (hmac_result[offset+2] & 0xff) <<  8
        //    | (hmac_result[offset+3] & 0xff) ;

        int offset = hash[19] & 0xf;
        int bin_code = ((hash[offset] & 0x7f) << 24) |
                     ((hash[offset + 1] & 0xff) << 16) |
                     ((hash[offset + 2] & 0xff) << 8) |
                     (hash[offset + 3] & 0xff);

        int otp = bin_code % 1000000;
        return String.format("%06d", otp);
    }

    // Recebe o contador e a chave secreta para produzir o hash HMAC-SHA1. 
    //MAC =Message Authentication Code
    private byte[] HMAC_SHA1(byte[] counter, byte[] keyByteArray) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec macKey = new SecretKeySpec(keyByteArray, "RAW");
            mac.init(macKey);
            return mac.doFinal(counter);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular HMAC-SHA1", e);
        }
    }

     // Recebe o intervalo de tempo e executa o algoritmo TOTP para produzir  
    // o código TOTP. Usa os métodos auxiliares getTOTPCodeFromHash e HMAC_SHA1.     
    private String TOTPCode(long timeInterval) {
        byte[] counter = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counter[i] = (byte) (timeInterval & 0xFF);
            timeInterval >>= 8; 
        }
        
        byte[] hash = HMAC_SHA1(counter, this.key);
        return getTOTPCodeFromHash(hash);
    }

    // Método que é utilizado para solicitar a geração do código TOTP. 
    public String generateCode() {
        long currentTimeSeconds = new Date().getTime() / 1000;
        long timeInterval = currentTimeSeconds / this.timeStepInSeconds;
        return TOTPCode(timeInterval);
    }

    // Método que é utilizado para validar um código TOTP (inputTOTP). 
    // Deve considerar um atraso ou adiantamento de 30 segundos no  
    // relógio da máquina que gerou o código TOTP.      
    public boolean validateCode(String inputTOTP) {
        long currentTimeSeconds = new Date().getTime() / 1000;
        long timeInterval = currentTimeSeconds / this.timeStepInSeconds;

        String codeCurrent = TOTPCode(timeInterval);
        String codePast = TOTPCode(timeInterval - 1);
        String codeFuture = TOTPCode(timeInterval + 1);

        return inputTOTP.equals(codeCurrent) || 
               inputTOTP.equals(codePast) || 
               inputTOTP.equals(codeFuture);
    }
}