//Nathan 2212759
//Hanna 2310289


package cofre.ui;

import cofre.SessaoSistema;
import cofre.crypto.CryptoManager;
import cofre.db.ChaveiroDAO;

import javax.swing.*;
import java.awt.*;
import java.security.PrivateKey;
import java.security.SecureRandom;

public class TelaFraseAdmin extends JFrame {

    private JPasswordField txtFrase;

    public TelaFraseAdmin() {
        setTitle("Cofre Digital - Partida do Sistema");
        setSize(400, 180);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder(
                "Validação da Chave Privada do Administrador"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        painel.add(new JLabel("Frase secreta do admin:"), gbc);

        txtFrase = new JPasswordField(20);
        gbc.gridx = 1; gbc.weightx = 1.0;
        painel.add(txtFrase, gbc);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnOK = new JButton("OK");
        btnOK.addActionListener(e -> validarFrase());
        getRootPane().setDefaultButton(btnOK);
        painelBotoes.add(btnOK);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        painel.add(painelBotoes, gbc);

        add(painel);
    }

    private void validarFrase() {
        String frase = new String(txtFrase.getPassword());

        if (frase.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Digite a frase secreta!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Busca a chave privada do admin no banco (UID=1)
            byte[] chaveBytes = ChaveiroDAO.buscarChavePrivada(1);

            if (chaveBytes == null) {
                JOptionPane.showMessageDialog(this,
                        "Chave do administrador não encontrada!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Tenta restaurar a chave privada com a frase fornecida
            // Valida assinando um array aleatório de 9216 bytes
            PrivateKey chavePrivada;
            try {
                chavePrivada = CryptoManager.restaurarChavePrivada(
                        chaveBytes, frase);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Frase secreta inválida! Sistema será encerrado.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }

            // Valida assinando um array aleatório de 9216 bytes
            String certPEM = ChaveiroDAO.buscarCertificado(1);
            cofre.crypto.CryptoManager.restaurarCertificado(certPEM);
            java.security.cert.X509Certificate cert =
                    CryptoManager.restaurarCertificado(certPEM);

            byte[] dados = new byte[9216];
            new SecureRandom().nextBytes(dados);
            byte[] assinatura = CryptoManager.assinar(dados, chavePrivada);
            boolean valido = CryptoManager.verificarAssinatura(
                    dados, assinatura, cert.getPublicKey());

            if (!valido) {
                JOptionPane.showMessageDialog(this,
                        "Validação da chave privada falhou! Sistema será encerrado.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }

            // Frase válida — salva em memória e abre TelaLogin
            SessaoSistema.setFraseSecretaAdmin(frase);
            dispose();
            new TelaLogin().setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erro: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}