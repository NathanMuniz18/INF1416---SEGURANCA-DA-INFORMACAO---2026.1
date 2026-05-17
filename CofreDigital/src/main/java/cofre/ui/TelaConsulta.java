package cofre.ui;

import cofre.crypto.CryptoManager;
import cofre.db.ChaveiroDAO;
import cofre.db.RegistroDAO;
import cofre.model.Usuario;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class TelaConsulta extends JFrame {

    private Usuario usuario;
    private JTextField txtCaminhoPasta;
    private JPasswordField txtFraseSecreta;
    private JTable tabelaArquivos;
    private DefaultTableModel modeloTabela;
    private String[][] dadosIndice;

    public TelaConsulta(Usuario usuario) {
        this.usuario = usuario;

        setTitle("Cofre Digital - Consulta de Arquivos");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        try {
            RegistroDAO.registrar(7001, usuario.getUid());
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
        corpo1.setBorder(BorderFactory.createTitledBorder("Consultas"));
        corpo1.add(new JLabel("Total de consultas do usuário: " + usuario.getCt()));

        JPanel corpo2 = new JPanel(new BorderLayout(5, 5));
        corpo2.setBorder(BorderFactory.createTitledBorder("Consulta de Arquivos Secretos"));

        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formulario.add(new JLabel("Caminho da pasta:"), gbc);

        txtCaminhoPasta = new JTextField(30);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formulario.add(txtCaminhoPasta, gbc);

        JButton btnPasta = new JButton("...");
        btnPasta.addActionListener(e -> escolherPasta());
        gbc.gridx = 2; gbc.weightx = 0;
        formulario.add(btnPasta, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formulario.add(new JLabel("Frase secreta:"), gbc);

        txtFraseSecreta = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formulario.add(txtFraseSecreta, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0;

        JButton btnListar = new JButton("Listar");
        btnListar.addActionListener(e -> listarArquivos());
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        formulario.add(btnListar, gbc);

        corpo2.add(formulario, BorderLayout.NORTH);

        String[] colunas = {"Nome Código", "Nome Secreto", "Dono", "Grupo"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tabelaArquivos = new JTable(modeloTabela);
        tabelaArquivos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tabelaArquivos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tabelaArquivos.getSelectedRow();
                    if (row >= 0) decriptarArquivo(row);
                }
            }
        });

        corpo2.add(new JScrollPane(tabelaArquivos), BorderLayout.CENTER);

        JButton btnVoltar = new JButton("Voltar para o Menu Principal");
        btnVoltar.addActionListener(e -> {
            try { RegistroDAO.registrar(7002, usuario.getUid()); } catch (Exception ex) {}
            dispose();
            new TelaPrincipal(usuario).setVisible(true);
        });
        corpo2.add(btnVoltar, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new BorderLayout());
        centro.add(corpo1, BorderLayout.NORTH);
        centro.add(corpo2, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }

    private void escolherPasta() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int resultado = fc.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            txtCaminhoPasta.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void listarArquivos() {
        String caminhoPasta = txtCaminhoPasta.getText().trim();
        String fraseSecreta = new String(txtFraseSecreta.getPassword());

        try { RegistroDAO.registrar(7003, usuario.getUid()); } catch (Exception e) {}

        if (caminhoPasta.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o caminho da pasta!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File pasta = new File(caminhoPasta);
        if (!pasta.exists() || !pasta.isDirectory()) {
            try { RegistroDAO.registrar(7004, usuario.getUid()); } catch (Exception e) {}
            JOptionPane.showMessageDialog(this, "Pasta inválida!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File indexEnc = new File(pasta, "index.enc");
        File indexEnv = new File(pasta, "index.env");
        File indexAsd = new File(pasta, "index.asd");

        if (!indexEnc.exists() || indexEnc.length() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Arquivo index.enc inválido ou corrompido!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!indexEnv.exists() || indexEnv.length() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Arquivo index.env inválido ou corrompido!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!indexAsd.exists() || indexAsd.length() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Arquivo index.asd inválido ou corrompido!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Valida a frase secreta do usuário logado
            byte[] chaveUsuarioBytes = ChaveiroDAO.buscarChavePrivada(usuario.getUid());
            try {
                CryptoManager.restaurarChavePrivada(chaveUsuarioBytes, fraseSecreta);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Frase secreta inválida!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Busca chave e certificado do ADMIN (UID=1) para decifrar o índice
            byte[] chaveAdminBytes = ChaveiroDAO.buscarChavePrivada(1);
            String certAdminPEM = ChaveiroDAO.buscarCertificado(1);

            if (chaveAdminBytes == null || certAdminPEM == null) {
                JOptionPane.showMessageDialog(this, "Chave do administrador não encontrada!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Restaura chave privada do admin com a frase em memória
            PrivateKey chavePrivadaAdmin;
            try {
                chavePrivadaAdmin = CryptoManager.restaurarChavePrivada(
                        chaveAdminBytes, cofre.SessaoSistema.getFraseSecretaAdmin());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erro ao acessar chave do administrador!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Decifra o envelope do índice com a chave privada do admin
            byte[] envelopeBytes = Files.readAllBytes(indexEnv.toPath());
            byte[] semente = CryptoManager.decifrarEnvelope(envelopeBytes, chavePrivadaAdmin);

            // Gera chave AES com SHA1PRNG a partir da semente
            java.security.SecureRandom sr = java.security.SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(semente);
            javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance("AES");
            kg.init(256, sr);
            SecretKey chaveAES = kg.generateKey();

            // Decifra o index.enc
            byte[] indexEncBytes = Files.readAllBytes(indexEnc.toPath());
            byte[] indexBytes;
            try {
                indexBytes = CryptoManager.decifrarAES(indexEncBytes, chaveAES);
                RegistroDAO.registrar(7005, usuario.getUid());
            } catch (Exception e) {
                RegistroDAO.registrar(7007, usuario.getUid());
                JOptionPane.showMessageDialog(this, "Falha na decriptação do arquivo de índice!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Verifica assinatura do índice com chave pública do admin
            X509Certificate certAdmin = CryptoManager.restaurarCertificado(certAdminPEM);
            PublicKey chavePublicaAdmin = certAdmin.getPublicKey();
            byte[] assinaturaBytes = Files.readAllBytes(indexAsd.toPath());

            if (!CryptoManager.verificarAssinatura(indexBytes, assinaturaBytes, chavePublicaAdmin)) {
                RegistroDAO.registrar(7008, usuario.getUid());
                JOptionPane.showMessageDialog(this, "Falha na verificação de integridade do índice!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            RegistroDAO.registrar(7006, usuario.getUid());

            // Lê e filtra o conteúdo do índice
            String conteudoIndex = new String(indexBytes, "ASCII");
            String[] linhas = conteudoIndex.split("\n");

            modeloTabela.setRowCount(0);
            dadosIndice = new String[linhas.length][4];

            int count = 0;
            for (String linha : linhas) {
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.trim().split(" ");
                if (partes.length < 4) continue;

                String nomeCodigo = partes[0];
                String nomeSecreto = partes[1];
                String dono = partes[2];
                String grupo = partes[3];

                if (dono.equals(usuario.getEmail()) ||
                        grupo.equals(usuario.getGid() == 1 ? "Administrador" : "Usuario")) {
                    modeloTabela.addRow(new Object[]{nomeCodigo, nomeSecreto, dono, grupo});
                    dadosIndice[count] = new String[]{nomeCodigo, nomeSecreto, dono, grupo};
                    count++;
                }
            }

            RegistroDAO.registrar(7009, usuario.getUid());

            if (count == 0) {
                JOptionPane.showMessageDialog(this, "Nenhum arquivo encontrado para este usuário!", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void decriptarArquivo(int row) {
        if (dadosIndice == null || dadosIndice[row] == null) return;

        String nomeCodigo = dadosIndice[row][0];
        String nomeSecreto = dadosIndice[row][1];
        String dono = dadosIndice[row][2];
        String caminhoPasta = txtCaminhoPasta.getText().trim();
        String fraseSecreta = new String(txtFraseSecreta.getPassword());

        try { RegistroDAO.registrar(7010, usuario.getUid(), nomeCodigo); } catch (Exception e) {}

        // Verifica se o usuário é o dono
        if (!dono.equals(usuario.getEmail())) {
            try { RegistroDAO.registrar(7012, usuario.getUid(), nomeSecreto); } catch (Exception e) {}
            JOptionPane.showMessageDialog(this, "Você não tem permissão para acessar este arquivo!", "Acesso Negado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            RegistroDAO.registrar(7011, usuario.getUid(), nomeSecreto);

            // Busca chave privada do USUÁRIO LOGADO (dono do arquivo)
            byte[] chavePrivadaBytes = ChaveiroDAO.buscarChavePrivada(usuario.getUid());
            PrivateKey chavePrivada = CryptoManager.restaurarChavePrivada(
                    chavePrivadaBytes, fraseSecreta);

            // Decifra o envelope do arquivo com a chave privada do usuário
            File arquivoEnv = new File(caminhoPasta, nomeCodigo + ".env");
            byte[] envelopeBytes = Files.readAllBytes(arquivoEnv.toPath());
            byte[] semente = CryptoManager.decifrarEnvelope(envelopeBytes, chavePrivada);

            // Gera chave AES com SHA1PRNG
            java.security.SecureRandom sr = java.security.SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(semente);
            javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance("AES");
            kg.init(256, sr);
            SecretKey chaveAES = kg.generateKey();

            // Decifra o arquivo .enc
            File arquivoEnc = new File(caminhoPasta, nomeCodigo + ".enc");
            byte[] encBytes = Files.readAllBytes(arquivoEnc.toPath());
            byte[] dadosDecriptados;
            try {
                dadosDecriptados = CryptoManager.decifrarAES(encBytes, chaveAES);
                RegistroDAO.registrar(7013, usuario.getUid(), nomeSecreto);
            } catch (Exception e) {
                RegistroDAO.registrar(7015, usuario.getUid(), nomeSecreto);
                JOptionPane.showMessageDialog(this, "Falha na decriptação do arquivo!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Verifica assinatura com chave pública do USUÁRIO LOGADO
            String certPEM = ChaveiroDAO.buscarCertificado(usuario.getUid());
            X509Certificate cert = CryptoManager.restaurarCertificado(certPEM);
            File arquivoAsd = new File(caminhoPasta, nomeCodigo + ".asd");
            byte[] assinaturaBytes = Files.readAllBytes(arquivoAsd.toPath());

            if (!CryptoManager.verificarAssinatura(dadosDecriptados, assinaturaBytes, cert.getPublicKey())) {
                RegistroDAO.registrar(7016, usuario.getUid(), nomeSecreto);
                JOptionPane.showMessageDialog(this, "Falha na verificação de integridade do arquivo!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            RegistroDAO.registrar(7014, usuario.getUid(), nomeSecreto);

            // Salva o arquivo decriptado
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(nomeSecreto));
            int resultado = fc.showSaveDialog(this);
            if (resultado == JFileChooser.APPROVE_OPTION) {
                FileOutputStream fos = new FileOutputStream(fc.getSelectedFile());
                fos.write(dadosDecriptados);
                fos.close();
                JOptionPane.showMessageDialog(this, "Arquivo salvo com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}