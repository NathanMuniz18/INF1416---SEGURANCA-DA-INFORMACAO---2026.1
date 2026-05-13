package cofre;

public class SessaoSistema {

    // Frase secreta do admin mantida em memória durante a execução
    // Apagada quando o sistema encerra
    private static String fraseSecretaAdmin = null;

    /**
     * Salva a frase secreta do admin em memória.
     * Chamado após cadastro ou validação bem sucedida na partida.
     */
    public static void setFraseSecretaAdmin(String frase) {
        fraseSecretaAdmin = frase;
    }

    /**
     * Retorna a frase secreta do admin que está em memória.
     */
    public static String getFraseSecretaAdmin() {
        return fraseSecretaAdmin;
    }

    /**
     * Apaga a frase secreta da memória.
     * Chamado quando o sistema encerra.
     */
    public static void limpar() {
        fraseSecretaAdmin = null;
    }
}