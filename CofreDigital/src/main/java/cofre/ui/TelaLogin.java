//Nathan 2212759
//Hanna 2310289


package cofre.ui;

import cofre.db.RegistroDAO;
import cofre.db.UsuarioDAO;
import cofre.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;

public class TelaLogin extends JFrame {

    private JTextField txtEmail;

    public TelaLogin() {
        setTitle("Cofre Digital - Autenticação");
        setSize(400, 180);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        try {
            RegistroDAO.registrar(2001); // Autenticação etapa 1 iniciada
        } catch (Exception e) {
            e.printStackTrace();
        }

        inicializarComponentes();
    }

    private void inicializarComponentes() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Cofre Digital - Autenticação"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label e campo de e-mail
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        painel.add(new JLabel("Login name:"), gbc);

        txtEmail = new JTextField(25);
        gbc.gridx = 1; gbc.weightx = 1.0;
        painel.add(txtEmail, gbc);

        // Botões OK e LIMPAR
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnOK = new JButton("OK");
        JButton btnLimpar = new JButton("LIMPAR");

        btnOK.addActionListener(e -> validarLogin());
        btnLimpar.addActionListener(e -> txtEmail.setText(""));

        // Permite pressionar Enter para confirmar
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

    private void validarLogin() {
        String email = txtEmail.getText().trim();

        // Validação básica de e-mail
        if (email.isEmpty() || !email.contains("@")) {
            JOptionPane.showMessageDialog(this,
                    "Login name inválido!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Busca o usuário no banco pelo e-mail
            ResultSet rs = UsuarioDAO.buscarPorEmail(email);

            if (!rs.next()) {
                // Usuário não encontrado
                RegistroDAO.registrar(2005);
                JOptionPane.showMessageDialog(this,
                        "Login name não identificado!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                rs.close();
                return;
            }

            // Monta o objeto Usuario com os dados do banco
            Usuario usuario = new Usuario();
            usuario.setUid(rs.getInt("UID"));
            usuario.setNome(rs.getString("NOME"));
            usuario.setEmail(rs.getString("EMAIL"));
            usuario.setHash(rs.getString("HASH"));
            usuario.setTokenKey(rs.getString("TOKEN_KEY"));
            usuario.setKeyId(rs.getInt("KEYID"));
            usuario.setGid(rs.getInt("GID"));
            usuario.setCt(rs.getInt("CT"));
            usuario.setBlk(rs.getInt("BLK"));
            usuario.setBlkTime(rs.getString("BLK_TIME"));
            rs.close();

            // Verifica se o usuário está bloqueado
            if (usuario.getBlk() == 1) {
                boolean aindaBloqueado = UsuarioDAO.estaBloqueado(
                        usuario.getUid(), usuario.getBlkTime());

                if (aindaBloqueado) {
                    RegistroDAO.registrar(2004, usuario.getUid());
                    JOptionPane.showMessageDialog(this,
                            "Acesso bloqueado! Tente novamente em alguns instantes.",
                            "Acesso Bloqueado", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // Login válido e desbloqueado — vai para etapa 2
            RegistroDAO.registrar(2003, usuario.getUid());
            RegistroDAO.registrar(2002, usuario.getUid());

            dispose();
            new TelaSenha(usuario).setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao verificar login: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}