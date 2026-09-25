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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KodaCut extends JFrame {
    private final Path root;

    private static final Color BG = new Color(10, 10, 10);
    private static final Color PANEL = new Color(21, 21, 21);
    private static final Color FIELD = new Color(32, 32, 32);
    private static final Color FG = new Color(245, 245, 245);
    private static final Color MUTED = new Color(158, 158, 158);
    private static final Color LINE = new Color(55, 55, 55);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final JPanel sidebar = new JPanel();
    private final LinkedHashMap<String, JButton> navButtons = new LinkedHashMap<>();
    private boolean sidebarCollapsed = false;

    private final JTextField mainVideoField = new JTextField();
    private final JTextField outputField = new JTextField();

    private final JComboBox<String> outputMode = new JComboBox<>(new String[]{
        "Horizontal 16:9 (YouTube)",
        "Vertical 9:16 (Reels / Shorts / Stories)",
        "Quadrado 1:1",
        "Horizontal + Vertical",
        "Gerar todos"
    });

    private final JComboBox<String> verticalMode = new JComboBox<>(new String[]{
        "Preservar video + fundo borrado",
        "Crop central 9:16"
    });

    private final JComboBox<String> quality = new JComboBox<>(new String[]{
        "Balanceado (recomendado)", "ECO", "Qualidade"
    });

    private final JComboBox<String> stylePreset = new JComboBox<>(new String[]{
        "Personalizado", "Gameplay / Meme", "Dark / Narrado",
        "Podcast / Cortes", "Shorts / Reels", "Clean / Documentario", "Cinematico"
    });

    private final JComboBox<String> contentType = new JComboBox<>(new String[]{
        "Gameplay", "Opiniao / Comentario", "Podcast / Entrevista", "Vlog",
        "Review / Analise", "Tutorial / Educacional", "Dark / Narrado",
        "Storytelling", "Documentario", "Noticias / Informativo",
        "Produto / E-commerce", "Institucional", "Imobiliario", "Gastronomia",
        "Fitness", "Viagem", "Musica", "React", "Unboxing", "Shorts / Reels",
        "Anuncio / Redes sociais"
    });

    private final JComboBox<String> editStyle = new JComboBox<>(new String[]{
        "Clean", "Dinamico", "Meme", "Cinematico", "Profissional",
        "Viral", "Minimalista", "Dark", "Energetico", "Storytelling", "Corporativo"
    });

    private final JComboBox<String> destination = new JComboBox<>(new String[]{
        "YouTube horizontal 16:9", "Vertical 9:16", "Instagram Feed 1:1",
        "Stories 9:16", "Podcast completo", "Corte curto", "Anuncio"
    });

    private final JTextArea guidedDescription = new JTextArea();

    private final JTextArea promptArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();

    private final JCheckBox autoTranscribe = new JCheckBox("Transcrever automaticamente", true);
    private final JComboBox<String> captionMode = new JComboBox<>(new String[]{
        "Desligada", "Completa", "Destaques", "Palavra por palavra"
    });
    private final JComboBox<String> language = new JComboBox<>(new String[]{"pt", "auto", "en", "es"});
    private final JCheckBox safeZone = new JCheckBox("Safe zone para Reels/TikTok", true);

    private final JCheckBox noiseReduction = new JCheckBox("Reducao de ruido", true);
    private final JCheckBox normalizeAudio = new JCheckBox("Normalizar voz", true);
    private final JCheckBox ducking = new JCheckBox("Ducking automatico da musica", true);
    private final JSpinner voiceVolume = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 3.0, 0.05));
    private final JSpinner musicVolume = new JSpinner(new SpinnerNumberModel(0.08, 0.0, 1.0, 0.01));

    private final JButton renderButton = new JButton("EDITAR / RENDERIZAR");
    private final JButton setupButton = new JButton("CONFIGURAR FERRAMENTAS");

    private final AssetTableModel assetModel = new AssetTableModel();
    private final JTable assetTable = new JTable(assetModel);

    private final JTextField cutsUrlField = new JTextField();
    private final JTextField cutsSourceField = new JTextField();
    private final JTextArea cutsPromptArea = new JTextArea();
    private final JCheckBox rightsCheck = new JCheckBox("Confirmo que tenho permissao para usar/reutilizar este conteudo");
    private final JSpinner cutCount = new JSpinner(new SpinnerNumberModel(6, 1, 30, 1));
    private final JSpinner cutMin = new JSpinner(new SpinnerNumberModel(35, 10, 600, 5));
    private final JSpinner cutMax = new JSpinner(new SpinnerNumberModel(70, 10, 900, 5));
    private final JComboBox<String> cutFormat = new JComboBox<>(new String[]{"Vertical 9:16", "Horizontal 16:9"});
    private final JComboBox<String> cutCaptionMode = new JComboBox<>(new String[]{"Completa", "Destaques", "Desligada"});

    private final DefaultListModel<Path> batchModel = new DefaultListModel<>();
    private final JList<Path> batchList = new JList<>(batchModel);

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
        SwingUtilities.invokeLater(() -> {
            UIManager.put("ToolTip.background", FIELD);
            UIManager.put("ToolTip.foreground", FG);
            KodaCut app = new KodaCut();
            app.setVisible(true);
        });
    }

    public KodaCut() {
        root = detectRoot();
        ensureFolders();
        setIconImage(KodaIcon.createIcon(256));
        buildUi();
        scanLibrary();
        loadExample();
        applyProfile("Gameplay / Meme");
        SwingUtilities.invokeLater(() -> OnboardingDialog.showIfNeeded(this, root));
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
        String[] dirs = {
            "biblioteca/videos","biblioteca/imagens","biblioteca/audios",
            "projetos","final","downloads","transcricoes","temp","tools","assets"
        };
        try {
            for (String d : dirs) Files.createDirectories(root.resolve(d));
        } catch (IOException e) {
            throw new RuntimeException("Nao foi possivel criar as pastas do Koda Cut: " + e.getMessage(), e);
        }
    }

    private void buildUi() {
        setTitle("Koda Cut v0.4");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1380, 900);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BG);
        sidebar.setBackground(new Color(14,14,14));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(10,10,10,10));
        sidebar.setPreferredSize(new Dimension(224, 100));

        JButton collapse = navButton("≡  Recolher", "≡", null);
        collapse.addActionListener(e -> toggleSidebar());
        sidebar.add(collapse);
        sidebar.add(Box.createVerticalStrut(8));

        addNav("Inicio", "INICIO", "IN");
        addNav("Novo Projeto", "GUIA", "NP");
        addNav("Edicao Automatica", "AUTO", "EA");
        addNav("Canal de Cortes", "CORTES", "CC");
        addNav("Gameplay", "GAMEPLAY", "GM");
        addNav("Dark / Narrado", "DARK", "DK");
        addNav("Podcast / Cortes", "PODCAST", "PC");
        addNav("Shorts / Reels", "SHORTS", "SR");
        addNav("Elementos", "ELEMENTOS", "EL");
        addNav("Legendas", "LEGENDAS", "CC");
        addNav("Audio", "AUDIO", "AU");
        addNav("Formato", "FORMATO", "FM");
        addNav("Estilos", "ESTILOS", "ST");
        addNav("Lote", "LOTE", "LT");
        addNav("Renderizacao", "RENDER", "RD");
        addNav("Como usar", "TUTORIAL", "?");
        addNav("Configuracoes", "CONFIG", "CF");

        sidebar.add(Box.createVerticalGlue());

        JLabel version = new JLabel("v0.3 • local");
        version.setForeground(MUTED);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(version);

        cards.setBackground(BG);
        cards.add(buildHomePanel(), "INICIO");
        cards.add(buildGuidePanel(), "GUIA");
        cards.add(buildAutoPanel(), "AUTO");
        cards.add(buildCutsPanel(), "CORTES");
        cards.add(buildPresetPanel(
            "GAMEPLAY / MEME",
            "Cortes rapidos, zooms, freeze frames, PNGs, SFX e legendas somente nos momentos que merecem destaque.",
            "Gameplay / Meme"), "GAMEPLAY");
        cards.add(buildPresetPanel(
            "DARK / NARRADO",
            "Legenda praticamente completa, ritmo constante, B-rolls, trilha baixa e leitura clara da narracao.",
            "Dark / Narrado"), "DARK");
        cards.add(buildPresetPanel(
            "PODCAST / CORTES",
            "Transcreve, procura trechos fortes e prepara clipes verticais ou horizontais com legenda automatica.",
            "Podcast / Cortes"), "PODCAST");
        cards.add(buildPresetPanel(
            "SHORTS / REELS",
            "9:16, safe zones, legenda grande, enquadramento vertical e ritmo mais agressivo para conteudo curto.",
            "Shorts / Reels"), "SHORTS");
        cards.add(buildAssetsPanel(), "ELEMENTOS");
        cards.add(buildCaptionsPanel(), "LEGENDAS");
        cards.add(buildAudioPanel(), "AUDIO");
        cards.add(buildFormatPanel(), "FORMATO");
        cards.add(buildStylesPanel(), "ESTILOS");
        cards.add(buildBatchPanel(), "LOTE");
        cards.add(buildRenderPanel(), "RENDER");
        cards.add(buildTutorialPanel(), "TUTORIAL");
        cards.add(buildSettingsPanel(), "CONFIG");

        body.add(sidebar, BorderLayout.WEST);
        body.add(cards, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JLabel footer = new JLabel("  Koda Cut • seus projetos, seus arquivos, sua edição");
        footer.setForeground(MUTED);
        footer.setBorder(new EmptyBorder(7,12,9,12));
        add(footer, BorderLayout.SOUTH);

        cardLayout.show(cards, "INICIO");
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(12,16,10,16));

        JComponent brand = new JComponent() {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D g = (Graphics2D) graphics.create();
                KodaIcon.paintBrand(g, getWidth(), getHeight(), FG);
                g.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(270, 64); }
        };
        header.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("AUTOMATIC VIDEO EDITOR");
        title.setForeground(FG);
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel sub = new JLabel("Edição automática para criadores • processamento local");
        sub.setForeground(MUTED);
        sub.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(Box.createVerticalGlue());
        right.add(title);
        right.add(sub);
        right.add(Box.createVerticalGlue());
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void addNav(String text, String card, String shortText) {
        JButton b = navButton(text, shortText, card);
        b.addActionListener(e -> {
            cardLayout.show(cards, card);
            highlightNav(card);
        });
        navButtons.put(card, b);
        sidebar.add(b);
        sidebar.add(Box.createVerticalStrut(4));
    }

    private JButton navButton(String text, String shortText, String card) {
        JButton b = new JButton(text);
        b.putClientProperty("fullText", text);
        b.putClientProperty("shortText", shortText);
        b.putClientProperty("card", card);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setBackground(new Color(22,22,22));
        b.setForeground(FG);
        b.setBorder(new EmptyBorder(9,10,9,10));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return b;
    }

    private void highlightNav(String active) {
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            e.getValue().setBackground(e.getKey().equals(active) ? new Color(52,52,52) : new Color(22,22,22));
        }
    }

    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;
        sidebar.setPreferredSize(new Dimension(sidebarCollapsed ? 72 : 224, 100));
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JButton b) {
                Object full = b.getClientProperty("fullText");
                Object shortText = b.getClientProperty("shortText");
                if (full != null && shortText != null) {
                    b.setText(sidebarCollapsed ? shortText.toString() : full.toString());
                    b.setHorizontalAlignment(sidebarCollapsed ? SwingConstants.CENTER : SwingConstants.LEFT);
                }
            }
        }
        sidebar.revalidate();
        sidebar.repaint();
    }

    private JPanel page(String title, String subtitle) {
        JPanel wrap = new JPanel(new BorderLayout(12,12));
        wrap.setBackground(BG);
        wrap.setBorder(new EmptyBorder(16,18,16,18));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel h = label(title, 24, Font.BOLD);
        JLabel s = new JLabel(subtitle);
        s.setForeground(MUTED);
        head.add(h);
        head.add(Box.createVerticalStrut(3));
        head.add(s);
        wrap.add(head, BorderLayout.NORTH);
        return wrap;
    }

    private JPanel buildGuidePanel() {
        JPanel wrap = page("Novo Projeto", "Escolha o conteúdo, o estilo e o destino. Depois copie as instruções para seu assistente de IA.");

        JPanel main = new JPanel(new GridLayout(1,2,12,0));
        main.setOpaque(false);

        JPanel left = panelBox();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("Tipo de conteúdo", 13, Font.BOLD));
        styleCombo(contentType);
        left.add(contentType);
        left.add(Box.createVerticalStrut(12));

        left.add(label("Estilo de edição", 13, Font.BOLD));
        styleCombo(editStyle);
        left.add(editStyle);
        left.add(Box.createVerticalStrut(12));

        left.add(label("Destino do vídeo", 13, Font.BOLD));
        styleCombo(destination);
        left.add(destination);
        left.add(Box.createVerticalStrut(18));

        left.add(label("Como você quer o resultado?", 13, Font.BOLD));
        guidedDescription.setLineWrap(true);
        guidedDescription.setWrapStyleWord(true);
        guidedDescription.setRows(8);
        guidedDescription.setText("Ex.: quero ritmo médio, cortar pausas, usar zoom leve nas frases fortes, imagens de apoio e música bem baixa.");
        styleTextArea(guidedDescription, true);
        left.add(new JScrollPane(guidedDescription));
        left.add(Box.createVerticalStrut(12));

        JButton add = button("ADICIONAR ARQUIVOS AO PROJETO");
        add.addActionListener(e -> addAssets());
        left.add(add);

        JPanel right = panelBox();
        right.setLayout(new BorderLayout(0,12));
        JPanel intro = new JPanel();
        intro.setOpaque(false);
        intro.setLayout(new BoxLayout(intro, BoxLayout.Y_AXIS));
        intro.add(label("Converse com uma IA compatível", 20, Font.BOLD));
        intro.add(Box.createVerticalStrut(8));
        intro.add(infoArea("O Koda Cut copia uma explicação pronta do aplicativo e um mapa dos arquivos do projeto. Você cola isso no assistente que preferir e conversa normalmente sobre a edição.", 6));
        right.add(intro, BorderLayout.NORTH);

        JPanel steps = new JPanel();
        steps.setOpaque(false);
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        steps.add(commercialStep("1", "Copiar instruções para IA", "Explica a lógica do Koda Cut sem citar marcas externas."));
        steps.add(Box.createVerticalStrut(8));
        steps.add(commercialStep("2", "Copiar projeto para IA", "Inclui vídeo, imagens, áudios, B-rolls e suas preferências."));
        steps.add(Box.createVerticalStrut(8));
        steps.add(commercialStep("3", "Converse sobre o vídeo", "Peça mudanças até ficar satisfeito."));
        steps.add(Box.createVerticalStrut(8));
        steps.add(commercialStep("4", "Colar edição no Koda Cut", "Volte à Edição Automática e renderize."));
        right.add(steps, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(3,1,8,8));
        actions.setOpaque(false);
        JButton copyInstructions = button("COPIAR INSTRUCOES PARA IA");
        stylePrimary(copyInstructions);
        copyInstructions.addActionListener(e -> copyAiInstructions());
        JButton copyProject = button("COPIAR PROJETO PARA IA");
        copyProject.addActionListener(e -> copyProjectForAssistant());
        JButton openEditor = button("IR PARA EDICAO AUTOMATICA");
        openEditor.addActionListener(e -> { cardLayout.show(cards, "AUTO"); highlightNav("AUTO"); });
        actions.add(copyInstructions);
        actions.add(copyProject);
        actions.add(openEditor);
        right.add(actions, BorderLayout.SOUTH);

        main.add(left);
        main.add(right);
        wrap.add(main, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel commercialStep(String number, String title, String description) {
        JPanel p = new JPanel(new BorderLayout(12,0));
        p.setBackground(new Color(26,26,26));
        p.setBorder(new EmptyBorder(12,12,12,12));
        JLabel n = new JLabel(number, SwingConstants.CENTER);
        n.setOpaque(true);
        n.setBackground(new Color(212,175,55));
        n.setForeground(Color.BLACK);
        n.setFont(new Font("SansSerif", Font.BOLD, 16));
        n.setPreferredSize(new Dimension(36,36));
        p.add(n, BorderLayout.WEST);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(label(title, 14, Font.BOLD));
        JLabel d = new JLabel(description);
        d.setForeground(MUTED);
        text.add(d);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTutorialPanel() {
        JPanel wrap = page("Como usar", "Tutorial rápido do Koda Cut para novos clientes.");
        JPanel p = panelBox();
        p.setLayout(new BorderLayout(18,18));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(label("Aprenda em poucos minutos", 26, Font.BOLD));
        copy.add(Box.createVerticalStrut(10));
        copy.add(infoArea("O tutorial mostra, em slides, como adicionar arquivos, preparar o projeto para um assistente de IA, conversar sobre a edição e renderizar o resultado.", 5));
        copy.add(Box.createVerticalStrut(18));
        copy.add(commercialStep("1", "Adicione seus arquivos", "Vídeo, imagens, PNGs, áudios, músicas e B-rolls."));
        copy.add(Box.createVerticalStrut(8));
        copy.add(commercialStep("2", "Copie as instruções", "O app prepara o contexto necessário para a IA."));
        copy.add(Box.createVerticalStrut(8));
        copy.add(commercialStep("3", "Descreva o resultado", "Use linguagem normal e exemplos."));
        copy.add(Box.createVerticalStrut(8));
        copy.add(commercialStep("4", "Cole e renderize", "Escolha o formato e gere o vídeo."));
        p.add(copy, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(3,1,8,8));
        actions.setOpaque(false);
        JButton tutorial = button("ABRIR TUTORIAL EM SLIDES");
        stylePrimary(tutorial);
        tutorial.addActionListener(e -> OnboardingDialog.showAlways(this, root));
        JButton instructions = button("COPIAR INSTRUCOES PARA IA");
        instructions.addActionListener(e -> copyAiInstructions());
        JButton project = button("COPIAR PROJETO PARA IA");
        project.addActionListener(e -> copyProjectForAssistant());
        actions.add(tutorial);
        actions.add(instructions);
        actions.add(project);
        p.add(actions, BorderLayout.SOUTH);

        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private void copyAiInstructions() {
        copyText(assistantInstructions(), "Instruções copiadas. Cole no assistente de IA que preferir.");
    }

    private void copyProjectForAssistant() {
        StringBuilder sb = new StringBuilder();
        sb.append(assistantInstructions()).append("\n\n");
        sb.append("KODA CUT - PROJETO ATUAL\n\n");
        sb.append("Tipo de conteúdo: ").append(Objects.toString(contentType.getSelectedItem(), "")).append("\n");
        sb.append("Estilo de edição: ").append(Objects.toString(editStyle.getSelectedItem(), "")).append("\n");
        sb.append("Destino: ").append(Objects.toString(destination.getSelectedItem(), "")).append("\n");
        sb.append("Pedido do cliente: ").append(guidedDescription.getText().trim()).append("\n\n");
        sb.append("MAPA DE ARQUIVOS DISPONÍVEIS\n");
        sb.append(buildManifestText()).append("\n");
        sb.append("Converse comigo normalmente para ajustar a edição. Quando eu disser GERAR EDICAO, entregue somente o KodaScript JSON final compatível com o Koda Cut.");
        copyText(sb.toString(), "Projeto copiado. Agora cole no assistente de IA e explique como quer seu vídeo.");
    }

    private String assistantInstructions() {
        return """
Você vai me ajudar a preparar uma edição para o Koda Cut.

O Koda Cut é um editor automático que executa instruções estruturadas chamadas KodaScript. Eu vou conversar com você normalmente sobre como quero o vídeo.

Seu trabalho é:
- entender o tipo de conteúdo, o estilo, o ritmo e o formato desejado;
- analisar o material que eu enviar quando ele estiver disponível;
- usar SOMENTE os arquivos presentes no MAPA DE ARQUIVOS;
- decidir onde usar cortes, zoom, shake, freeze frame, textos, legendas, imagens, PNGs, B-rolls, efeitos sonoros e música;
- manter fala, legibilidade e elementos importantes do vídeo como prioridade;
- usar timestamps reais do vídeo;
- nunca inventar nomes de assets que não estejam no mapa;
- quando eu disser GERAR EDICAO, entregar somente um KodaScript JSON compatível com o Koda Cut.

Exemplo de IDs:
image:logo
image:meme_chocado
audio:impacto
audio:risada
video:broll_praia

Você pode conversar comigo e sugerir ajustes antes de gerar a edição final.
""";
    }

    private void copyText(String text, String message) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        JOptionPane.showMessageDialog(this, message, "Koda Cut", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel buildHomePanel() {
        JPanel wrap = page("Koda Cut", "Crie vídeos para diferentes formatos e estilos em um fluxo simples.");

        JPanel top = new JPanel(new BorderLayout(12,12));
        top.setOpaque(false);
        JPanel hero = panelBox();
        hero.setLayout(new BorderLayout(18,18));
        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        heroText.add(label("Comece um novo projeto", 28, Font.BOLD));
        heroText.add(Box.createVerticalStrut(8));
        JTextArea desc = infoArea("Adicione seu conteúdo, escolha o tipo de vídeo e o estilo de edição. O Koda Cut organiza o projeto e prepara tudo para você conversar com um assistente de IA compatível.", 4);
        heroText.add(desc);
        JButton start = button("NOVO PROJETO");
        stylePrimary(start);
        start.addActionListener(e -> { cardLayout.show(cards, "GUIA"); highlightNav("GUIA"); });
        hero.add(heroText, BorderLayout.CENTER);
        hero.add(start, BorderLayout.SOUTH);
        top.add(hero, BorderLayout.CENTER);

        JPanel grid = new JPanel(new GridLayout(2,3,12,12));
        grid.setOpaque(false);
        grid.add(homeCard("EDICAO AUTOMATICA", "Traga a edição gerada pelo assistente e renderize localmente.", "AUTO"));
        grid.add(homeCard("CANAL DE CORTES", "Transforme conteúdo autorizado em vários cortes.", "CORTES"));
        grid.add(homeCard("SHORTS / REELS", "Prepare conteúdo vertical com safe zones e legendas.", "SHORTS"));
        grid.add(homeCard("ELEMENTOS", "Organize imagens, vídeos, áudios, músicas e B-rolls.", "ELEMENTOS"));
        grid.add(homeCard("MEUS ESTILOS", "Escolha perfis de edição ou personalize o seu.", "ESTILOS"));
        grid.add(homeCard("COMO USAR", "Veja o tutorial rápido novamente a qualquer momento.", "TUTORIAL"));

        JPanel body = new JPanel(new BorderLayout(0,12));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(grid, BorderLayout.CENTER);
        wrap.add(body, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel homeCard(String title, String desc, String target) {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(18,18,18,18));
        JLabel h = label(title, 16, Font.BOLD);
        JTextArea d = infoArea(desc, 4);
        JButton go = button("ABRIR");
        go.addActionListener(e -> {
            cardLayout.show(cards, target);
            highlightNav(target);
        });
        p.add(h, BorderLayout.NORTH);
        p.add(d, BorderLayout.CENTER);
        p.add(go, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildAutoPanel() {
        JPanel wrap = page("Edicao Automatica", "Adicione o material, cole o KodaScript gerado pelo assistente de IA e clique em renderizar.");

        JPanel main = new JPanel(new GridLayout(1,2,12,0));
        main.setOpaque(false);

        JPanel left = panelBox();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("Video principal", 13, Font.BOLD));
        left.add(Box.createVerticalStrut(6));
        left.add(fileRow(mainVideoField, "ADICIONAR VIDEO", e -> chooseMainVideo()));
        left.add(Box.createVerticalStrut(12));

        JButton addAssets = button("ADICIONAR IMAGENS / VIDEOS / AUDIOS");
        addAssets.addActionListener(e -> addAssets());
        JButton map = button("COPIAR MAPA P/ CHATGPT");
        map.addActionListener(e -> copyManifest());
        left.add(addAssets);
        left.add(Box.createVerticalStrut(7));
        left.add(map);
        left.add(Box.createVerticalStrut(16));

        left.add(label("Perfil", 13, Font.BOLD));
        styleCombo(stylePreset);
        stylePreset.addActionListener(e -> {
            Object selected = stylePreset.getSelectedItem();
            if (selected != null && !"Personalizado".equals(selected.toString())) applyProfile(selected.toString());
        });
        left.add(stylePreset);
        left.add(Box.createVerticalStrut(10));

        left.add(label("Formato", 13, Font.BOLD));
        styleCombo(outputMode);
        left.add(outputMode);
        left.add(Box.createVerticalStrut(7));
        styleCombo(verticalMode);
        left.add(verticalMode);
        left.add(Box.createVerticalStrut(10));

        left.add(label("Pasta final", 13, Font.BOLD));
        outputField.setText(root.resolve("final").toString());
        left.add(fileRow(outputField, "ESCOLHER", e -> chooseOutput()));
        left.add(Box.createVerticalGlue());

        JPanel right = panelBox();
        right.setLayout(new BorderLayout(0,8));
        JPanel rh = new JPanel();
        rh.setOpaque(false);
        rh.setLayout(new BoxLayout(rh, BoxLayout.Y_AXIS));
        rh.add(label("Prompt de edicao / KodaScript", 13, Font.BOLD));
        JLabel help = new JLabel("Aceita JSON puro ou JSON dentro de bloco de codigo.");
        help.setForeground(MUTED);
        rh.add(help);
        right.add(rh, BorderLayout.NORTH);

        styleTextArea(promptArea, true);
        right.add(new JScrollPane(promptArea), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        buttons.setOpaque(false);
        JButton load = button("CARREGAR");
        JButton save = button("SALVAR");
        JButton example = button("EXEMPLO");
        load.addActionListener(e -> loadPrompt());
        save.addActionListener(e -> savePrompt());
        example.addActionListener(e -> loadExample());
        buttons.add(load);
        buttons.add(save);
        buttons.add(example);
        right.add(buttons, BorderLayout.SOUTH);

        main.add(left);
        main.add(right);
        wrap.add(main, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1,3,8,0));
        actions.setOpaque(false);
        stylePrimary(renderButton);
        renderButton.addActionListener(e -> renderMain());
        setupButton.addActionListener(e -> runSetup());
        styleSecondary(setupButton);
        JButton folder = new JButton("ABRIR PASTA FINAL");
        styleSecondary(folder);
        folder.addActionListener(e -> openPath(Paths.get(outputField.getText().trim())));
        actions.add(renderButton);
        actions.add(setupButton);
        actions.add(folder);
        wrap.add(actions, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildCutsPanel() {
        JPanel wrap = page("Canal de Cortes", "Cole um link autorizado do YouTube ou escolha um arquivo local. O Koda Cut transcreve e procura os melhores trechos.");

        JPanel form = panelBox();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        form.add(label("Link do YouTube", 13, Font.BOLD));
        form.add(Box.createVerticalStrut(5));
        styleField(cutsUrlField);
        form.add(cutsUrlField);
        form.add(Box.createVerticalStrut(8));
        styleCheck(rightsCheck);
        form.add(rightsCheck);
        form.add(Box.createVerticalStrut(12));

        form.add(label("Ou arquivo local", 13, Font.BOLD));
        form.add(Box.createVerticalStrut(5));
        form.add(fileRow(cutsSourceField, "ESCOLHER VIDEO", e -> chooseCutsSource()));
        form.add(Box.createVerticalStrut(12));

        JPanel opts = new JPanel(new GridLayout(2,5,8,8));
        opts.setOpaque(false);
        opts.add(fieldGroup("Quantidade", cutCount));
        opts.add(fieldGroup("Min. segundos", cutMin));
        opts.add(fieldGroup("Max. segundos", cutMax));
        opts.add(fieldGroup("Formato", cutFormat));
        opts.add(fieldGroup("Legendas", cutCaptionMode));
        opts.add(new JPanel());
        opts.add(new JPanel());
        opts.add(new JPanel());
        opts.add(new JPanel());
        opts.add(new JPanel());
        form.add(opts);
        form.add(Box.createVerticalStrut(12));

        form.add(label("Prompt do canal de cortes", 13, Font.BOLD));
        JLabel h = new JLabel("Ex.: Gere 8 cortes de 40 a 70 segundos, vertical, legenda completa, comece direto na melhor frase.");
        h.setForeground(MUTED);
        form.add(h);
        form.add(Box.createVerticalStrut(6));
        styleTextArea(cutsPromptArea, true);
        cutsPromptArea.setText("Gere cortes com inicio forte, sem introducao longa, remova trechos mortos e mantenha contexto suficiente para entender a fala.");
        JScrollPane sp = new JScrollPane(cutsPromptArea);
        sp.setPreferredSize(new Dimension(300,160));
        form.add(sp);

        wrap.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1,3,8,0));
        actions.setOpaque(false);
        JButton importBtn = button("IMPORTAR LINK");
        JButton transcriptBtn = button("TRANSCREVER");
        JButton generateBtn = new JButton("GERAR CORTES");
        stylePrimary(generateBtn);
        importBtn.addActionListener(e -> importCutsUrl(null));
        transcriptBtn.addActionListener(e -> transcribeCutsSource());
        generateBtn.addActionListener(e -> generateCuts());
        actions.add(importBtn);
        actions.add(transcriptBtn);
        actions.add(generateBtn);
        wrap.add(actions, BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildPresetPanel(String title, String desc, String profile) {
        JPanel wrap = page(title, desc);

        JPanel center = panelBox();
        center.setLayout(new BorderLayout(12,12));
        JTextArea detail = infoArea(profileDescription(profile), 12);
        detail.setFont(new Font("SansSerif", Font.PLAIN, 15));
        center.add(detail, BorderLayout.CENTER);

        JButton apply = new JButton("USAR ESTE PERFIL");
        stylePrimary(apply);
        apply.addActionListener(e -> {
            applyProfile(profile);
            cardLayout.show(cards, "AUTO");
            highlightNav("AUTO");
        });
        center.add(apply, BorderLayout.SOUTH);

        wrap.add(center, BorderLayout.CENTER);
        return wrap;
    }

    private String profileDescription(String profile) {
        return switch (profile) {
            case "Gameplay / Meme" ->
                "• Legendas de destaque\n• Zoom e freeze nos beats do KodaScript\n• PNGs/memes/SFX por timestamp\n• Voz limpa, jogo preservado\n• 1080p60 e NVENC\n\nIdeal para gameplay, reacts e videos com humor.";
            case "Dark / Narrado" ->
                "• Legenda completa por padrao\n• Whisper local\n• B-roll e imagens da biblioteca\n• Trilha baixa com ducking\n• Ritmo limpo, sem efeitos exagerados\n\nIdeal para historias, curiosidades, documentarios curtos e canais dark.";
            case "Podcast / Cortes" ->
                "• Transcricao completa\n• Canal de Cortes com selecao automatica\n• Vertical ou horizontal\n• Legenda completa ou destaques\n• Inicio do clipe puxado para a frase mais forte\n\nIdeal para podcasts, entrevistas e lives.";
            case "Shorts / Reels" ->
                "• 1080x1920\n• Safe zone para botoes das plataformas\n• Fundo borrado ou crop\n• Legendas grandes\n• Render rapido com NVENC\n\nIdeal para Reels, Shorts, TikTok e Stories.";
            default ->
                "Perfil de edicao do Koda Cut.";
        };
    }

    private JPanel buildAssetsPanel() {
        JPanel wrap = page("Elementos", "Biblioteca permanente de videos, B-rolls, PNGs, fotos, logos, memes, musicas e SFX.");

        JPanel p = panelBox();
        p.setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        top.setOpaque(false);
        JButton add = button("ADICIONAR ARQUIVOS");
        JButton remove = button("REMOVER");
        JButton reload = button("RECARREGAR");
        JButton copy = button("COPIAR MAPA P/ CHATGPT");
        JButton folder = button("ABRIR BIBLIOTECA");
        add.addActionListener(e -> addAssets());
        remove.addActionListener(e -> removeSelectedAsset());
        reload.addActionListener(e -> scanLibrary());
        copy.addActionListener(e -> copyManifest());
        folder.addActionListener(e -> openPath(root.resolve("biblioteca")));
        top.add(add); top.add(remove); top.add(reload); top.add(copy); top.add(folder);
        p.add(top, BorderLayout.NORTH);

        assetTable.setBackground(FIELD);
        assetTable.setForeground(FG);
        assetTable.setGridColor(LINE);
        assetTable.setSelectionBackground(new Color(70,70,70));
        assetTable.setSelectionForeground(Color.WHITE);
        assetTable.setRowHeight(27);
        assetTable.getTableHeader().setReorderingAllowed(false);
        p.add(new JScrollPane(assetTable), BorderLayout.CENTER);

        JTextArea help = infoArea(
            "Cada arquivo recebe um ID estavel. Ex.: boom.wav → audio:boom, logo.png → image:logo, praia.mp4 → video:praia. " +
            "O assistente de IA usa esses IDs no KodaScript, entao o motor sabe exatamente qual arquivo colocar, em qual segundo e por quanto tempo.", 4);
        p.add(help, BorderLayout.SOUTH);

        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildCaptionsPanel() {
        JPanel wrap = page("Legendas", "Whisper roda localmente. Escolha entre legenda completa, destaques ou palavra por palavra.");

        JPanel p = panelBox();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        styleCheck(autoTranscribe);
        styleCheck(safeZone);
        p.add(autoTranscribe);
        p.add(Box.createVerticalStrut(8));
        p.add(fieldGroup("Modo de legenda", captionMode));
        p.add(Box.createVerticalStrut(8));
        p.add(fieldGroup("Idioma", language));
        p.add(Box.createVerticalStrut(8));
        p.add(safeZone);
        p.add(Box.createVerticalStrut(16));

        JTextArea help = infoArea(
            "Completa: legenda todas as falas detectadas.\n\n" +
            "Destaques: usa heuristicas para escolher frases mais expressivas; combina com gameplay/meme.\n\n" +
            "Palavra por palavra: gera karaoke ASS local, distribuindo o tempo da frase entre as palavras para destacar a palavra atual.\n\n" +
            "O Whisper usa um modelo local e nao consome creditos de IA.", 10);
        p.add(help);
        p.add(Box.createVerticalStrut(12));

        JButton transcribe = button("TRANSCREVER VIDEO PRINCIPAL AGORA");
        transcribe.addActionListener(e -> transcribeMainVideo());
        p.add(transcribe);

        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildAudioPanel() {
        JPanel wrap = page("Audio", "Tratamento local da voz, normalizacao, ducking e controle de musica.");

        JPanel p = panelBox();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        styleCheck(noiseReduction);
        styleCheck(normalizeAudio);
        styleCheck(ducking);
        p.add(noiseReduction);
        p.add(Box.createVerticalStrut(8));
        p.add(normalizeAudio);
        p.add(Box.createVerticalStrut(8));
        p.add(ducking);
        p.add(Box.createVerticalStrut(12));
        p.add(fieldGroup("Volume da voz", voiceVolume));
        p.add(Box.createVerticalStrut(8));
        p.add(fieldGroup("Volume padrao da musica", musicVolume));
        p.add(Box.createVerticalStrut(16));
        p.add(infoArea(
            "Ducking abaixa a trilha quando existe voz no video. O KodaScript ainda pode controlar volumes de SFX e musicas individualmente.", 5));
        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildFormatPanel() {
        JPanel wrap = page("Formato", "Um mesmo projeto pode gerar YouTube, Reels/Stories/Shorts e quadrado.");

        JComboBox<String> formatOutput = new JComboBox<>(new String[]{
            "Horizontal 16:9 (YouTube)",
            "Vertical 9:16 (Reels / Shorts / Stories)",
            "Quadrado 1:1",
            "Horizontal + Vertical",
            "Gerar todos"
        });
        JComboBox<String> formatVertical = new JComboBox<>(new String[]{
            "Preservar video + fundo borrado",
            "Crop central 9:16"
        });
        JCheckBox formatSafe = new JCheckBox("Safe zone para Reels/TikTok", safeZone.isSelected());

        formatOutput.setSelectedIndex(outputMode.getSelectedIndex());
        formatVertical.setSelectedIndex(verticalMode.getSelectedIndex());
        formatOutput.addActionListener(e -> outputMode.setSelectedIndex(formatOutput.getSelectedIndex()));
        formatVertical.addActionListener(e -> verticalMode.setSelectedIndex(formatVertical.getSelectedIndex()));
        formatSafe.addActionListener(e -> safeZone.setSelected(formatSafe.isSelected()));

        outputMode.addActionListener(e -> {
            if (formatOutput.getSelectedIndex() != outputMode.getSelectedIndex()) {
                formatOutput.setSelectedIndex(outputMode.getSelectedIndex());
            }
        });
        verticalMode.addActionListener(e -> {
            if (formatVertical.getSelectedIndex() != verticalMode.getSelectedIndex()) {
                formatVertical.setSelectedIndex(verticalMode.getSelectedIndex());
            }
        });
        safeZone.addActionListener(e -> formatSafe.setSelected(safeZone.isSelected()));

        JPanel p = panelBox();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(fieldGroup("Saida", formatOutput));
        p.add(Box.createVerticalStrut(10));
        p.add(fieldGroup("Vertical", formatVertical));
        p.add(Box.createVerticalStrut(10));
        styleCheck(formatSafe);
        p.add(formatSafe);
        p.add(Box.createVerticalStrut(16));
        p.add(infoArea(
            "Horizontal: 1920x1080.\nVertical: 1080x1920.\nQuadrado: 1080x1080.\n\n" +
            "No vertical, o modo Preservar mantem todo o gameplay e preenche o fundo com blur. O modo Crop ocupa a tela inteira cortando as laterais.", 8));
        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildStylesPanel() {
        JPanel wrap = page("Estilos", "Perfis mudam as configuracoes padrao; o KodaScript continua podendo mandar instrucoes especificas.");

        JComboBox<String> stylesPagePreset = new JComboBox<>(new String[]{
            "Personalizado", "Gameplay / Meme", "Dark / Narrado",
            "Podcast / Cortes", "Shorts / Reels", "Clean / Documentario", "Cinematico"
        });
        stylesPagePreset.setSelectedItem(stylePreset.getSelectedItem());
        stylesPagePreset.addActionListener(e -> {
            Object selected = stylesPagePreset.getSelectedItem();
            if (selected != null) applyProfile(selected.toString());
        });
        stylePreset.addActionListener(e -> stylesPagePreset.setSelectedItem(stylePreset.getSelectedItem()));

        JPanel p = panelBox();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(fieldGroup("Perfil atual", stylesPagePreset));
        p.add(Box.createVerticalStrut(12));

        JButton gameplay = button("APLICAR GAMEPLAY / MEME");
        JButton dark = button("APLICAR DARK / NARRADO");
        JButton podcast = button("APLICAR PODCAST / CORTES");
        JButton shorts = button("APLICAR SHORTS / REELS");
        gameplay.addActionListener(e -> applyProfile("Gameplay / Meme"));
        dark.addActionListener(e -> applyProfile("Dark / Narrado"));
        podcast.addActionListener(e -> applyProfile("Podcast / Cortes"));
        shorts.addActionListener(e -> applyProfile("Shorts / Reels"));
        p.add(gameplay); p.add(Box.createVerticalStrut(7));
        p.add(dark); p.add(Box.createVerticalStrut(7));
        p.add(podcast); p.add(Box.createVerticalStrut(7));
        p.add(shorts);

        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildBatchPanel() {
        JPanel wrap = page("Lote", "Aplique o mesmo KodaScript a varios videos sem iniciar cada render manualmente.");

        JPanel p = panelBox();
        p.setLayout(new BorderLayout(8,8));
        batchList.setBackground(FIELD);
        batchList.setForeground(FG);
        p.add(new JScrollPane(batchList), BorderLayout.CENTER);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0));
        top.setOpaque(false);
        JButton add = button("ADICIONAR VIDEOS");
        JButton remove = button("REMOVER");
        JButton clear = button("LIMPAR");
        add.addActionListener(e -> addBatchVideos());
        remove.addActionListener(e -> {
            for (Path path : batchList.getSelectedValuesList()) batchModel.removeElement(path);
        });
        clear.addActionListener(e -> batchModel.clear());
        top.add(add); top.add(remove); top.add(clear);
        p.add(top, BorderLayout.NORTH);

        JButton render = new JButton("RENDERIZAR LOTE");
        stylePrimary(render);
        render.addActionListener(e -> renderBatch());
        p.add(render, BorderLayout.SOUTH);

        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildRenderPanel() {
        JPanel wrap = page("Renderizacao", "Presets pensados para o seu Ryzen 5 4500 + GTX 1660 Super + 16 GB.");

        JPanel controls = panelBox();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(fieldGroup("Qualidade", quality));
        controls.add(Box.createVerticalStrut(14));

        JButton setup = button("CONFIGURAR / ATUALIZAR FERRAMENTAS");
        JButton nvenc = button("TESTAR NVIDIA NVENC");
        JButton finalFolder = button("ABRIR PASTA FINAL");
        JButton cache = button("LIMPAR CACHE");
        setup.addActionListener(e -> runSetup());
        nvenc.addActionListener(e -> testNvenc());
        finalFolder.addActionListener(e -> openPath(root.resolve("final")));
        cache.addActionListener(e -> clearCache());
        controls.add(setup); controls.add(Box.createVerticalStrut(7));
        controls.add(nvenc); controls.add(Box.createVerticalStrut(7));
        controls.add(finalFolder); controls.add(Box.createVerticalStrut(7));
        controls.add(cache); controls.add(Box.createVerticalStrut(14));

        controls.add(infoArea(
            "Balanceado usa H.264 NVENC quando disponivel. A GTX 1660 Super faz a codificacao e reduz a carga da CPU. " +
            "O Koda Cut evita arquivos intermediarios grandes e limpa a pasta temp para poupar o SSD de 256 GB.", 6));

        JPanel console = panelBox();
        console.setLayout(new BorderLayout(0,8));
        console.add(label("Console / progresso", 13, Font.BOLD), BorderLayout.NORTH);
        logArea.setEditable(false);
        logArea.setBackground(new Color(8,8,8));
        logArea.setForeground(new Color(215,215,215));
        logArea.setCaretColor(FG);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        console.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls, console);
        split.setResizeWeight(0.34);
        split.setDividerLocation(380);
        split.setBorder(null);
        wrap.add(split, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSettingsPanel() {
        JPanel wrap = page("Configuracoes", "Ferramentas locais, pastas e informacoes do projeto.");

        JPanel p = panelBox();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JTextArea paths = infoArea(
            "Raiz: " + root + "\n" +
            "FFmpeg: ffmpeg\\bin\\ffmpeg.exe\n" +
            "yt-dlp: tools\\yt-dlp.exe\n" +
            "Whisper: tools\\whisper\\whisper-cli.exe\n" +
            "Modelo: tools\\whisper\\models\\ggml-base.bin\n" +
            "Downloads: downloads\\\n" +
            "Transcricoes: transcricoes\\\n" +
            "Saidas: final\\", 9);
        p.add(paths);
        p.add(Box.createVerticalStrut(10));

        JButton setup = button("CONFIGURAR TUDO");
        JButton map = button("COPIAR MAPA DE ARQUIVOS");
        JButton console = button("ABRIR CONSOLE / LOG");
        setup.addActionListener(e -> runSetup());
        map.addActionListener(e -> copyManifest());
        console.addActionListener(e -> {
            cardLayout.show(cards, "RENDER");
            highlightNav("RENDER");
        });
        p.add(setup); p.add(Box.createVerticalStrut(7));
        p.add(map); p.add(Box.createVerticalStrut(7));
        p.add(console);

        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel panelBox() {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(16,16,16,16));
        return p;
    }

    private JTextArea infoArea(String text, int rows) {
        JTextArea a = new JTextArea(text);
        a.setEditable(false);
        a.setWrapStyleWord(true);
        a.setLineWrap(true);
        a.setRows(rows);
        a.setBackground(new Color(16,16,16));
        a.setForeground(new Color(210,210,210));
        a.setBorder(new EmptyBorder(10,10,10,10));
        return a;
    }

    private JPanel fieldGroup(String name, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0,5));
        p.setOpaque(false);
        p.add(label(name, 12, Font.BOLD), BorderLayout.NORTH);
        if (c instanceof JComboBox<?> combo) styleCombo(combo);
        if (c instanceof JTextField tf) styleField(tf);
        if (c instanceof JSpinner sp) styleSpinner(sp);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    private JLabel label(String text, int size, int style) {
        JLabel l = new JLabel(text);
        l.setForeground(FG);
        l.setFont(new Font("SansSerif", style, size));
        return l;
    }

    private JPanel fileRow(JTextField f, String text, java.awt.event.ActionListener a) {
        styleField(f);
        JButton b = button(text);
        b.addActionListener(a);
        JPanel p = new JPanel(new BorderLayout(8,0));
        p.setOpaque(false);
        p.add(f, BorderLayout.CENTER);
        p.add(b, BorderLayout.EAST);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));
        return p;
    }

    private JButton button(String text) {
        JButton b = new JButton(text);
        styleSecondary(b);
        return b;
    }

    private void stylePrimary(JButton b) {
        b.setFocusPainted(false);
        b.setBackground(Color.WHITE);
        b.setForeground(Color.BLACK);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setBorder(new EmptyBorder(12,14,12,14));
    }

    private void styleSecondary(JButton b) {
        b.setFocusPainted(false);
        b.setBackground(new Color(43,43,43));
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(9,12,9,12));
    }

    private void styleField(JTextField f) {
        f.setBackground(FIELD);
        f.setForeground(FG);
        f.setCaretColor(FG);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(LINE),
            new EmptyBorder(8,8,8,8)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));
    }

    private void styleTextArea(JTextArea a, boolean mono) {
        a.setBackground(FIELD);
        a.setForeground(FG);
        a.setCaretColor(FG);
        a.setFont(new Font(mono ? Font.MONOSPACED : "SansSerif", Font.PLAIN, 12));
        a.setLineWrap(!mono);
        a.setWrapStyleWord(!mono);
    }

    private void styleCombo(JComboBox<?> c) {
        c.setBackground(FIELD);
        c.setForeground(FG);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
    }

    private void styleSpinner(JSpinner s) {
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE,36));
    }

    private void styleCheck(JCheckBox c) {
        c.setOpaque(false);
        c.setForeground(FG);
        c.setFocusPainted(false);
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

    private void chooseCutsSource() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path p = fc.getSelectedFile().toPath();
            if (!"video".equals(classify(p))) {
                error("Escolha um arquivo de video.");
                return;
            }
            cutsSourceField.setText(p.toAbsolutePath().toString());
        }
    }

    private void chooseOutput() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputField.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void addBatchVideos() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        for (File f : fc.getSelectedFiles()) {
            Path p = f.toPath().toAbsolutePath();
            if ("video".equals(classify(p)) && !batchModel.contains(p)) batchModel.addElement(p);
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
        found.sort(Comparator.comparing((Asset a) -> a.type).thenComparing(a -> a.file.getFileName().toString().toLowerCase(Locale.ROOT)));
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
                    out.add(new Asset(id, type, p, humanSize(Files.size(p))));
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
        if ("video".equals(type)) return root.resolve("biblioteca/videos");
        if ("image".equals(type)) return root.resolve("biblioteca/imagens");
        return root.resolve("biblioteca/audios");
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
            "Mapa do projeto copiado. Cole junto das instruções no assistente de sua preferência.",
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

    private Path saveManifestInternal(Path video) throws IOException {
        Path p = root.resolve("projetos/assets.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        boolean first = true;
        if (video != null) {
            sb.append("  \"video:principal\": {\"type\":\"video\",\"path\":\"")
              .append(jsonEscape(video.toAbsolutePath().toString()))
              .append("\",\"name\":\"")
              .append(jsonEscape(video.getFileName().toString()))
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
        Path p = root.resolve("projetos/exemplo-geral.json");
        try {
            if (Files.exists(p)) promptArea.setText(Files.readString(p, StandardCharsets.UTF_8));
            else promptArea.setText(minimalProject());
        } catch (IOException e) {
            promptArea.setText(minimalProject());
        }
    }

    private String minimalProject() {
        return "{\n" +
            "  \"version\": 3,\n" +
            "  \"fps\": 60,\n" +
            "  \"duracao_saida\": 0,\n" +
            "  \"musica\": {\"asset\":\"\", \"volume\":0.08},\n" +
            "  \"timeline\": []\n" +
            "}";
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

    private String extractJson(String raw) {
        String text = raw.trim();
        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first >= 0 && last > first) return text.substring(first, last + 1).trim();
        return text;
    }

    private Path savePromptInternal() throws IOException {
        String normalized = extractJson(promptArea.getText());
        if (!normalized.startsWith("{") || !normalized.endsWith("}")) {
            throw new IOException("O KodaScript precisa conter um JSON. O assistente de IA pode enviar o JSON dentro de bloco de codigo.");
        }
        Path p = root.resolve("projetos/ultimo.json");
        Files.writeString(p, normalized, StandardCharsets.UTF_8);
        return p;
    }

    private void savePrompt() {
        try {
            Path p = savePromptInternal();
            Path video = null;
            if (!mainVideoField.getText().trim().isEmpty()) video = Paths.get(mainVideoField.getText().trim());
            saveManifestInternal(video);
            append("[OK] KodaScript salvo em: " + p);
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private String outputModeCode() {
        return switch (outputMode.getSelectedIndex()) {
            case 1 -> "vertical";
            case 2 -> "quadrado";
            case 3 -> "horizontal_vertical";
            case 4 -> "todos";
            default -> "horizontal";
        };
    }

    private String verticalModeCode() {
        return verticalMode.getSelectedIndex() == 1 ? "crop" : "blur";
    }

    private String qualityCode() {
        if (quality.getSelectedIndex() == 1) return "eco";
        if (quality.getSelectedIndex() == 2) return "qualidade";
        return "balanceado";
    }

    private String captionModeCode() {
        return switch (captionMode.getSelectedIndex()) {
            case 1 -> "completa";
            case 2 -> "destaques";
            case 3 -> "palavra";
            default -> "off";
        };
    }

    private String cutCaptionCode() {
        return switch (cutCaptionMode.getSelectedIndex()) {
            case 1 -> "destaques";
            case 2 -> "off";
            default -> "completa";
        };
    }

    private String currentStyleCode() {
        Object o = stylePreset.getSelectedItem();
        if (o == null) return "personalizado";
        String s = o.toString();
        if (s.startsWith("Gameplay")) return "gameplay";
        if (s.startsWith("Dark")) return "dark";
        if (s.startsWith("Podcast")) return "podcast";
        if (s.startsWith("Shorts")) return "shorts";
        if (s.startsWith("Clean")) return "clean";
        if (s.startsWith("Cinematico")) return "cinematico";
        return "personalizado";
    }

    private void applyProfile(String profile) {
        if (!Objects.equals(stylePreset.getSelectedItem(), profile)) stylePreset.setSelectedItem(profile);
        switch (profile) {
            case "Gameplay / Meme" -> {
                captionMode.setSelectedItem("Destaques");
                outputMode.setSelectedIndex(0);
                safeZone.setSelected(true);
                noiseReduction.setSelected(true);
                normalizeAudio.setSelected(true);
                ducking.setSelected(true);
            }
            case "Dark / Narrado" -> {
                captionMode.setSelectedItem("Completa");
                outputMode.setSelectedIndex(1);
                verticalMode.setSelectedIndex(0);
                safeZone.setSelected(true);
                ducking.setSelected(true);
            }
            case "Podcast / Cortes" -> {
                captionMode.setSelectedItem("Completa");
                outputMode.setSelectedIndex(1);
                safeZone.setSelected(true);
            }
            case "Shorts / Reels" -> {
                captionMode.setSelectedItem("Completa");
                outputMode.setSelectedIndex(1);
                verticalMode.setSelectedIndex(0);
                safeZone.setSelected(true);
            }
            case "Clean / Documentario" -> {
                captionMode.setSelectedItem("Completa");
                outputMode.setSelectedIndex(0);
                safeZone.setSelected(false);
            }
            case "Cinematico" -> {
                captionMode.setSelectedItem("Desligada");
                outputMode.setSelectedIndex(0);
                safeZone.setSelected(false);
            }
            default -> {}
        }
    }

    private void runSetup() {
        runProcess(List.of(
            "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
            root.resolve("scripts/configurar.ps1").toString()
        ), "Configuracao concluida.", null);
    }

    private void renderMain() {
        String video = mainVideoField.getText().trim();
        if (video.isEmpty() || !Files.exists(Paths.get(video))) {
            error("Adicione um video principal valido.");
            return;
        }
        renderSingle(Paths.get(video), null);
    }

    private void renderSingle(Path video, Runnable onDone) {
        try {
            Path project = savePromptInternal();
            Path manifest = saveManifestInternal(video);
            Path out = Paths.get(outputField.getText().trim().isBlank() ? root.resolve("final").toString() : outputField.getText().trim());
            Files.createDirectories(out);

            List<String> cmd = new ArrayList<>();
            Collections.addAll(cmd,
                "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
                root.resolve("scripts/editor.ps1").toString(),
                "-Video",video.toAbsolutePath().toString(),
                "-Project",project.toString(),
                "-Manifest",manifest.toString(),
                "-OutputMode",outputModeCode(),
                "-VerticalMode",verticalModeCode(),
                "-Quality",qualityCode(),
                "-OutputDir",out.toString(),
                "-CaptionMode",captionModeCode(),
                "-AutoTranscribe",Boolean.toString(autoTranscribe.isSelected()),
                "-Language",Objects.toString(language.getSelectedItem(),"pt"),
                "-SafeZone",Boolean.toString(safeZone.isSelected()),
                "-NoiseReduction",Boolean.toString(noiseReduction.isSelected()),
                "-NormalizeAudio",Boolean.toString(normalizeAudio.isSelected()),
                "-Ducking",Boolean.toString(ducking.isSelected()),
                "-VoiceVolume",voiceVolume.getValue().toString(),
                "-DefaultMusicVolume",musicVolume.getValue().toString(),
                "-Style",currentStyleCode()
            );
            runProcess(cmd, "Renderizacao finalizada.", onDone);
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void renderBatch() {
        if (batchModel.isEmpty()) {
            error("Adicione videos ao lote.");
            return;
        }
        try {
            savePromptInternal();
        } catch (IOException e) {
            error(e.getMessage());
            return;
        }

        List<Path> queue = Collections.list(batchModel.elements());
        renderBatchAt(queue, 0);
    }

    private void renderBatchAt(List<Path> queue, int index) {
        if (index >= queue.size()) {
            append("[OK] Lote concluido.");
            JOptionPane.showMessageDialog(this, "Lote concluido.", "Koda Cut", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Path video = queue.get(index);
        append("[LOTE] " + (index + 1) + "/" + queue.size() + " -> " + video.getFileName());
        renderSingle(video, () -> renderBatchAt(queue, index + 1));
    }

    private void importCutsUrl(Runnable after) {
        String url = cutsUrlField.getText().trim();
        if (url.isEmpty()) {
            error("Cole o link do video.");
            return;
        }
        if (!rightsCheck.isSelected()) {
            error("Confirme que voce tem permissao para usar/reutilizar o conteudo.");
            return;
        }

        List<String> cmd = List.of(
            "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
            root.resolve("scripts/importar-youtube.ps1").toString(),
            "-Url",url,
            "-OutputDir",root.resolve("downloads").toString()
        );

        runProcess(cmd, "Importacao concluida.", () -> {
            Path newest = newestVideo(root.resolve("downloads"));
            if (newest != null) {
                SwingUtilities.invokeLater(() -> cutsSourceField.setText(newest.toString()));
                append("[OK] Fonte de cortes: " + newest);
                if (after != null) after.run();
            } else {
                append("[ERRO] Nao encontrei o video importado.");
            }
        });
    }

    private Path newestVideo(Path dir) {
        try {
            return Files.list(dir)
                .filter(Files::isRegularFile)
                .filter(p -> "video".equals(classify(p)))
                .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private void transcribeMainVideo() {
        String v = mainVideoField.getText().trim();
        if (v.isEmpty() || !Files.exists(Paths.get(v))) {
            error("Escolha o video principal.");
            return;
        }
        transcribe(Paths.get(v));
    }

    private void transcribeCutsSource() {
        String v = cutsSourceField.getText().trim();
        if (!v.isEmpty() && Files.exists(Paths.get(v))) {
            transcribe(Paths.get(v));
            return;
        }
        if (!cutsUrlField.getText().trim().isEmpty()) {
            importCutsUrl(() -> {
                String imported = cutsSourceField.getText().trim();
                if (!imported.isEmpty()) transcribe(Paths.get(imported));
            });
            return;
        }
        error("Escolha um arquivo ou cole um link autorizado.");
    }

    private void transcribe(Path video) {
        List<String> cmd = List.of(
            "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
            root.resolve("scripts/transcrever.ps1").toString(),
            "-Video",video.toAbsolutePath().toString(),
            "-Language",Objects.toString(language.getSelectedItem(),"pt"),
            "-OutputDir",root.resolve("transcricoes").toString()
        );
        runProcess(cmd, "Transcricao concluida.", null);
    }

    private void generateCuts() {
        String source = cutsSourceField.getText().trim();
        if (!source.isEmpty() && Files.exists(Paths.get(source))) {
            generateCutsFrom(Paths.get(source));
            return;
        }
        if (!cutsUrlField.getText().trim().isEmpty()) {
            importCutsUrl(() -> {
                String imported = cutsSourceField.getText().trim();
                if (!imported.isEmpty()) generateCutsFrom(Paths.get(imported));
            });
            return;
        }
        error("Escolha um arquivo local ou cole um link autorizado.");
    }

    private void generateCutsFrom(Path source) {
        try {
            Path promptFile = root.resolve("projetos/cortes-prompt.txt");
            Files.writeString(promptFile, cutsPromptArea.getText(), StandardCharsets.UTF_8);

            String format = cutFormat.getSelectedIndex() == 0 ? "vertical" : "horizontal";
            List<String> cmd = List.of(
                "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
                root.resolve("scripts/cortes.ps1").toString(),
                "-Video",source.toAbsolutePath().toString(),
                "-PromptFile",promptFile.toString(),
                "-Count",cutCount.getValue().toString(),
                "-MinSeconds",cutMin.getValue().toString(),
                "-MaxSeconds",cutMax.getValue().toString(),
                "-Format",format,
                "-VerticalMode",verticalModeCode(),
                "-CaptionMode",cutCaptionCode(),
                "-Language",Objects.toString(language.getSelectedItem(),"pt"),
                "-Quality",qualityCode(),
                "-OutputDir",root.resolve("final/cortes").toString()
            );
            runProcess(cmd, "Cortes gerados.", () -> openPath(root.resolve("final/cortes")));
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void testNvenc() {
        Path bat = root.resolve("TESTAR-NVENC.bat");
        if (Files.exists(bat)) {
            runProcess(List.of("cmd.exe","/c",bat.toString()), "Teste NVENC finalizado.", null);
        } else {
            error("TESTAR-NVENC.bat nao encontrado.");
        }
    }

    private void clearCache() {
        Path temp = root.resolve("temp");
        try {
            if (Files.exists(temp)) {
                try (var stream = Files.walk(temp)) {
                    stream.sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(temp))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                        });
                }
            }
            Files.createDirectories(temp);
            append("[OK] Cache limpo.");
        } catch (IOException e) {
            error(e.getMessage());
        }
    }

    private void runProcess(List<String> cmd, String success, Runnable afterSuccess) {
        setBusy(true);
        append("");
        append("> " + String.join(" ", cmd));

        new Thread(() -> {
            int code = -1;
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(root.toFile());
                pb.redirectErrorStream(true);
                Process p = pb.start();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) append(line);
                }
                code = p.waitFor();
                append(code == 0 ? "[OK] " + success : "[ERRO] Processo saiu com codigo " + code);
            } catch (Exception e) {
                append("[ERRO] " + e.getMessage());
            } finally {
                final int result = code;
                SwingUtilities.invokeLater(() -> {
                    setBusy(false);
                    if (result == 0 && afterSuccess != null) afterSuccess.run();
                });
            }
        }, "koda-cut-worker").start();
    }

    private void setBusy(boolean busy) {
        SwingUtilities.invokeLater(() -> {
            renderButton.setEnabled(!busy);
            setupButton.setEnabled(!busy);
        });
    }

    private void openPath(Path p) {
        try {
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
        private final String[] cols = {"ID para o KodaScript", "Tipo", "Arquivo", "Tamanho"};
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
