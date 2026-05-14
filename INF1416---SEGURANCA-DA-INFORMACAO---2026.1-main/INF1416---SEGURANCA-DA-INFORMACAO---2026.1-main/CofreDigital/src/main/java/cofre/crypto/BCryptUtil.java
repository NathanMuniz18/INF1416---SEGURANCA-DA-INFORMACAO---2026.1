package cofre.crypto;

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

import java.security.SecureRandom;

public class BCryptUtil {

    // Custo do BCrypt conforme especificado no enunciado (2^8 iterações)
    private static final int CUSTO = 8;

    /**
     * Gera o hash BCrypt de uma senha pessoal.
     * Gera um SALT aleatório de 16 bytes automaticamente.
     * Retorna uma String de 60 caracteres no formato:
     * $2y$08$BASE64(salt)BASE64(hash)
     *
     * @param senha senha pessoal em texto plano (ex: "13572468")
     * @return hash BCrypt de 60 caracteres
     */
    public static String gerarHash(String senha) {
        // Gera um SALT aleatório de 16 bytes
        try { //precisa ser sha1prng obrigatoriamente ne?
            byte[] salt = new byte[16];
            SecureRandom.getInstance("SHA1PRNG").nextBytes(salt);

        // Gera o hash BCrypt versão 2y com custo 8
        // O método generate recebe: senha como char[], salt e custo
            return OpenBSDBCrypt.generate("2y",
                    senha.toCharArray(),
                    salt,
                    CUSTO);
        } catch (Exception e) {
            throw new RuntimeException("Erro crítico: Algoritmo SHA1PRNG não encontrado no sistema.", e);
        }

    }

    /**
     * Verifica se uma senha confere com um hash BCrypt armazenado.
     * Usado na etapa 2 de autenticação (validação da senha pessoal).
     *
     * @param hash  hash BCrypt armazenado no banco (60 caracteres)
     * @param senha senha fornecida pelo usuário no teclado virtual
     * @return true se a senha conferir, false caso contrário
     */
    public static boolean verificar(String hash, String senha) {
        return OpenBSDBCrypt.checkPassword(hash, senha.toCharArray());
    }
}