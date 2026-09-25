package kodacut;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class KodaCut extends JFrame {
    private final Path root;
    private final JTextField videoField = new JTextField();
    private final JTextField outputField = new JTextField();
    private final JComboBox<String> outputMode = new JComboBox<>(new String[]{
        "Horizontal (YouTube 16:9)",
        "Vertical (Reels / Shorts 9:16)",
        "Gerar as duas versoes"
    });
    private final JComboBox<String> verticalMode = new JComboBox<>(new String[]{
        "Preservar gameplay + fundo borrado",
        "Crop central 9:16"
    });
    private final JComboBox<String> quality = new JComboBox<>(new String[]{
        "Balanceado (recomendado)", "ECO", "Qualidade"
    });
    private final JTextArea jsonArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final JButton renderButton = new JButton("RENDERIZAR");
    private final JButton setupButton = new JButton("CONFIGURAR");
    private final JButton openFinalButton = new JButton("ABRIR PASTA FINAL");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KodaCut().setVisible(true));
    }

    public KodaCut() {
        root = detectRoot();
        buildUi();
        loadDefaultProject();
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

    private void buildUi() {
        Color bg = new Color(12,12,12);
        Color panel = new Color(24,24,24);
        Color fg = new Color(245,245,245);
        Color muted = new Color(160,160,160);
        Color field = new Color(34,34,34);

        setTitle("Koda Cut");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000,760));
        setSize(1120,820);
        setLocationRelativeTo(null);
        getContentPane().setBackground(bg);
        setLayout(new BorderLayout(14,14));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(bg);
        header.setBorder(new EmptyBorder(18,22,0,22));
        JLabel brand = new JLabel("KODA CUT");
        brand.setForeground(fg);
        brand.setFont(new Font("SansSerif", Font.BOLD, 30));
        header.add(brand, BorderLayout.WEST);
        JLabel sub = new JLabel("Editor automatico local • FFmpeg + NVENC • 16:9 / 9:16");
        sub.setForeground(muted);
        header.add(sub, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1,2,14,0));
        body.setBackground(bg);
        body.setBorder(new EmptyBorder(0,22,0,22));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(panel);
        left.setBorder(new EmptyBorder(18,18,18,18));

        left.add(label("1. Video", fg));
        left.add(Box.createVerticalStrut(6));
        left.add(fileRow(videoField, "ESCOLHER VIDEO", e -> chooseVideo(), field, fg));
        left.add(Box.createVerticalStrut(14));

        left.add(label("2. Formato", fg));
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
        left.add(Box.createVerticalStrut(14));

        left.add(label("4. Cole aqui o JSON que o ChatGPT mandar", fg));
        left.add(Box.createVerticalStrut(6));
        jsonArea.setBackground(field);
        jsonArea.setForeground(fg);
        jsonArea.setCaretColor(fg);
        jsonArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane jsonScroll = new JScrollPane(jsonArea);
        jsonScroll.setPreferredSize(new Dimension(470,300));
        left.add(jsonScroll);

        JPanel jsonButtons = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));
        jsonButtons.setBackground(panel);
        JButton load = button("CARREGAR JSON");
        JButton save = button("SALVAR JSON");
        load.addActionListener(e -> loadJson());
        save.addActionListener(e -> saveJson());
        jsonButtons.add(load);
        jsonButtons.add(save);
        left.add(jsonButtons);
        body.add(left);

        JPanel right = new JPanel(new BorderLayout(0,12));
        right.setBackground(panel);
        right.setBorder(new EmptyBorder(18,18,18,18));
        right.add(label("Console / progresso", fg), BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setBackground(new Color(8,8,8));
        logArea.setForeground(new Color(215,215,215));
        logArea.setCaretColor(fg);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        right.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(3,1,0,8));
        actions.setBackground(panel);
        stylePrimary(renderButton);
        styleSecondary(setupButton);
        styleSecondary(openFinalButton);
        renderButton.addActionListener(e -> render());
        setupButton.addActionListener(e -> runSetup());
        openFinalButton.addActionListener(e -> openFinal());
        actions.add(renderButton);
        actions.add(setupButton);
        actions.add(openFinalButton);
        right.add(actions, BorderLayout.SOUTH);
        body.add(right);

        add(body, BorderLayout.CENTER);
        JLabel footer = new JLabel("Koda ecosystem • processamento local no seu PC");
        footer.setForeground(muted);
        footer.setBorder(new EmptyBorder(0,22,14,22));
        add(footer, BorderLayout.SOUTH);
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
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
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

    private void chooseVideo() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            videoField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void chooseOutput() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadDefaultProject() {
        Path p = root.resolve("projetos").resolve("exemplo-ddtank.json");
        try {
            if (Files.exists(p)) jsonArea.setText(Files.readString(p, StandardCharsets.UTF_8));
            else jsonArea.setText("{\n  \"duracao_saida\": 20,\n  \"eventos\": []\n}");
        } catch (IOException e) {
            jsonArea.setText("{\n  \"eventos\": []\n}");
        }
    }

    private void loadJson() {
        JFileChooser fc = new JFileChooser(root.resolve("projetos").toFile());
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                jsonArea.setText(Files.readString(fc.getSelectedFile().toPath(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                error(e.getMessage());
            }
        }
    }

    private Path saveJsonInternal() throws IOException {
        Path dir = root.resolve("projetos");
        Files.createDirectories(dir);
        Path p = dir.resolve("ultimo.json");
        Files.writeString(p, jsonArea.getText(), StandardCharsets.UTF_8);
        return p;
    }

    private void saveJson() {
        try {
            append("JSON salvo em: " + saveJsonInternal());
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
        String video = videoField.getText().trim();
        if (video.isEmpty() || !Files.exists(Paths.get(video))) {
            error("Escolha um video valido.");
            return;
        }
        try {
            Path project = saveJsonInternal();
            Path out = Paths.get(outputField.getText().trim());
            Files.createDirectories(out);

            List<String> cmd = new ArrayList<>();
            Collections.addAll(cmd,
                "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
                root.resolve("scripts").resolve("editor.ps1").toString(),
                "-Video",video,
                "-Project",project.toString(),
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
        append("> " + String.join(" ", cmd));
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

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void error(String s) {
        JOptionPane.showMessageDialog(this, s, "Koda Cut", JOptionPane.ERROR_MESSAGE);
    }
}
