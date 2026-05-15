package cofre;

import cofre.db.DatabaseManager;
import cofre.db.RegistroDAO;
import cofre.db.UsuarioDAO;
import cofre.ui.TelaCadastro;
import cofre.ui.TelaFraseAdmin;
import cofre.ui.TelaLogin;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        try {
            DatabaseManager.inicializar();
            RegistroDAO.registrar(1001);
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                SessaoSistema.limpar();
                try {
                    RegistroDAO.registrar(1002);
                } catch (Exception e) {
                    System.err.println("Erro ao registrar encerramento: " + e.getMessage());
                }
            }));

            if (!UsuarioDAO.existeUsuario()) {
                // Primeira execução — cadastro do admin
                RegistroDAO.registrar(1005);
                SwingUtilities.invokeLater(() -> {
                    new TelaCadastro(null).setVisible(true);
                });
            } else {
                // Execuções seguintes — pede frase do admin primeiro
                RegistroDAO.registrar(1006);
                SwingUtilities.invokeLater(() -> {
                    new TelaFraseAdmin().setVisible(true);
                });
            }
            

        } catch (Exception e) {
            System.err.println("Erro fatal ao iniciar o sistema:");
            e.printStackTrace();
        }
    }
}