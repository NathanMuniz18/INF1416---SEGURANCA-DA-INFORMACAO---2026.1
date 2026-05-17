//Nathan 2212759
//Hanna 2310289


package cofre.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {

    // Endereço do banco de dados SQLite
    // O arquivo cofre.db será criado automaticamente na pasta do projeto
    private static final String DB_URL = "jdbc:sqlite:cofre.db";

    private static Connection connection = null;

    public static Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    public static void inicializar() throws Exception {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();


        stmt.execute("""
            CREATE TABLE IF NOT EXISTS Grupos (
                GID  INTEGER PRIMARY KEY AUTOINCREMENT,
                NOME TEXT NOT NULL UNIQUE
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS Usuarios (
                UID       INTEGER PRIMARY KEY AUTOINCREMENT,
                NOME      TEXT NOT NULL,
                EMAIL     TEXT NOT NULL UNIQUE,
                HASH      TEXT NOT NULL,
                TOKEN_KEY TEXT NOT NULL,
                KEYID     INTEGER,
                GID       INTEGER NOT NULL,
                CT        INTEGER DEFAULT 0,
                BLK       INTEGER DEFAULT 0,
                BLK_TIME  TEXT,
                FOREIGN KEY (GID) REFERENCES Grupos(GID)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS Chaveiro (
                KID           INTEGER PRIMARY KEY AUTOINCREMENT,
                UID           INTEGER NOT NULL,
                CERTIFICADO   TEXT NOT NULL,
                CHAVE_PRIVADA BLOB NOT NULL,
                FOREIGN KEY (UID) REFERENCES Usuarios(UID)
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS Mensagens (
                MID   INTEGER PRIMARY KEY,
                TEXTO TEXT NOT NULL
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS Registros (
                RID       INTEGER PRIMARY KEY AUTOINCREMENT,
                DATA_HORA TEXT NOT NULL,
                MID       INTEGER NOT NULL,
                UID       INTEGER,
                ARQ_NOME  TEXT,
                FOREIGN KEY (MID) REFERENCES Mensagens(MID),
                FOREIGN KEY (UID) REFERENCES Usuarios(UID)
            )
        """);

        stmt.close();

        popularMensagens(conn);
        popularGrupos(conn);
    }

    private static void popularGrupos(Connection conn) throws Exception {
        Statement stmt = conn.createStatement();
        stmt.execute("INSERT OR IGNORE INTO Grupos (GID, NOME) VALUES (1, 'Administrador')");
        stmt.execute("INSERT OR IGNORE INTO Grupos (GID, NOME) VALUES (2, 'Usuario')");
        stmt.close();
    }

    private static void popularMensagens(Connection conn) throws Exception {
        String[] mensagens = {
                "INSERT OR IGNORE INTO Mensagens VALUES (1001, 'Sistema iniciado.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (1002, 'Sistema encerrado.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (1003, 'Sessão iniciada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (1004, 'Sessão encerrada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (1005, 'Partida do sistema iniciada para cadastro do administrador.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (1006, 'Partida do sistema iniciada para operação normal pelos usuários.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (2001, 'Autenticação etapa 1 iniciada.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (2002, 'Autenticação etapa 1 encerrada.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (2003, 'Login name {0} identificado com acesso liberado.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (2004, 'Login name {0} identificado com acesso bloqueado.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (2005, 'Login name {0} não identificado.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (3001, 'Autenticação etapa 2 iniciada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (3002, 'Autenticação etapa 2 encerrada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (3003, 'Senha pessoal verificada positivamente para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (3004, 'Primeiro erro da senha pessoal contabilizado para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (3005, 'Segundo erro da senha pessoal contabilizado para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (3006, 'Terceiro erro da senha pessoal contabilizado para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (3007, 'Acesso do usuario {0} bloqueado pela autenticação etapa 2.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (4001, 'Autenticação etapa 3 iniciada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (4002, 'Autenticação etapa 3 encerrada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (4003, 'Token verificado positivamente para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (4004, 'Primeiro erro de token contabilizado para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (4005, 'Segundo erro de token contabilizado para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (4006, 'Terceiro erro de token contabilizado para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (4007, 'Acesso do usuario {0} bloqueado pela autenticação etapa 3.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (5001, 'Tela principal apresentada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (5002, 'Opção 1 do menu principal selecionada por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (5003, 'Opção 2 do menu principal selecionada por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (5004, 'Opção 3 do menu principal selecionada por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6001, 'Tela de cadastro apresentada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6002, 'Botão cadastrar pressionado por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6003, 'Senha pessoal inválida fornecida por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6004, 'Caminho do certificado digital inválido fornecido por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6005, 'Chave privada verificada negativamente para {0} (caminho inválido).')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6006, 'Chave privada verificada negativamente para {0} (frase secreta inválida).')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6007, 'Chave privada verificada negativamente para {0} (assinatura digital inválida).')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6008, 'Confirmação de dados aceita por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6009, 'Confirmação de dados rejeitada por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (6010, 'Botão voltar de cadastro para o menu principal pressionado por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7001, 'Tela de consulta de arquivos secretos apresentada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7002, 'Botão voltar de consulta para o menu principal pressionado por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7003, 'Botão Listar de consulta pressionado por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7004, 'Caminho de pasta inválido fornecido por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7005, 'Arquivo de índice decriptado com sucesso para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7006, 'Arquivo de índice verificado (integridade e autenticidade) com sucesso para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7007, 'Falha na decriptação do arquivo de índice para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7008, 'Falha na verificação (integridade e autenticidade) do arquivo de índice para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7009, 'Lista de arquivos presentes no índice apresentada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7010, 'Arquivo {1} selecionado por {0} para decriptação.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7011, 'Acesso permitido ao arquivo {1} para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7012, 'Acesso negado ao arquivo {1} para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7013, 'Arquivo {1} decriptado com sucesso para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7014, 'Arquivo {1} verificado (integridade e autenticidade) com sucesso para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7015, 'Falha na decriptação do arquivo {1} para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (7016, 'Falha na verificação (integridade e autenticidade) do arquivo {1} para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (8001, 'Tela de saída apresentada para {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (8002, 'Botão encerrar sessão pressionado por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (8003, 'Botão encerrar sistema pressionado por {0}.')",
                "INSERT OR IGNORE INTO Mensagens VALUES (8004, 'Botão voltar de sair para o menu principal pressionado por {0}.')"
        };

        Statement stmt = conn.createStatement();
        for (String sql : mensagens) {
            stmt.execute(sql);
        }
        stmt.close();
    }
}