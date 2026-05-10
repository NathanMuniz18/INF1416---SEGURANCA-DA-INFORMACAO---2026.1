package cofre.ui;

import cofre.model.Usuario;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cofre.db.RegistroDAO;
import cofre.db.UsuarioDAO;

public class TelaSenha extends JFrame {

    private Usuario usuarioLogado;
    private JButton[] teclado;
    private JPasswordField senha;
    private List<String> sequenciaDigitada;
    private int tentativasErro = 0;

    public TelaSenha(Usuario usuario) {
        this.usuarioLogado = usuario;
        this.sequenciaDigitada = new ArrayList<>();
        
        setTitle("Cofre Digital - Autenticação (Etapa 2)");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        try {
            RegistroDAO.registrar(3001, usuario.getUid());
        } catch (Exception e) {
            e.printStackTrace();
        }

        inicializarComp();
        embaralharBotoes(); 
    }

    private void inicializarComp() {
        JPanel painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel painelTopo = new JPanel(new FlowLayout());
        painelTopo.add(new JLabel("Senha pessoal:"));
        senha = new JPasswordField(10);
        senha.setEditable(false);
        senha.setBackground(Color.WHITE);
        painelTopo.add(senha);
        painelPrincipal.add(painelTopo, BorderLayout.NORTH);

        JPanel painelTeclado = new JPanel(new GridLayout(1, 5, 5, 5));
        teclado = new JButton[5];
        
        for (int i = 0; i < 5; i++) {
            teclado[i] = new JButton();
            
            teclado[i].addActionListener(e -> {
                JButton btn = (JButton) e.getSource();
                registrarClique(btn.getText());
            });

            painelTeclado.add(teclado[i]);
        }
        painelPrincipal.add(painelTeclado, BorderLayout.CENTER);

        JPanel painelAcoes = new JPanel(new FlowLayout());
        JButton btnOk = new JButton("OK");
        JButton btnLimpar = new JButton("LIMPAR");

        btnLimpar.addActionListener(e -> limparSenha());
        btnOk.addActionListener(e -> validarSenha());

        painelAcoes.add(btnOk);
        painelAcoes.add(btnLimpar);
        painelPrincipal.add(painelAcoes, BorderLayout.SOUTH);

        add(painelPrincipal);
    }

    private void embaralharBotoes() {
        List<Integer> digitos = new ArrayList<>();
        for (int i = 0; i <= 9; i++) {
            digitos.add(i);
        }
        
        Collections.shuffle(digitos);

        int indexDigito = 0;
        for (int i = 0; i < 5; i++) {
            int num1 = digitos.get(indexDigito++);
            int num2 = digitos.get(indexDigito++);
            teclado[i].setText(num1 + " " + num2);
        }
    }

    private void registrarClique(String dupla) {
        // maximo 10 cliques 
        if (sequenciaDigitada.size() < 10) {
            sequenciaDigitada.add(dupla);
            
            String senhaAtual = new String(senha.getPassword());
            senha.setText(senhaAtual + "*");
            
            embaralharBotoes();
        }
    }

    private void limparSenha() {
        sequenciaDigitada.clear();
        senha.setText("");
        embaralharBotoes();
    }

    private void validarSenha() {
        if (sequenciaDigitada.size() < 8) { //PERGUNTAR precisa desse check??? ou deixa a senha errar?? PERGUNTAR
            JOptionPane.showMessageDialog(this, "A senha deve ter no mínimo 8 dígitos.");
            return;
        }

        List<String> possiveisSenhas = gerarCombinacoes(sequenciaDigitada);
        boolean autenticado = false;
        String senhaCorreta = null;

        System.out.println("Testando " + possiveisSenhas.size() + " combinações...");
        for (String senhaTeste : possiveisSenhas) {
            if (cofre.crypto.BCryptUtil.verificar(usuarioLogado.getHash(), senhaTeste)) {
                autenticado = true;
                senhaCorreta = senhaTeste;
                break;
            }
        }

        if (autenticado) {

            try {
                RegistroDAO.registrar(3003, usuarioLogado.getUid());
                RegistroDAO.registrar(3002, usuarioLogado.getUid());
            } catch (Exception e) {
                e.printStackTrace();
            }
            tentativasErro = 0;
            dispose();
            new TelaToken(usuarioLogado, senhaCorreta).setVisible(true);
        
        } else {
            processarErro();
        }
        
        //System.out.println("Duplas selecionadas: " + sequenciaDigitada);
    }


    private List<String> gerarCombinacoes(List<String> sequencia) {
        List<String> combinacoes = new ArrayList<>();
        gerarCombinacoesRecursivo(sequencia, 0, "", combinacoes);
        return combinacoes;
    }

    private void gerarCombinacoesRecursivo(List<String> sequencia, int index, String senhaAtual, List<String> combinacoes) {
        if (index == sequencia.size()) {
            combinacoes.add(senhaAtual);
            return;
        }

        String duplaAtual = sequencia.get(index);
        String[] numeros = duplaAtual.split(" ");
        String num1 = numeros[0];
        String num2 = numeros[1];

        gerarCombinacoesRecursivo(sequencia, index + 1, senhaAtual + num1, combinacoes);
        
        gerarCombinacoesRecursivo(sequencia, index + 1, senhaAtual + num2, combinacoes);
    }


    private void processarErro() {
        tentativasErro++;
        try {
            if (tentativasErro == 1) RegistroDAO.registrar(3004, usuarioLogado.getUid());
            else if (tentativasErro == 2) RegistroDAO.registrar(3005, usuarioLogado.getUid());
            else RegistroDAO.registrar(3006, usuarioLogado.getUid());
        } catch (Exception e) { e.printStackTrace(); }

        if (tentativasErro >= 3) {
            try {
                UsuarioDAO.bloquear(usuarioLogado.getUid());
                RegistroDAO.registrar(3007, usuarioLogado.getUid());
                RegistroDAO.registrar(3002, usuarioLogado.getUid());
            } catch (Exception e) { e.printStackTrace(); }
            JOptionPane.showMessageDialog(this,
                    "Acesso bloqueado por 2 minutos!", "Bloqueio", JOptionPane.ERROR_MESSAGE);
            dispose();
            new TelaLogin().setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Senha incorreta! Tentativa " + tentativasErro + " de 3.",
                    "Erro", JOptionPane.WARNING_MESSAGE);
            limparSenha();
        }
    }
}