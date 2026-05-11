package cofre.ui;

import cofre.crypto.CryptoManager;
import cofre.db.ChaveiroDAO;
import cofre.db.RegistroDAO;
import cofre.db.UsuarioDAO;
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
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class TelaConsulta extends JFrame {

    private Usuario usuario;
    private JTextField txtCaminhoPasta;
    private JPasswordField txtFraseSecreta;
    private JTable tabelaArquivos;
    private DefaultTableModel modeloTabela;

    // Guarda os dados do índice para usar quando selecionar um arquivo
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

        // -------------------------------------------------------
        // CABEÇALHO
        // -------------------------------------------------------
        JPanel cabecalho = new JPanel(new GridLayout(3, 1));
        cabecalho.setBorder(BorderFactory.createTitledBorder("Usuário Logado"));
        cabecalho.add(new JLabel("  Login: " + usuario.getEmail()));
        cabecalho.add(new JLabel("  Grupo: " +
                (usuario.getGid() == 1 ? "Administrador" : "Usuário")));
        cabecalho.add(new JLabel("  Nome: " + usuario.getNome()));
        add(cabecalho, BorderLayout.NORTH);

        // -------------------------------------------------------
        // CORPO 1 - total de consultas (CT do usuário)
        // -------------------------------------------------------
        JPanel corpo1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        corpo1.setBorder(BorderFactory.createTitledBorder("Consultas"));
        corpo1.add(new JLabel("Total de consultas do usuário: " + usuario.getCt()));

        // -------------------------------------------------------
        // CORPO 2 - formulário e tabela
        // -------------------------------------------------------
        JPanel corpo2 = new JPanel(new BorderLayout(5, 5));
        corpo2.setBorder(BorderFactory.createTitledBorder("Consulta de Arquivos Secretos"));

        // Formulário
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Caminho da pasta
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formulario.add(new JLabel("Caminho da pasta:"), gbc);

        txtCaminhoPasta = new JTextField(30);
        gbc.gridx = 1; gbc.weightx = 1.0;
        formulario.add(txtCaminhoPasta, gbc);

        JButton btnPasta = new JButton("...");
        btnPasta.addActionListener(e -> escolherPasta());
        gbc.gridx = 2; gbc.weightx = 0;
        formulario.add(btnPasta, gbc);

        // Frase secreta
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formulario.add(new JLabel("Frase secreta:"), gbc);

        txtFraseSecreta = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formulario.add(txtFraseSecreta, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0;

        // Botão Listar
        JButton btnListar = new JButton("Listar");
        btnListar.addActionListener(e -> listarArquivos());
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        formulario.add(btnListar, gbc);

        corpo2.add(formulario, BorderLayout.NORTH);

        // Tabela de arquivos
        String[] colunas = {"Nome Código", "Nome Secreto", "Dono", "Grupo"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tabelaArquivos = new JTable(modeloTabela);
        tabelaArquivos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Ao clicar duas vezes em um arquivo, tenta decriptar
        tabelaArquivos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tabelaArquivos.getSelectedRow();
                    if (row >= 0) {
                        decriptarArquivo(row);
                    }
                }
            }
        });

        corpo2.add(new JScrollPane(tabelaArquivos), BorderLayout.CENTER);

        // Botão Voltar
        JButton btnVoltar = new JButton("Voltar para o Menu Principal");
        btnVoltar.addActionListener(e -> {
            try {
                RegistroDAO.registrar(7002, usuario.getUid());
            } catch (Exception ex) {}
            dispose();
            new TelaPrincipal(usuario).setVisible(true);
        });
        corpo2.add(btnVoltar, BorderLayout.SOUTH);

        // Monta centro
        JPanel centro = new JPanel(new BorderLayout());
        centro.add(corpo1, BorderLayout.NORTH);
        centro.add(corpo2, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
    }

    private void escolherPasta() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Selecionar Pasta Segura");
        int resultado = fc.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            txtCaminhoPasta.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void listarArquivos() {
        String caminhoPasta = txtCaminhoPasta.getText().trim();
        String fraseSecreta = new String(txtFraseSecreta.getPassword());

        try {
            RegistroDAO.registrar(7003, usuario.getUid());
        } catch (Exception e) {}

        // Validação da pasta
        if (caminhoPasta.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Informe o caminho da pasta!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File pasta = new File(caminhoPasta);
        if (!pasta.exists() || !pasta.isDirectory()) {
            try { RegistroDAO.registrar(7004, usuario.getUid()); } catch (Exception e) {}
            JOptionPane.showMessageDialog(this,
                    "Pasta inválida!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Verifica se os arquivos do índice existem
        File indexEnc = new File(pasta, "index.enc");
        File indexEnv = new File(pasta, "index.env");
        File indexAsd = new File(pasta, "index.asd");

        if (!indexEnc.exists() || !indexEnv.exists() || !indexAsd.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Pasta não contém os arquivos de índice!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Busca a chave privada e certificado do admin no banco
            // O índice pertence ao administrador (UID=1)
            byte[] chavePrivadaBytes = ChaveiroDAO.buscarChavePrivada(1);
            String certPEM = ChaveiroDAO.buscarCertificado(1);

            if (chavePrivadaBytes == null || certPEM == null) {
                JOptionPane.showMessageDialog(this,
                        "Chave do administrador não encontrada no banco!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Valida a frase secreta restaurando a chave privada
            PrivateKey chavePrivada;
            try {
                chavePrivada = CryptoManager.restaurarChavePrivada(
                        chavePrivadaBytes, fraseSecreta);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Frase secreta inválida!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Lê o envelope (index.env) e decifra com a chave privada
            // para obter a semente
            byte[] envelopeBytes = Files.readAllBytes(indexEnv.toPath());
            byte[] semente = CryptoManager.decifrarEnvelope(envelopeBytes, chavePrivada);

            // Usa a semente com SHA1PRNG para gerar a chave AES
            java.security.SecureRandom sr =
                    java.security.SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(semente);
            javax.crypto.KeyGenerator kg =
                    javax.crypto.KeyGenerator.getInstance("AES");
            kg.init(256, sr);
            SecretKey chaveAES = kg.generateKey();

            // Decifra o index.enc com a chave AES
            byte[] indexEncBytes = Files.readAllBytes(indexEnc.toPath());
            byte[] indexBytes;
            try {
                indexBytes = CryptoManager.decifrarAES(indexEncBytes, chaveAES);
                RegistroDAO.registrar(7005, usuario.getUid());
            } catch (Exception e) {
                RegistroDAO.registrar(7007, usuario.getUid());
                JOptionPane.showMessageDialog(this,
                        "Falha na decriptação do arquivo de índice!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Verifica a assinatura digital do índice
            X509Certificate cert = CryptoManager.restaurarCertificado(certPEM);
            PublicKey chavePublica = cert.getPublicKey();
            byte[] assinaturaBytes = Files.readAllBytes(indexAsd.toPath());

            boolean assinaturaValida = CryptoManager.verificarAssinatura(
                    indexBytes, assinaturaBytes, chavePublica);

            if (!assinaturaValida) {
                RegistroDAO.registrar(7008, usuario.getUid());
                JOptionPane.showMessageDialog(this,
                        "Falha na verificação de integridade do índice!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            RegistroDAO.registrar(7006, usuario.getUid());

            // Lê o conteúdo do índice e filtra arquivos do usuário
            String conteudoIndex = new String(indexBytes, "ASCII");
            String[] linhas = conteudoIndex.split("\n");

            // Limpa a tabela
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

                // Mostra apenas arquivos do usuário ou do seu grupo
                if (dono.equals(usuario.getEmail()) ||
                        grupo.equals(usuario.getGid() == 1 ? "Administrador" : "Usuario")) {

                    modeloTabela.addRow(new Object[]{
                            nomeCodigo, nomeSecreto, dono, grupo});
                    dadosIndice[count] = new String[]{
                            nomeCodigo, nomeSecreto, dono, grupo};
                    count++;
                }
            }

            RegistroDAO.registrar(7009, usuario.getUid());

            if (count == 0) {
                JOptionPane.showMessageDialog(this,
                        "Nenhum arquivo encontrado para este usuário!",
                        "Aviso", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erro: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
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

        try {
            RegistroDAO.registrar(7010, usuario.getUid(), nomeCodigo);
        } catch (Exception e) {}

        // Verifica se o usuário é o dono do arquivo
        if (!dono.equals(usuario.getEmail())) {
            try { RegistroDAO.registrar(7012, usuario.getUid(), nomeSecreto); }
            catch (Exception e) {}
            JOptionPane.showMessageDialog(this,
                    "Você não tem permissão para acessar este arquivo!",
                    "Acesso Negado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            RegistroDAO.registrar(7011, usuario.getUid(), nomeSecreto);

            // Busca a chave privada do usuário dono do arquivo
            byte[] chavePrivadaBytes = ChaveiroDAO.buscarChavePrivada(usuario.getUid());
            PrivateKey chavePrivada = CryptoManager.restaurarChavePrivada(
                    chavePrivadaBytes, fraseSecreta);

            // Lê o envelope do arquivo e decifra para obter a semente
            File arquivoEnv = new File(caminhoPasta, nomeCodigo + ".env");
            byte[] envelopeBytes = Files.readAllBytes(arquivoEnv.toPath());
            byte[] semente = CryptoManager.decifrarEnvelope(envelopeBytes, chavePrivada);

            // Gera a chave AES com SHA1PRNG
            java.security.SecureRandom sr =
                    java.security.SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(semente);
            javax.crypto.KeyGenerator kg =
                    javax.crypto.KeyGenerator.getInstance("AES");
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
                JOptionPane.showMessageDialog(this,
                        "Falha na decriptação do arquivo!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Verifica a assinatura digital do arquivo
            String certPEM = ChaveiroDAO.buscarCertificado(usuario.getUid());
            X509Certificate cert = CryptoManager.restaurarCertificado(certPEM);
            File arquivoAsd = new File(caminhoPasta, nomeCodigo + ".asd");
            byte[] assinaturaBytes = Files.readAllBytes(arquivoAsd.toPath());

            boolean valido = CryptoManager.verificarAssinatura(
                    dadosDecriptados, assinaturaBytes, cert.getPublicKey());

            if (!valido) {
                RegistroDAO.registrar(7016, usuario.getUid(), nomeSecreto);
                JOptionPane.showMessageDialog(this,
                        "Falha na verificação de integridade do arquivo!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            RegistroDAO.registrar(7014, usuario.getUid(), nomeSecreto);

            // Salva o arquivo decriptado com o nome secreto
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(nomeSecreto));
            fc.setDialogTitle("Salvar arquivo decriptado");
            int resultado = fc.showSaveDialog(this);

            if (resultado == JFileChooser.APPROVE_OPTION) {
                FileOutputStream fos = new FileOutputStream(fc.getSelectedFile());
                fos.write(dadosDecriptados);
                fos.close();
                JOptionPane.showMessageDialog(this,
                        "Arquivo salvo com sucesso!",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Erro: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}