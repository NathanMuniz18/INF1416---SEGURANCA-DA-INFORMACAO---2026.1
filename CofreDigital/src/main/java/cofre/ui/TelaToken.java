package cofre.ui;

import cofre.crypto.CryptoManager;
import cofre.crypto.TOTP;
import cofre.db.RegistroDAO;
import cofre.db.UsuarioDAO;
import cofre.model.Usuario;

import javax.crypto.SecretKey;
import javax.swing.*;
import java.awt.*;
import java.util.Base64;

public class TelaToken extends JFrame {

    private Usuario usuario;
    private String senhaCorreta;
    private int tentativasErro = 0;
    private JTextField txtToken;

    public TelaToken(Usuario usuario, String senhaCorreta) {
        this.usuario = usuario;
        this.senhaCorreta = senhaCorreta;

        setTitle("Cofre Digital - Autenticação (Etapa 3)");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        try {
            RegistroDAO.registrar(4001, usuario.getUid());
        } catch (Exception e) {
            e.printStackTrace();
        }

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder(
                "Cofre Digital - Autenticação"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label e campo do token
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        painel.add(new JLabel("TOTP:"), gbc);

        txtToken = new JTextField(10);
        gbc.gridx = 1; gbc.weightx = 1.0;
        painel.add(txtToken, gbc);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnOK = new JButton("OK");
        JButton btnLimpar = new JButton("LIMPAR");

        btnOK.addActionListener(e -> validarToken());
        btnLimpar.addActionListener(e -> txtToken.setText(""));

        getRootPane().setDefaultButton(btnOK);

        painelBotoes.add(btnOK);
        painelBotoes.add(btnLimpar);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        painel.add(painelBotoes, gbc);

        add(painel);
    }

    private void validarToken() {
        String codigoDigitado = txtToken.getText().trim();

        if (codigoDigitado.isEmpty() || codigoDigitado.length() != 6) {
            JOptionPane.showMessageDialog(this,
                    "Digite o código de 6 dígitos do Google Authenticator!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Decifra a TOKEN_KEY usando a senha pessoal do usuário
            // A TOKEN_KEY está cifrada com AES gerado da senha pessoal
            SecretKey chaveAES = CryptoManager.gerarChaveAES(senhaCorreta);
            byte[] tokenKeyCifrado = Base64.getDecoder()
                    .decode(usuario.getTokenKey());
            byte[] tokenKeyBytes = CryptoManager.decifrarAES(
                    tokenKeyCifrado, chaveAES);
            String tokenKeyBase32 = new String(tokenKeyBytes, "UTF-8");

            TOTP totp = new TOTP(tokenKeyBase32, 30);

            boolean valido = totp.validateCode(codigoDigitado);

            if (valido) {
                try {
                    RegistroDAO.registrar(4003, usuario.getUid());
                    RegistroDAO.registrar(4002, usuario.getUid());
                    RegistroDAO.registrar(1003, usuario.getUid());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                tentativasErro = 0;
                dispose();
                new TelaPrincipal(usuario).setVisible(true);

            } else {
                processarErro();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao validar token: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void processarErro() {
        tentativasErro++;

        try {
            if (tentativasErro == 1)
                RegistroDAO.registrar(4004, usuario.getUid());
            else if (tentativasErro == 2)
                RegistroDAO.registrar(4005, usuario.getUid());
            else
                RegistroDAO.registrar(4006, usuario.getUid());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tentativasErro >= 3) {
            try {
                UsuarioDAO.bloquear(usuario.getUid());
                RegistroDAO.registrar(4007, usuario.getUid());
                RegistroDAO.registrar(4002, usuario.getUid());
            } catch (Exception e) {
                e.printStackTrace();
            }

            JOptionPane.showMessageDialog(this,
                    "Acesso bloqueado por 2 minutos!",
                    "Bloqueio", JOptionPane.ERROR_MESSAGE);

            dispose();
            new TelaLogin().setVisible(true);

        } else {
            JOptionPane.showMessageDialog(this,
                    "Token inválido! Tentativa " + tentativasErro + " de 3.",
                    "Erro", JOptionPane.WARNING_MESSAGE);
            txtToken.setText("");
        }
    }
}