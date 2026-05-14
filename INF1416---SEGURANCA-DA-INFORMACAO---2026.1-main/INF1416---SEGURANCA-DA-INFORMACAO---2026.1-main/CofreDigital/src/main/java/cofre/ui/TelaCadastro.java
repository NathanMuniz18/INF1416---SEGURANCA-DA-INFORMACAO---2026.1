package cofre.ui;
import cofre.SessaoSistema;
import cofre.ui.TelaLogin;
import cofre.crypto.BCryptUtil;
import cofre.crypto.CryptoManager;
import cofre.db.ChaveiroDAO;
import cofre.db.RegistroDAO;
import cofre.db.UsuarioDAO;
import cofre.model.Usuario;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelaCadastro extends JFrame {

    // null = primeira execução (cadastro do admin)
    // preenchido = admin logado cadastrando outro usuário
    private Usuario adminLogado;

    // Guarda o arquivo selecionado pelo usuário
    private File arquivoCertificado;
    private File arquivoChave;

    // Labels que mostram o nome do arquivo selecionado
    private JLabel lblCertificado;
    private JLabel lblChave;

    private JPasswordField txtFraseSecreta;
    private JComboBox<String> cbGrupo;
    private JPasswordField txtSenha;
    private JPasswordField txtConfirmaSenha;

    /**
     * @param adminLogado null se for o primeiro cadastro do admin,
     *                    ou o objeto Usuario do admin logado
     */
    public TelaCadastro(Usuario adminLogado) {
        this.adminLogado = adminLogado;

        setTitle("Cofre Digital - Cadastro");
        setSize(620, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        try {
            if (adminLogado != null && adminLogado.getUid() > 0) {
                RegistroDAO.registrar(6001, adminLogado.getUid());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        inicializarComponentes();
    }

    private void inicializarComponentes() {

        // -------------------------------------------------------
        // CABEÇALHO
        // Se for primeira execução, mostra traços
        // Se admin já logado, mostra seus dados
        // -------------------------------------------------------
        JPanel painelCabecalho = new JPanel(new GridLayout(3, 1));
        painelCabecalho.setBorder(BorderFactory.createTitledBorder("Dados do Usuário"));

        boolean primeiroAdmin = (adminLogado == null || adminLogado.getUid() == 0);

        painelCabecalho.add(new JLabel("Login: " +
                (primeiroAdmin ? "-" : adminLogado.getEmail())));
        painelCabecalho.add(new JLabel("Grupo: " +
                (primeiroAdmin ? "-" : (adminLogado.getGid() == 1 ? "Administrador" : "Usuário"))));
        painelCabecalho.add(new JLabel("Nome: " +
                (primeiroAdmin ? "-" : adminLogado.getNome())));

        add(painelCabecalho, BorderLayout.NORTH);

        // -------------------------------------------------------
        // CORPO 1 - total de usuários
        // -------------------------------------------------------
        JPanel painelCorpo1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelCorpo1.setBorder(BorderFactory.createTitledBorder("Sistema"));
        int totalUsuarios = 0;
        try {
            totalUsuarios = UsuarioDAO.totalUsuarios();
        } catch (Exception e) {
            e.printStackTrace();
        }
        painelCorpo1.add(new JLabel("Total de usuários no sistema: " + totalUsuarios));

        // -------------------------------------------------------
        // CORPO 2 - formulário
        // -------------------------------------------------------
        JPanel painelCorpo2 = new JPanel(new GridBagLayout());
        painelCorpo2.setBorder(BorderFactory.createTitledBorder("Formulário de Cadastro"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // --- Certificado digital ---
        gbc.gridx = 0; gbc.gridy = 0;
        painelCorpo2.add(new JLabel("Certificado digital (.crt):"), gbc);

        // Label que mostra o nome do arquivo selecionado
        lblCertificado = new JLabel("Nenhum arquivo selecionado");
        lblCertificado.setForeground(Color.GRAY);
        gbc.gridx = 1; gbc.weightx = 1.0;
        painelCorpo2.add(lblCertificado, gbc);

        // Botão para abrir o explorador de arquivos
        JButton btnCertificado = new JButton("Escolher arquivo...");
        btnCertificado.addActionListener(e -> escolherArquivo("crt"));
        gbc.gridx = 2; gbc.weightx = 0;
        painelCorpo2.add(btnCertificado, gbc);

        // --- Chave privada ---
        gbc.gridx = 0; gbc.gridy = 1;
        painelCorpo2.add(new JLabel("Chave privada (.key):"), gbc);

        lblChave = new JLabel("Nenhum arquivo selecionado");
        lblChave.setForeground(Color.GRAY);
        gbc.gridx = 1; gbc.weightx = 1.0;
        painelCorpo2.add(lblChave, gbc);

        JButton btnChave = new JButton("Escolher arquivo...");
        btnChave.addActionListener(e -> escolherArquivo("key"));
        gbc.gridx = 2; gbc.weightx = 0;
        painelCorpo2.add(btnChave, gbc);

        // --- Frase secreta ---
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        painelCorpo2.add(new JLabel("Frase secreta:"), gbc);
        txtFraseSecreta = new JPasswordField(20);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        painelCorpo2.add(txtFraseSecreta, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0;

        // --- Grupo ---
        gbc.gridx = 0; gbc.gridy = 3;
        painelCorpo2.add(new JLabel("Grupo:"), gbc);

        // Se for o primeiro cadastro, só pode ser Administrador
        if (primeiroAdmin) {
            cbGrupo = new JComboBox<>(new String[]{"Administrador"});
            cbGrupo.setEnabled(false);
        } else {
            cbGrupo = new JComboBox<>(new String[]{"Administrador", "Usuário"});
        }
        gbc.gridx = 1; gbc.gridwidth = 2;
        painelCorpo2.add(cbGrupo, gbc);
        gbc.gridwidth = 1;

        // --- Senha pessoal ---
        gbc.gridx = 0; gbc.gridy = 4;
        painelCorpo2.add(new JLabel("Senha pessoal (8-10 dígitos):"), gbc);
        txtSenha = new JPasswordField(10);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        painelCorpo2.add(txtSenha, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0;

        // --- Confirmação de senha ---
        gbc.gridx = 0; gbc.gridy = 5;
        painelCorpo2.add(new JLabel("Confirmação da senha:"), gbc);
        txtConfirmaSenha = new JPasswordField(10);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        painelCorpo2.add(txtConfirmaSenha, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0;

        // --- Botões ---
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnCadastrar = new JButton("Cadastrar");
        JButton btnVoltar = new JButton("Voltar para o Menu Principal");

        // Esconde o botão Voltar no primeiro cadastro
        if (primeiroAdmin) btnVoltar.setVisible(false);

        btnCadastrar.addActionListener(e -> {
            try {
                if (adminLogado != null && adminLogado.getUid() > 0) {
                    RegistroDAO.registrar(6002, adminLogado.getUid());
                }
            } catch (Exception ex) {}
            realizarProcessoCadastro();
        });

        btnVoltar.addActionListener(e -> {
            try {
                RegistroDAO.registrar(6010, adminLogado.getUid());
            } catch (Exception ex) {}
            dispose();
            new TelaLogin().setVisible(true);
        });

        painelBotoes.add(btnCadastrar);
        painelBotoes.add(btnVoltar);

        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        painelCorpo2.add(painelBotoes, gbc);

        // Monta o centro
        JPanel painelCentro = new JPanel(new BorderLayout());
        painelCentro.add(painelCorpo1, BorderLayout.NORTH);
        painelCentro.add(painelCorpo2, BorderLayout.CENTER);
        add(painelCentro, BorderLayout.CENTER);
    }

    /**
     * Abre o explorador de arquivos para selecionar .crt ou .key
     */
    private void escolherArquivo(String tipo) {
        JFileChooser fc = new JFileChooser();

        if (tipo.equals("crt")) {
            fc.setDialogTitle("Selecionar Certificado Digital");
            fc.setFileFilter(new FileNameExtensionFilter(
                    "Certificado Digital (*.crt)", "crt"));
        } else {
            fc.setDialogTitle("Selecionar Chave Privada");
            fc.setFileFilter(new FileNameExtensionFilter(
                    "Chave Privada (*.key)", "key"));
        }

        int resultado = fc.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File arquivoSelecionado = fc.getSelectedFile();
            if (tipo.equals("crt")) {
                arquivoCertificado = arquivoSelecionado;
                // Mostra só o nome do arquivo, não o caminho todo
                lblCertificado.setText(arquivoSelecionado.getName());
                lblCertificado.setForeground(Color.BLACK);
            } else {
                arquivoChave = arquivoSelecionado;
                lblChave.setText(arquivoSelecionado.getName());
                lblChave.setForeground(Color.BLACK);
            }
        }
    }

    private void realizarProcessoCadastro() {
        String senha = new String(txtSenha.getPassword());
        String confirmaSenha = new String(txtConfirmaSenha.getPassword());
        String frase = new String(txtFraseSecreta.getPassword());
        int grupoSelecionado = cbGrupo.getSelectedIndex() + 1;

        // Validação 1 - arquivos selecionados
        if (arquivoCertificado == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione o arquivo do certificado digital!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (arquivoChave == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione o arquivo da chave privada!",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validação 2 - senha pessoal
        if (!validarSenha(senha, confirmaSenha)) {
            try {
                if (adminLogado != null && adminLogado.getUid() > 0)
                    RegistroDAO.registrar(6003, adminLogado.getUid());
            } catch (Exception e) {}
            JOptionPane.showMessageDialog(this,
                    "Senha inválida!\n" +
                            "- Deve ter 8 a 10 dígitos numéricos\n" +
                            "- Não pode ter dígitos repetidos\n" +
                            "- As senhas devem coincidir",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Validação 3 - lê o certificado
            String certPEM;
            X509Certificate certificado;
            try {
                certPEM = CryptoManager.lerCertificadoDoArquivo(
                        arquivoCertificado.getAbsolutePath());
                certificado = CryptoManager.restaurarCertificado(certPEM);
            } catch (Exception e) {
                if (adminLogado != null && adminLogado.getUid() > 0)
                    RegistroDAO.registrar(6004, adminLogado.getUid());
                JOptionPane.showMessageDialog(this,
                        "Certificado digital inválido!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Extrai nome e e-mail do certificado
            String dn = certificado.getSubjectDN().getName();
            String nome = extrairDoDN(dn, "CN");
            String email = extrairDoDN(dn, "EMAILADDRESS");
            if (email.equals("Não encontrado"))
                email = extrairDoDN(dn, "E");

            // Validação 4 - lê e valida a chave privada
            byte[] bytesChave;
            PrivateKey chavePrivada;
            try {
                // Lê os bytes cifrados do arquivo
                bytesChave = CryptoManager.lerChavePrivadaDoArquivo(
                        arquivoChave.getAbsolutePath(), frase);

                // Decifra os bytes para guardar no banco já decifrados
                javax.crypto.SecretKey chaveAESArquivo = CryptoManager.gerarChaveAES(frase);
                byte[] bytesDecifrados = CryptoManager.decifrarAES(bytesChave, chaveAESArquivo);

                // Restaura a chave privada para validação
                chavePrivada = CryptoManager.restaurarChavePrivada(bytesChave, frase);
                System.out.println("Tamanho bytes salvos no banco: " + bytesChave.length);
            } catch (Exception e) {
                if (adminLogado != null && adminLogado.getUid() > 0)
                    RegistroDAO.registrar(6006, adminLogado.getUid());
                JOptionPane.showMessageDialog(this,
                        "Frase secreta incorreta ou chave privada inválida!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validação 5 - verifica assinatura com 9216 bytes
            byte[] dados = new byte[9216];
            new SecureRandom().nextBytes(dados);
            byte[] assinatura = CryptoManager.assinar(dados, chavePrivada);
            if (!CryptoManager.verificarAssinatura(
                    dados, assinatura, certificado.getPublicKey())) {
                if (adminLogado != null && adminLogado.getUid() > 0)
                    RegistroDAO.registrar(6007, adminLogado.getUid());
                JOptionPane.showMessageDialog(this,
                        "Assinatura digital inválida!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Tela de confirmação dos dados do certificado
            String msgConfirmacao = String.format(
                    "Versão: %d\n" +
                            "Série: %s\n" +
                            "Validade: %s até %s\n" +
                            "Tipo de Assinatura: %s\n" +
                            "Emissor: %s\n" +
                            "Sujeito: %s\n" +
                            "E-mail: %s\n\n" +
                            "Confirma o cadastro?",
                    certificado.getVersion(),
                    certificado.getSerialNumber().toString(),
                    certificado.getNotBefore(),
                    certificado.getNotAfter(),
                    certificado.getSigAlgName(),
                    certificado.getIssuerDN().getName(),
                    nome, email
            );

            int opcao = JOptionPane.showConfirmDialog(this,
                    msgConfirmacao, "Confirmar Dados do Certificado",
                    JOptionPane.YES_NO_OPTION);

            if (opcao != JOptionPane.YES_OPTION) {
                if (adminLogado != null && adminLogado.getUid() > 0)
                    RegistroDAO.registrar(6009, adminLogado.getUid());
                return;
            }
            if (adminLogado != null && adminLogado.getUid() > 0)
                RegistroDAO.registrar(6008, adminLogado.getUid());

            // Verifica se e-mail já existe no banco
            java.sql.ResultSet rs = UsuarioDAO.buscarPorEmail(email);
            if (rs.next()) {
                JOptionPane.showMessageDialog(this,
                        "Este e-mail já está cadastrado no sistema!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                rs.close();
                return;
            }
            rs.close();

            // Gera hash BCrypt da senha pessoal
            String hash = BCryptUtil.gerarHash(senha);

            // Gera chave TOTP de 20 bytes aleatórios
            byte[] tokenKeyBytes = new byte[20];
            new SecureRandom().nextBytes(tokenKeyBytes);

            // Codifica a chave TOTP em BASE32
            cofre.util.Base32 base32 = new cofre.util.Base32(
                    cofre.util.Base32.Alphabet.BASE32, false, false);
            String tokenKeyBase32 = base32.toString(tokenKeyBytes);

            // Cifra a chave TOTP com AES gerado da senha pessoal
            javax.crypto.SecretKey chaveAES = CryptoManager.gerarChaveAES(senha);
            byte[] tokenKeyCifrado = CryptoManager.cifrarAES(
                    tokenKeyBase32.getBytes("UTF-8"), chaveAES);
            String tokenKeyCifradoBase64 = java.util.Base64.getEncoder()
                    .encodeToString(tokenKeyCifrado);

            // Insere usuário no banco
            int uid = UsuarioDAO.inserir(nome, email, hash,
                    tokenKeyCifradoBase64, grupoSelecionado);

            // Insere certificado e chave no Chaveiro
            int kid = ChaveiroDAO.inserir(uid, certPEM, bytesChave);

            // Atualiza KEYID do usuário
            UsuarioDAO.atualizarKeyId(uid, kid);
            // Monta a URI do Google Authenticator
            String uri = "otpauth://totp/Cofre%20Digital:" + email + "?secret=" + tokenKeyBase32;

// Gera o QRCode
            com.google.zxing.qrcode.QRCodeWriter qrWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrWriter.encode(
                    uri, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);
            java.awt.image.BufferedImage qrImage = com.google.zxing.client.j2se.MatrixToImageWriter
                    .toBufferedImage(bitMatrix);

// Painel com QRCode e campos copiáveis
            JPanel painelTotp = new JPanel(new BorderLayout(10, 10));

// QRCode no topo
            JLabel lblQR = new JLabel(new ImageIcon(qrImage));
            lblQR.setHorizontalAlignment(SwingConstants.CENTER);
            painelTotp.add(lblQR, BorderLayout.NORTH);

// Campos copiáveis no centro
            JPanel painelCampos = new JPanel(new GridLayout(4, 1, 5, 5));
            painelCampos.add(new JLabel("Usuário cadastrado! Escaneie o QRCode ou copie o segredo:"));

            JTextField campoSegredo = new JTextField(tokenKeyBase32);
            campoSegredo.setEditable(false);
            campoSegredo.setFont(new Font("Monospaced", Font.BOLD, 12));
            painelCampos.add(new JLabel("Segredo BASE32:"));
            painelCampos.add(campoSegredo);

            JTextField campoURI = new JTextField(uri);
            campoURI.setEditable(false);
            painelCampos.add(campoURI);

            painelTotp.add(painelCampos, BorderLayout.CENTER);

            JOptionPane.showMessageDialog(this,
                    painelTotp,
                    "Cadastro Realizado",
                    JOptionPane.INFORMATION_MESSAGE);

            JOptionPane.showMessageDialog(this,
                    painelTotp,
                    "Cadastro Realizado",
                    JOptionPane.INFORMATION_MESSAGE);

            // Limpa o formulário
            arquivoCertificado = null;
            arquivoChave = null;
            lblCertificado.setText("Nenhum arquivo selecionado");
            lblCertificado.setForeground(Color.GRAY);
            lblChave.setText("Nenhum arquivo selecionado");
            lblChave.setForeground(Color.GRAY);
            txtFraseSecreta.setText("");
            txtSenha.setText("");
            txtConfirmaSenha.setText("");

            // Se for o primeiro admin vai para a tela de login
            boolean primeiroAdmin = (adminLogado == null || adminLogado.getUid() == 0);
            if (primeiroAdmin) {
                // Salva a frase do admin em memória
                SessaoSistema.setFraseSecretaAdmin(frase);
                dispose();
                new TelaLogin().setVisible(true);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro inesperado: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private boolean validarSenha(String senha, String confirmaSenha) {
        if (!senha.equals(confirmaSenha)) return false;
        if (!senha.matches("\\d{8,10}")) return false;

        // Verifica se há dígitos repetidos em qualquer posição
        for (int i = 0; i < senha.length(); i++) {
            for (int j = i + 1; j < senha.length(); j++) {
                if (senha.charAt(i) == senha.charAt(j)) return false;
            }
        }
        return true;
    }

    private String extrairDoDN(String dn, String campo) {
        Pattern pattern = Pattern.compile(campo + "=([^,]+)");
        Matcher matcher = pattern.matcher(dn);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Não encontrado";
    }
}