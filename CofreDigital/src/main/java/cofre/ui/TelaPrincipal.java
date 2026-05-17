package cofre.ui;

import cofre.db.RegistroDAO;
import cofre.db.UsuarioDAO;
import cofre.model.Usuario;

import javax.swing.*;
import java.awt.*;

public class TelaPrincipal extends JFrame {

    private Usuario usuario;

    public TelaPrincipal(Usuario usuario) {
        this.usuario = usuario;

        setTitle("Cofre Digital - Menu Principal");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        try {
            // Incrementa contador de acessos
            UsuarioDAO.incrementarAcessos(usuario.getUid());
            usuario.setCt(usuario.getCt() + 1);

            RegistroDAO.registrar(5001, usuario.getUid());
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

        JPanel corpo2 = new JPanel(new GridLayout(4, 1, 5, 5));
        corpo2.setBorder(BorderFactory.createTitledBorder("Menu Principal"));

        // Opção 1 - só aparece para administrador
        if (usuario.isAdmin()) {
            JButton btn1 = new JButton("Cadastrar um novo usuário");
            btn1.addActionListener(e -> {
                try {
                    RegistroDAO.registrar(5002, usuario.getUid());
                } catch (Exception ex) {}
                dispose();
                new TelaCadastro(usuario).setVisible(true);
            });
            corpo2.add(btn1);
        }

        // Opção 2
        JButton btn2 = new JButton("Consultar pasta de arquivos secretos");
        btn2.addActionListener(e -> {
            try {
                RegistroDAO.registrar(5003, usuario.getUid());
            } catch (Exception ex) {}
            dispose();
            new TelaConsulta(usuario).setVisible(true);
        });
        corpo2.add(btn2);

        // Opção 3 - Sair
        JButton btn3 = new JButton("Sair do Sistema");
        btn3.addActionListener(e -> {
            try {
                RegistroDAO.registrar(5004, usuario.getUid());
            } catch (Exception ex) {}
            dispose();
            new TelaSaida(usuario).setVisible(true);
        });
        corpo2.add(btn3);

        // Monta centro
        JPanel centro = new JPanel(new BorderLayout());
        centro.add(corpo1, BorderLayout.NORTH);
        centro.add(corpo2, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }
}