package cofre.ui;

import cofre.db.RegistroDAO;
import cofre.model.Usuario;

import javax.swing.*;
import java.awt.*;

public class TelaSaida extends JFrame {

    private Usuario usuario;

    public TelaSaida(Usuario usuario) {
        this.usuario = usuario;

        setTitle("Cofre Digital - Saída");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        try {
            RegistroDAO.registrar(8001, usuario.getUid());
        } catch (Exception e) {
            e.printStackTrace();
        }

        inicializarComponentes();
    }

    private void inicializarComponentes() {

        JPanel cabecalho = new JPanel(new GridLayout(3, 1));
        cabecalho.setBorder(BorderFactory.createTitledBorder("Usuário Logado"));
        cabecalho.add(new JLabel("  Login: " + usuario.getEmail()));
        cabecalho.add(new JLabel("  Grupo: " +
                (usuario.getGid() == 1 ? "Administrador" : "Usuário")));
        cabecalho.add(new JLabel("  Nome: " + usuario.getNome()));
        add(cabecalho, BorderLayout.NORTH);

        JPanel corpo1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        corpo1.setBorder(BorderFactory.createTitledBorder("Acessos"));
        corpo1.add(new JLabel("Total de acessos do usuário: " + usuario.getCt()));

        JPanel corpo2 = new JPanel(new BorderLayout(10, 10));
        corpo2.setBorder(BorderFactory.createTitledBorder("Saída do Sistema"));

        JLabel msgSaida = new JLabel(
                "Pressione o botão Encerrar Sessão ou o botão Encerrar Sistema para confirmar.",
                SwingConstants.CENTER);
        msgSaida.setFont(new Font("Arial", Font.PLAIN, 12));
        corpo2.add(msgSaida, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton btnSessao = new JButton("Encerrar Sessão");
        JButton btnSistema = new JButton("Encerrar Sistema");
        JButton btnVoltar = new JButton("Voltar para o Menu Principal");

        btnSessao.addActionListener(e -> {
            try {
                RegistroDAO.registrar(8002, usuario.getUid());
                RegistroDAO.registrar(1004, usuario.getUid());
            } catch (Exception ex) {}
            dispose();
            new TelaLogin().setVisible(true);
        });

        btnSistema.addActionListener(e -> {
            try {
                RegistroDAO.registrar(8003, usuario.getUid());
                RegistroDAO.registrar(1002);
            } catch (Exception ex) {}
            System.exit(0);
        });

        btnVoltar.addActionListener(e -> {
            try {
                RegistroDAO.registrar(8004, usuario.getUid());
            } catch (Exception ex) {}
            dispose();
            new TelaPrincipal(usuario).setVisible(true);
        });

        painelBotoes.add(btnSessao);
        painelBotoes.add(btnSistema);
        painelBotoes.add(btnVoltar);
        corpo2.add(painelBotoes, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(corpo1, BorderLayout.NORTH);
        centro.add(corpo2, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }
}