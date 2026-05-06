package cofre.model;

public class Usuario {

    // Identificador único do usuário no banco
    private int uid;

    // Nome extraído do certificado digital
    private String nome;

    // E-mail extraído do certificado (usado como login name)
    private String email;

    // Senha pessoal armazenada com BCrypt
    private String hash;

    // Chave TOTP cifrada com AES, codificada em BASE32
    private String tokenKey;

    // Referência ao certificado/chave na tabela Chaveiro
    private int keyId;

    // Grupo: 1 = Administrador, 2 = Usuário
    private int gid;

    // Contador de acessos ao sistema
    private int ct;

    // Bloqueio: 0 = livre, 1 = bloqueado
    private int blk;

    // Data/hora em que o bloqueio foi aplicado
    private String blkTime;

    // Construtor vazio
    public Usuario() {}

    // Getters e Setters
    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getTokenKey() { return tokenKey; }
    public void setTokenKey(String tokenKey) { this.tokenKey = tokenKey; }

    public int getKeyId() { return keyId; }
    public void setKeyId(int keyId) { this.keyId = keyId; }

    public int getGid() { return gid; }
    public void setGid(int gid) { this.gid = gid; }

    public int getCt() { return ct; }
    public void setCt(int ct) { this.ct = ct; }

    public int getBlk() { return blk; }
    public void setBlk(int blk) { this.blk = blk; }

    public String getBlkTime() { return blkTime; }
    public void setBlkTime(String blkTime) { this.blkTime = blkTime; }

    // Retorna true se o usuário é administrador
    public boolean isAdmin() { return gid == 1; }
}