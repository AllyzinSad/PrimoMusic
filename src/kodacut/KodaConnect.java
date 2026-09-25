package kodacut;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public final class KodaConnect extends JFrame {
    private static final Color BG = new Color(10,10,10);
    private static final Color PANEL = new Color(22,22,22);
    private static final Color FIELD = new Color(34,34,34);
    private static final Color FG = new Color(246,246,246);
    private static final Color MUTED = new Color(160,160,160);
    private static final Color GOLD = new Color(212,175,55);

    private final Path root;
    private KodaConnectServer server;

    private final JLabel serverStatus = new JLabel("Inicializando...");
    private final JLabel connectionStatus = new JLabel("Ainda não conectado a um assistente externo");
    private final JTextArea allowedFolders = new JTextArea();
    private final JTextArea activity = new JTextArea();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KodaConnect app = new KodaConnect();
            app.setVisible(true);
        });
    }

    public KodaConnect() {
        root = detectRoot();
        ensureFolders();
        setTitle("Koda Connect v0.5");
        setIconImage(KodaIcon.createIcon(256));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setSize(1120, 760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loadAllowedFolders();
        startServer();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                if (server != null) server.stop();
            }
        });
    }

    private Path detectRoot() {
        try {
            Path code = Paths.get(KodaConnect.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
            if (Files.isRegularFile(code)) {
                Path p = code.getParent();
                if (p != null && Files.exists(p.resolve("scripts"))) return p;
            } else {
                if (Files.exists(code.resolve("scripts"))) return code;
                Path p = code.getParent();
                if (p != null && Files.exists(p.resolve("scripts"))) return p;
            }
        } catch (Exception ignored) {}
        return Paths.get("").toAbsolutePath();
    }

    private void ensureFolders() {
        try {
            for (String d : List.of("config","videos","biblioteca/videos","biblioteca/imagens","biblioteca/audios","projetos","final","temp","transcricoes")) {
                Files.createDirectories(root.resolve(d));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JComponent buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(18,22,14,22));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("KODA CONNECT");
        brand.setForeground(FG);
        brand.setFont(new Font("SansSerif", Font.BOLD, 28));
        JLabel sub = new JLabel("A ponte entre sua IA e a edição local do seu PC");
        sub.setForeground(MUTED);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        left.add(brand);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);
        p.add(left, BorderLayout.WEST);

        JLabel badge = new JLabel("v0.5  •  LOCAL");
        badge.setOpaque(true);
        badge.setBackground(new Color(35,31,20));
        badge.setForeground(new Color(247,211,119));
        badge.setBorder(new EmptyBorder(8,12,8,12));
        p.add(badge, BorderLayout.EAST);
        return p;
    }

    private JComponent buildBody() {
        JPanel body = new JPanel(new GridLayout(1,2,14,0));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(10,22,18,22));

        JPanel left = card();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(sectionTitle("Conexão"));
        left.add(Box.createVerticalStrut(8));
        left.add(statusRow("Servidor local", serverStatus));
        left.add(Box.createVerticalStrut(8));
        left.add(statusRow("Integração externa", connectionStatus));
        left.add(Box.createVerticalStrut(18));

        JButton copyInfo = primary("COPIAR DADOS DE CONEXÃO");
        copyInfo.addActionListener(e -> copyConnectionInfo());
        JButton restart = secondary("REINICIAR SERVIDOR LOCAL");
        restart.addActionListener(e -> restartServer());
        left.add(copyInfo);
        left.add(Box.createVerticalStrut(8));
        left.add(restart);
        left.add(Box.createVerticalStrut(20));

        left.add(sectionTitle("Pastas permitidas"));
        JLabel hint = new JLabel("A IA só poderá trabalhar com os locais que você autorizar.");
        hint.setForeground(MUTED);
        left.add(hint);
        left.add(Box.createVerticalStrut(8));

        allowedFolders.setRows(7);
        allowedFolders.setLineWrap(false);
        styleTextArea(allowedFolders);
        JScrollPane scroll = new JScrollPane(allowedFolders);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(scroll);
        left.add(Box.createVerticalStrut(8));

        JPanel folderButtons = new JPanel(new GridLayout(1,2,8,0));
        folderButtons.setOpaque(false);
        JButton addFolder = secondary("ADICIONAR PASTA");
        addFolder.addActionListener(e -> chooseAllowedFolder());
        JButton saveFolders = secondary("SALVAR PERMISSÕES");
        saveFolders.addActionListener(e -> saveAllowedFolders());
        folderButtons.add(addFolder);
        folderButtons.add(saveFolders);
        folderButtons.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        left.add(folderButtons);
        left.add(Box.createVerticalGlue());

        JPanel right = card();
        right.setLayout(new BorderLayout(0,12));
        JPanel intro = new JPanel();
        intro.setOpaque(false);
        intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));
        intro.add(sectionTitle("Como vai funcionar"));
        intro.add(Box.createVerticalStrut(10));
        intro.add(step("1", "Instale e abra o Koda Connect", "O servidor local inicia automaticamente."));
        intro.add(Box.createVerticalStrut(8));
        intro.add(step("2", "Conecte um assistente compatível", "A conexão futura usará as ferramentas seguras do Koda."));
        intro.add(Box.createVerticalStrut(8));
        intro.add(step("3", "Converse normalmente", "Peça cortes, estilo, formato, legendas e render."));
        intro.add(Box.createVerticalStrut(8));
        intro.add(step("4", "O PC executa", "FFmpeg, Whisper, CPU e GPU fazem o trabalho local."));
        right.add(intro, BorderLayout.NORTH);

        JPanel logWrap = new JPanel(new BorderLayout(0,6));
        logWrap.setOpaque(false);
        logWrap.add(sectionTitle("Atividade"), BorderLayout.NORTH);
        activity.setEditable(false);
        styleTextArea(activity);
        right.add(new JScrollPane(activity), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1,2,8,0));
        actions.setOpaque(false);
        JButton advanced = secondary("ABRIR EDITOR AVANÇADO");
        advanced.addActionListener(e -> SwingUtilities.invokeLater(() -> new KodaCut().setVisible(true)));
        JButton finalFolder = secondary("ABRIR RESULTADOS");
        finalFolder.addActionListener(e -> open(root.resolve("final")));
        actions.add(advanced);
        actions.add(finalFolder);
        right.add(actions, BorderLayout.SOUTH);

        body.add(left);
        body.add(right);
        return body;
    }

    private JComponent buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(14,14,14));
        p.setBorder(new EmptyBorder(10,22,12,22));
        JLabel l = new JLabel("Seus arquivos ficam no seu PC. O Koda expõe apenas funções e pastas autorizadas.");
        l.setForeground(MUTED);
        p.add(l, BorderLayout.WEST);
        return p;
    }

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(18,18,18,18));
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG);
        l.setFont(new Font("SansSerif", Font.BOLD, 18));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel statusRow(String name, JLabel value) {
        JPanel p = new JPanel(new BorderLayout(10,0));
        p.setOpaque(false);
        JLabel n = new JLabel(name);
        n.setForeground(MUTED);
        value.setForeground(FG);
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        p.add(n, BorderLayout.WEST);
        p.add(value, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));
        return p;
    }

    private JPanel step(String n, String title, String desc) {
        JPanel p = new JPanel(new BorderLayout(12,0));
        p.setBackground(new Color(27,27,27));
        p.setBorder(new EmptyBorder(10,10,10,10));
        JLabel badge = new JLabel(n, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(GOLD);
        badge.setForeground(Color.BLACK);
        badge.setFont(new Font("SansSerif", Font.BOLD, 15));
        badge.setPreferredSize(new Dimension(34,34));
        p.add(badge, BorderLayout.WEST);

        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(title);
        h.setForeground(FG);
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel d = new JLabel(desc);
        d.setForeground(MUTED);
        t.add(h); t.add(d);
        p.add(t, BorderLayout.CENTER);
        return p;
    }

    private JButton primary(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(GOLD);
        b.setForeground(Color.BLACK);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(11,14,11,14));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        return b;
    }

    private JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(FIELD);
        b.setForeground(FG);
        b.setBorder(new EmptyBorder(10,12,10,12));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));
        return b;
    }

    private void styleTextArea(JTextArea a) {
        a.setBackground(FIELD);
        a.setForeground(FG);
        a.setCaretColor(FG);
        a.setFont(new Font("Monospaced", Font.PLAIN, 12));
        a.setBorder(new EmptyBorder(8,8,8,8));
    }

    private void startServer() {
        try {
            server = new KodaConnectServer(root, 8787);
            server.start();
            serverStatus.setText("Ativo em 127.0.0.1:" + server.getPort());
            serverStatus.setForeground(new Color(140,220,150));
            append("[OK] Koda Connect local ativo.");
            append("[SEGURANÇA] Acesso protegido por token e limitado a 127.0.0.1.");
        } catch (Exception e) {
            serverStatus.setText("Erro ao iniciar");
            serverStatus.setForeground(new Color(240,130,130));
            append("[ERRO] " + e.getMessage());
        }
    }

    private void restartServer() {
        if (server != null) server.stop();
        server = null;
        startServer();
    }

    private void copyConnectionInfo() {
        if (server == null || !server.isRunning()) {
            JOptionPane.showMessageDialog(this, "Servidor local não está ativo.", "Koda Connect", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String text = "KODA CONNECT\n" +
            "URL local: http://127.0.0.1:" + server.getPort() + "\n" +
            "Token: " + server.getToken() + "\n" +
            "Header: X-Koda-Token";
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        JOptionPane.showMessageDialog(this, "Dados locais copiados. Não compartilhe o token publicamente.", "Koda Connect", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadAllowedFolders() {
        Path file = root.resolve("config/allowed-folders.txt");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                String initial =
                    root.resolve("videos").toAbsolutePath().normalize() + System.lineSeparator() +
                    root.resolve("biblioteca").toAbsolutePath().normalize() + System.lineSeparator() +
                    root.resolve("projetos").toAbsolutePath().normalize() + System.lineSeparator() +
                    root.resolve("final").toAbsolutePath().normalize() + System.lineSeparator();
                Files.writeString(file, initial, StandardCharsets.UTF_8);
            }
            allowedFolders.setText(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            append("[ERRO] Não foi possível carregar permissões: " + e.getMessage());
        }
    }

    private void saveAllowedFolders() {
        Path file = root.resolve("config/allowed-folders.txt");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, allowedFolders.getText(), StandardCharsets.UTF_8);
            append("[OK] Pastas permitidas atualizadas.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Koda Connect", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chooseAllowedFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().toPath().toAbsolutePath().normalize().toString();
            if (!allowedFolders.getText().contains(path)) {
                if (!allowedFolders.getText().endsWith(System.lineSeparator()) && !allowedFolders.getText().isBlank())
                    allowedFolders.append(System.lineSeparator());
                allowedFolders.append(path + System.lineSeparator());
            }
        }
    }

    private void open(Path p) {
        try {
            Files.createDirectories(p);
            Desktop.getDesktop().open(p.toFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Koda Connect", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void append(String s) {
        activity.append(s + System.lineSeparator());
        activity.setCaretPosition(activity.getDocument().getLength());
    }
}
