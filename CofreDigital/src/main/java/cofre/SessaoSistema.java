//Nathan 2212759
//Hanna 2310289


package cofre;

public class SessaoSistema {

    private static String fraseSecretaAdmin = null;

    public static void setFraseSecretaAdmin(String frase) {
        fraseSecretaAdmin = frase;
    }

    public static String getFraseSecretaAdmin() {
        return fraseSecretaAdmin;
    }


    public static void limpar() {
        fraseSecretaAdmin = null;
    }
}