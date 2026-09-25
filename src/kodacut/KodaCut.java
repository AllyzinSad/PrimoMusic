package kodacut;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.List;

public class KodaCut extends JFrame {
    private final Path root;

    private final JTextField mainVideoField = new JTextField();
    private final JTextField outputField = new JTextField();

    private final JComboBox<String> outputMode = new JComboBox<>(new String[]{
        "Horizontal (YouTube 16:9)",
        "Vertical (Reels / Shorts / Stories 9:16)",
        "Gerar as duas versoes"
    });

    private final JComboBox<String> verticalMode = new JComboBox<>(new String[]{
        "Preservar video + fundo borrado",
        "Crop central 9:16"
    });

    private final JComboBox<String> quality = new JComboBox<>(new String[]{
        "Balanceado (recomendado)", "ECO", "Qualidade"
    });

    private final JTextArea promptArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();

    private final JButton renderButton = new JButton("EDITAR / RENDERIZAR");
    private final JButton setupButton = new JButton("CONFIGURAR FFmpeg");
    private final JButton openFinalButton = new JButton("ABRIR PASTA FINAL");

    private final AssetTableModel assetModel = new AssetTableModel();
    private final JTable assetTable = new JTable(assetModel);

    private static final Set<String> VIDEO_EXT = Set.of(
        "mp4","mov","mkv","webm","avi","m4v","wmv","ts"
    );
    private static final Set<String> IMAGE_EXT = Set.of(
        "png","jpg","jpeg","webp","bmp","gif"
    );
    private static final Set<String> AUDIO_EXT = Set.of(
        "mp3","wav","m4a","aac","flac","ogg","opus","wma"
    );

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KodaCut().setVisible(true));
    }

    public KodaCut() {
        root = detectRoot();
        ensureFolders();
        buildUi();
        scanLibrary();
        loadExample();
    }

    private Path detectRoot() {
        try {
            Path code = Paths.get(KodaCut.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
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
            Files.createDirectories(root.resolve("biblioteca").resolve("videos"));
            Files.createDirectories(root.resolve("biblioteca").resolve("imagens"));
            Files.createDirectories(root.resolve("biblioteca").resolve("audios"));
            Files.createDirectories(root.resolve("projetos"));
            Files.createDirectories(root.resolve("final"));
        } catch (IOException e) {
            throw new RuntimeException("Nao foi possivel criar as pastas do Koda Cut: " + e.getMessage(), e);
        }
    }

    private void buildUi() {
        Color bg = new Color(11,11,11);
        Color panel = new Color(23,23,23);
        Color field = new Color(34,34,34);
        Color fg = new Color(245,245,245);
        Color muted = new Color(160,160,160);

        setTitle("Koda Cut v0.2");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 780));
        setSize(1240, 880);
        setLocationRelativeTo(null);
        getContentPane().setBackground(bg);
        setLayout(new BorderLayout(12,12));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(bg);
        header.setBorder(new EmptyBorder(16,20,0,20));

        JLabel brand = new JLabel("KODA CUT");
        brand.setForeground(fg);
        brand.setFont(new Font("SansSerif", Font.BOLD, 30));
        header.add(brand, BorderLayout.WEST);

        JLabel sub = new JLabel("Editor automatico local • videos, imagens e audios • FFmpeg + NVENC");
        sub.setForeground(muted);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(panel);
        tabs.setForeground(fg);
        tabs.addTab("PROJETO", buildProjectTab(bg, panel, field, fg, muted));
        tabs.addTab("ARQUIVOS / ELEMENTOS", buildAssetsTab(bg, panel, field, fg, muted));
        tabs.addTab("CONSOLE", buildConsoleTab(bg, panel, fg));
        tabs.setBorder(new EmptyBorder(0,20,0,20));
        add(tabs, BorderLayout.CENTER);

        JLabel footer = new JLabel("Koda ecosystem • nada e enviado para a internet pelo editor • processamento local no seu PC");
        footer.setForeground(muted);
        footer.setBorder(new EmptyBorder(0,20,12,20));
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel buildProjectTab(Color bg, Color panel, Color field, Color fg, Color muted) {
        JPanel wrap = new JPanel(new BorderLayout(12,12));
        wrap.setBackground(bg);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(panel);
        left.setBorder(new EmptyBorder(16,16,16,16));

        left.add(label("1. Video principal", fg));
        left.add(Box.createVerticalStrut(6));
        left.add(fileRow(mainVideoField, "ADICIONAR VIDEO", e -> chooseMainVideo(), field, fg));
        left.add(Box.createVerticalStrut(14));

        left.add(label("2. Formato de saida", fg));
        left.add(Box.createVerticalStrut(6));
        styleCombo(outputMode, field, fg);
        left.add(outputMode);
        left.add(Box.createVerticalStrut(8));

        left.add(label("Modo vertical", fg));
        left.add(Box.createVerticalStrut(6));
        styleCombo(verticalMode, field, fg);
        left.add(verticalMode);
        left.add(Box.createVerticalStrut(8));

        left.add(label("Qualidade", fg));
        left.add(Box.createVerticalStrut(6));
        styleCombo(quality, field, fg);
        left.add(quality);
        left.add(Box.createVerticalStrut(14));

        left.add(label("3. Pasta final", fg));
        left.add(Box.createVerticalStrut(6));
        outputField.setText(root.resolve("final").toString());
        left.add(fileRow(outputField, "ESCOLHER PASTA", e -> chooseOutput(), field, fg));

        JPanel right = new JPanel(new BorderLayout(0,8));
        right.setBackground(panel);
        right.setBorder(new EmptyBorder(16,16,16,16));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);
        titlePanel.add(label("4. Prompt de edicao / KodaScript", fg), BorderLayout.WEST);

        JLabel info = new JLabel("Cole exatamente o JSON gerado pelo ChatGPT");
        info.setForeground(muted);
        titlePanel.add(info, BorderLayout.SOUTH);
        right.add(titlePanel, BorderLayout.NORTH);

        promptArea.setBackground(field);
        promptArea.setForeground(fg);
        promptArea.setCaretColor(fg);
        promptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        promptArea.setLineWrap(false);
        right.add(new JScrollPane(promptArea), BorderLayout.CENTER);

        JPanel promptButtons = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        promptButtons.setOpaque(false);
        JButton load = button("CARREGAR PROMPT");
        JButton save = button("SALVAR PROMPT");
        JButton example = button("EXEMPLO");
        load.addActionListener(e -> loadPrompt());
        save.addActionListener(e -> savePrompt());
        example.addActionListener(e -> loadExample());
        promptButtons.add(load);
        promptButtons.add(save);
        promptButtons.add(example);
        right.add(promptButtons, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.36);
        split.setDividerLocation(390);
        split.setBorder(null);
        wrap.add(split, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1,3,8,0));
        actions.setBackground(bg);
        stylePrimary(renderButton);
        styleSecondary(setupButton);
        styleSecondary(openFinalButton);
        renderButton.addActionListener(e -> render());
        setupButton.addActionListener(e -> runSetup());
        openFinalButton.addActionListener(e -> openFinal());
        actions.add(renderButton);
        actions.add(setupButton);
        actions.add(openFinalButton);
        wrap.add(actions, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildAssetsTab(Color bg, Color panel, Color field, Color fg, Color muted) {
        JPanel wrap = new JPanel(new BorderLayout(10,10));
        wrap.setBackground(panel);
        wrap.setBorder(new EmptyBorder(16,16,16,16));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(label("Biblioteca do projeto", fg));
        JLabel help = new JLabel("Adicione qualquer video, imagem/PNG ou audio. O Koda Cut cria um ID para cada arquivo.");
        help.setForeground(muted);
        heading.add(help);
        top.add(heading, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        buttons.setOpaque(false);
        JButton addFiles = button("ADICIONAR ARQUIVOS");
        JButton remove = button("REMOVER");
        JButton reload = button("RECARREGAR");
        JButton copy = button("COPIAR MAPA P/ CHATGPT");
        JButton folder = button("ABRIR BIBLIOTECA");

        addFiles.addActionListener(e -> addAssets());
        remove.addActionListener(e -> removeSelectedAsset());
        reload.addActionListener(e -> scanLibrary());
        copy.addActionListener(e -> copyManifest());
        folder.addActionListener(e -> openLibrary());

        buttons.add(addFiles);
        buttons.add(remove);
        buttons.add(reload);
        buttons.add(copy);
        buttons.add(folder);
        top.add(buttons, BorderLayout.EAST);
        wrap.add(top, BorderLayout.NORTH);

        assetTable.setBackground(field);
        assetTable.setForeground(fg);
        assetTable.setGridColor(new Color(58,58,58));
        assetTable.setSelectionBackground(new Color(70,70,70));
        assetTable.setSelectionForeground(Color.WHITE);
        assetTable.setRowHeight(26);
        assetTable.getTableHeader().setReorderingAllowed(false);
        assetTable.getColumnModel().getColumn(0).setPreferredWidth(210);
        assetTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        assetTable.getColumnModel().getColumn(2).setPreferredWidth(330);
        assetTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        wrap.add(new JScrollPane(assetTable), BorderLayout.CENTER);

        JTextArea explanation = new JTextArea(
            "COMO O KODA CUT RECONHECE OS ARQUIVOS\n" +
            "• Video: video:nome_do_arquivo\n" +
            "• Imagem/PNG: image:nome_do_arquivo\n" +
            "• Audio/SFX/musica: audio:nome_do_arquivo\n\n" +
            "Exemplo: se voce adicionar boom.wav, o ID vira audio:boom. " +
            "No prompt o ChatGPT manda algo como: tocar audio:boom em 00:12.400.\n\n" +
            "O programa nao precisa adivinhar qual som voce quis dizer: o ID liga exatamente o comando ao arquivo certo. " +
            "O mesmo vale para B-roll, logos, memes, PNGs, fotos, musicas e efeitos."
        );
        explanation.setEditable(false);
        explanation.setWrapStyleWord(true);
        explanation.setLineWrap(true);
        explanation.setRows(6);
        explanation.setBackground(new Color(16,16,16));
        explanation.setForeground(new Color(205,205,205));
        explanation.setBorder(new EmptyBorder(10,10,10,10));
        wrap.add(explanation, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildConsoleTab(Color bg, Color panel, Color fg) {
        JPanel wrap = new JPanel(new BorderLayout(0,10));
        wrap.setBackground(panel);
        wrap.setBorder(new EmptyBorder(16,16,16,16));
        wrap.add(label("Console / progresso", fg), BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setBackground(new Color(8,8,8));
        logArea.setForeground(new Color(215,215,215));
        logArea.setCaretColor(fg);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        wrap.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return wrap;
    }

    private JLabel label(String text, Color c) {
        JLabel l = new JLabel(text);
        l.setForeground(c);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        return l;
    }

    private JPanel fileRow(JTextField f, String text, java.awt.event.ActionListener a, Color bg, Color fg) {
        f.setBackground(bg);
        f.setForeground(fg);
        f.setCaretColor(fg);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60,60,60)),
            new EmptyBorder(7,8,7,8)
        ));
        JButton b = button(text);
        b.addActionListener(a);
        JPanel p = new JPanel(new BorderLayout(8,0));
        p.setOpaque(false);
        p.add(f, BorderLayout.CENTER);
        p.add(b, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
        return p;
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(new Color(45,45,45));
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(8,12,8,12));
        return b;
    }

    private void stylePrimary(JButton b) {
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(Color.BLACK);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setBorder(new EmptyBorder(12,12,12,12));
    }

    private void styleSecondary(JButton b) {
        b.setFocusPainted(false);
        b.setBackground(new Color(44,44,44));
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(10,12,10,12));
    }

    private void styleCombo(JComboBox<String> c, Color bg, Color fg) {
        c.setBackground(bg);
        c.setForeground(fg);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
    }

    private void chooseMainVideo() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path p = fc.getSelectedFile().toPath();
            if (!"video".equals(classify(p))) {
                error("O video principal precisa ser um arquivo de video.");
                return;
            }
            mainVideoField.setText(p.toAbsolutePath().toString());
        }
    }

    private void chooseOutput() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void addAssets() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        int ok = 0;
        List<String> unsupported = new ArrayList<>();

        for (File f : fc.getSelectedFiles()) {
            Path src = f.toPath();
            String type = classify(src);
            if (type == null) {
                unsupported.add(f.getName());
                continue;
            }
            try {
                Path dir = libraryDir(type);
                Files.createDirectories(dir);
                Path dst = uniqueDestination(dir, src.getFileName().toString());
                Files.copy(src, dst, StandardCopyOption.COPY_ATTRIBUTES);
                ok++;
            } catch (IOException ex) {
                append("[ERRO] " + f.getName() + ": " + ex.getMessage());
            }
        }

        scanLibrary();
        append("[OK] " + ok + " arquivo(s) adicionado(s) a biblioteca.");
        if (!unsupported.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Formatos nao reconhecidos:\n" + String.join("\n", unsupported),
                "Koda Cut", JOptionPane.WARNING_MESSAGE);
        }
    }

    private Path uniqueDestination(Path dir, String name) {
        Path original = dir.resolve(name);
        if (!Files.exists(original)) return original;

        String stem = stem(name);
        String ext = extension(name);
        int n = 2;
        while (true) {
            String candidate = stem + "_" + n + (ext.isEmpty() ? "" : "." + ext);
            Path p = dir.resolve(candidate);
            if (!Files.exists(p)) return p;
            n++;
        }
    }

    private void removeSelectedAsset() {
        int row = assetTable.getSelectedRow();
        if (row < 0) {
            error("Selecione um arquivo da biblioteca.");
            return;
        }

        Asset a = assetModel.assets.get(row);
        int choice = JOptionPane.showConfirmDialog(this,
            "Remover da biblioteca?\n" + a.file.getFileName(),
            "Koda Cut", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) return;

        try {
            Files.deleteIfExists(a.file);
            scanLibrary();
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void scanLibrary() {
        List<Asset> found = new ArrayList<>();
        scanType(found, "video");
        scanType(found, "image");
        scanType(found, "audio");
        found.sort(Comparator.comparing((Asset a) -> a.type).thenComparing(a -> a.file.getFileName().toString().toLowerCase()));
        assetModel.setAssets(found);
        append("[INFO] Biblioteca: " + found.size() + " elemento(s).");
    }

    private void scanType(List<Asset> out, String type) {
        Path dir = libraryDir(type);
        try {
            Files.createDirectories(dir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) {
                    if (!Files.isRegularFile(p)) continue;
                    if (!type.equals(classify(p))) continue;
                    String id = typePrefix(type) + ":" + slug(stem(p.getFileName().toString()));
                    String detail = humanSize(Files.size(p));
                    out.add(new Asset(id, type, p, detail));
                }
            }
        } catch (IOException e) {
            append("[ERRO] Falha ao ler biblioteca: " + e.getMessage());
        }
    }

    private String classify(Path p) {
        String ext = extension(p.getFileName().toString()).toLowerCase(Locale.ROOT);
        if (VIDEO_EXT.contains(ext)) return "video";
        if (IMAGE_EXT.contains(ext)) return "image";
        if (AUDIO_EXT.contains(ext)) return "audio";
        return null;
    }

    private Path libraryDir(String type) {
        if ("video".equals(type)) return root.resolve("biblioteca").resolve("videos");
        if ("image".equals(type)) return root.resolve("biblioteca").resolve("imagens");
        return root.resolve("biblioteca").resolve("audios");
    }

    private String typePrefix(String type) {
        return switch (type) {
            case "image" -> "image";
            case "audio" -> "audio";
            default -> "video";
        };
    }

    private String slug(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        n = n.replaceAll("^_+|_+$", "");
        return n.isBlank() ? "arquivo" : n;
    }

    private static String stem(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    private static String extension(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 && i < name.length()-1 ? name.substring(i+1) : "";
    }

    private String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
    }

    private void copyManifest() {
        String text = buildManifestText();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        JOptionPane.showMessageDialog(this,
            "Mapa copiado. Cole junto da mensagem para o ChatGPT quando quiser que ele gere a edicao.",
            "Koda Cut", JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildManifestText() {
        StringBuilder sb = new StringBuilder();
        sb.append("KODA CUT - MAPA DE ARQUIVOS\n");
        String main = mainVideoField.getText().trim();
        if (!main.isEmpty()) {
            sb.append("video:principal -> ").append(Paths.get(main).getFileName()).append("\n");
        }
        for (Asset a : assetModel.assets) {
            sb.append(a.id).append(" -> ").append(a.file.getFileName()).append("\n");
        }
        return sb.toString();
    }

    private Path saveManifestInternal() throws IOException {
        Path p = root.resolve("projetos").resolve("assets.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        boolean first = true;
        String main = mainVideoField.getText().trim();
        if (!main.isEmpty()) {
            sb.append("  \"video:principal\": {\"type\":\"video\",\"path\":\"")
              .append(jsonEscape(Paths.get(main).toAbsolutePath().toString()))
              .append("\",\"name\":\"")
              .append(jsonEscape(Paths.get(main).getFileName().toString()))
              .append("\"}");
            first = false;
        }

        for (Asset a : assetModel.assets) {
            if (!first) sb.append(",\n");
            sb.append("  \"").append(jsonEscape(a.id)).append("\": {\"type\":\"")
              .append(a.type).append("\",\"path\":\"")
              .append(jsonEscape(a.file.toAbsolutePath().toString()))
              .append("\",\"name\":\"")
              .append(jsonEscape(a.file.getFileName().toString()))
              .append("\"}");
            first = false;
        }
        sb.append("\n}\n");
        Files.writeString(p, sb.toString(), StandardCharsets.UTF_8);
        return p;
    }

    private String jsonEscape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private void loadExample() {
        Path p = root.resolve("projetos").resolve("exemplo-geral.json");
        try {
            if (Files.exists(p)) {
                promptArea.setText(Files.readString(p, StandardCharsets.UTF_8));
            } else {
                promptArea.setText("{\n  \"version\": 2,\n  \"fps\": 60,\n  \"timeline\": []\n}");
            }
        } catch (IOException e) {
            promptArea.setText("{\n  \"version\": 2,\n  \"timeline\": []\n}");
        }
    }

    private void loadPrompt() {
        JFileChooser fc = new JFileChooser(root.resolve("projetos").toFile());
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                promptArea.setText(Files.readString(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                error(e.getMessage());
            }
        }
    }

    private Path savePromptInternal() throws IOException {
        Path p = root.resolve("projetos").resolve("ultimo.json");
        Files.writeString(p, promptArea.getText(), StandardCharsets.UTF_8);
        return p;
    }

    private void savePrompt() {
        try {
            Path p = savePromptInternal();
            saveManifestInternal();
            append("[OK] Prompt salvo em: " + p);
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private String outputModeCode() {
        if (outputMode.getSelectedIndex() == 1) return "vertical";
        if (outputMode.getSelectedIndex() == 2) return "ambos";
        return "horizontal";
    }

    private String verticalModeCode() {
        return verticalMode.getSelectedIndex() == 1 ? "crop" : "blur";
    }

    private String qualityCode() {
        if (quality.getSelectedIndex() == 1) return "eco";
        if (quality.getSelectedIndex() == 2) return "qualidade";
        return "balanceado";
    }

    private void runSetup() {
        runProcess(List.of(
            "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
            root.resolve("scripts").resolve("configurar.ps1").toString()
        ), "Configuracao concluida.");
    }

    private void render() {
        String video = mainVideoField.getText().trim();
        if (video.isEmpty() || !Files.exists(Paths.get(video))) {
            error("Adicione um video principal valido.");
            return;
        }

        if (promptArea.getText().trim().isEmpty()) {
            error("Cole o prompt/KodaScript da edicao.");
            return;
        }

        try {
            Path project = savePromptInternal();
            Path manifest = saveManifestInternal();
            Path out = Paths.get(outputField.getText().trim());
            Files.createDirectories(out);

            List<String> cmd = new ArrayList<>();
            Collections.addAll(cmd,
                "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
                root.resolve("scripts").resolve("editor.ps1").toString(),
                "-Video",video,
                "-Project",project.toString(),
                "-Manifest",manifest.toString(),
                "-OutputMode",outputModeCode(),
                "-VerticalMode",verticalModeCode(),
                "-Quality",qualityCode(),
                "-OutputDir",out.toString()
            );

            runProcess(cmd, "Renderizacao finalizada.");
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void runProcess(List<String> cmd, String success) {
        renderButton.setEnabled(false);
        setupButton.setEnabled(false);
        append("");
        append("> Iniciando processo...");

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(root.toFile());
                pb.redirectErrorStream(true);
                Process p = pb.start();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) append(line);
                }

                int code = p.waitFor();
                append(code == 0 ? "[OK] " + success : "[ERRO] Processo saiu com codigo " + code);
            } catch (Exception e) {
                append("[ERRO] " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    renderButton.setEnabled(true);
                    setupButton.setEnabled(true);
                });
            }
        }, "koda-cut-worker").start();
    }

    private void openFinal() {
        try {
            Path p = Paths.get(outputField.getText().trim());
            Files.createDirectories(p);
            Desktop.getDesktop().open(p.toFile());
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void openLibrary() {
        try {
            Path p = root.resolve("biblioteca");
            Files.createDirectories(p);
            Desktop.getDesktop().open(p.toFile());
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void error(String s) {
        JOptionPane.showMessageDialog(this, s, "Koda Cut", JOptionPane.ERROR_MESSAGE);
    }

    private static class Asset {
        final String id;
        final String type;
        final Path file;
        final String details;

        Asset(String id, String type, Path file, String details) {
            this.id = id;
            this.type = type;
            this.file = file;
            this.details = details;
        }
    }

    private static class AssetTableModel extends AbstractTableModel {
        private final String[] cols = {"ID para o prompt", "Tipo", "Arquivo", "Tamanho"};
        private List<Asset> assets = new ArrayList<>();

        void setAssets(List<Asset> list) {
            this.assets = new ArrayList<>(list);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return assets.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Asset a = assets.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> a.id;
                case 1 -> a.type;
                case 2 -> a.file.getFileName().toString();
                default -> a.details;
            };
        }
    }
}
