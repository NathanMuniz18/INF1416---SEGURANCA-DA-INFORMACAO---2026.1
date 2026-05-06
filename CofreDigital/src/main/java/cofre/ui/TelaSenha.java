package cofre.ui;

import cofre.model.Usuario;

import javax.swing.*;
import java.awt.*;

public class TelaSenha extends JFrame {

    private Usuario usuario;

    public TelaSenha(Usuario usuario) {
        this.usuario = usuario;

        setTitle("Cofre Digital - Senha Pessoal");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel painel = new JPanel(new BorderLayout(10, 10));
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        painel.add(new JLabel(
                "Etapa 2 - Teclado virtual (em construção)",
                SwingConstants.CENTER), BorderLayout.CENTER);

        // Botão temporário para pular direto para a TelaPrincipal
        JButton btnPular = new JButton(">> Pular para Menu Principal (temporário)");
        btnPular.addActionListener(e -> {
            dispose();
            new TelaPrincipal(usuario).setVisible(true);
        });
        painel.add(btnPular, BorderLayout.SOUTH);

        add(painel);
    }
}