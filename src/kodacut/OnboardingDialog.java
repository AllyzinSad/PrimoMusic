package kodacut;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class OnboardingDialog extends JDialog {
    private static final Color BG = new Color(10,10,10);
    private static final Color CARD = new Color(20,20,20);
    private static final Color FIELD = new Color(30,30,30);
    private static final Color FG = new Color(246,246,246);
    private static final Color MUTED = new Color(165,165,165);
    private static final Color GOLD = new Color(212,175,55);
    private static final Color GOLD_SOFT = new Color(247,211,119);

    private final Path root;
    private final CardLayout layout = new CardLayout();
    private final JPanel cards = new JPanel(layout);
    private final JLabel progress = new JLabel();
    private final JButton back = new JButton("Voltar");
    private final JButton next = new JButton("Próximo  ›");
    private final JCheckBox dontShow = new JCheckBox("Não mostrar automaticamente novamente");
    private int index = 0;
    private final int total = 5;

    private OnboardingDialog(Window owner, Path root) {
        super(owner, "Tutorial rápido • Koda Cut", ModalityType.APPLICATION_MODAL);
        this.root = root;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1040, 700);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        cards.setBackground(BG);
        cards.add(slideWelcome(), "0");
        cards.add(slideAssets(), "1");
        cards.add(slideAssistant(), "2");
        cards.add(slideConversation(), "3");
        cards.add(slideRender(), "4");
        add(cards, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12,0));
        bottom.setBackground(new Color(14,14,14));
        bottom.setBorder(new EmptyBorder(14,24,14,24));

        dontShow.setOpaque(false);
        dontShow.setForeground(MUTED);
        dontShow.setFocusPainted(false);
        bottom.add(dontShow, BorderLayout.WEST);

        progress.setForeground(GOLD_SOFT);
        progress.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(progress, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        actions.setOpaque(false);
        styleSecondary(back);
        stylePrimary(next);
        JButton skip = new JButton("Pular tutorial");
        styleSecondary(skip);
        skip.addActionListener(e -> finish(false));
        back.addActionListener(e -> go(-1));
        next.addActionListener(e -> {
            if (index == total - 1) finish(true);
            else go(1);
        });
        actions.add(skip);
        actions.add(back);
        actions.add(next);
        bottom.add(actions, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);
        updateState();
    }

    public static void showIfNeeded(Window owner, Path root) {
        try {
            Path marker = marker(root);
            if (Files.exists(marker)) return;
        } catch (Exception ignored) {}
        new OnboardingDialog(owner, root).setVisible(true);
    }

    public static void showAlways(Window owner, Path root) {
        new OnboardingDialog(owner, root).setVisible(true);
    }

    private static Path marker(Path root) {
        return root.resolve("config").resolve("tutorial-concluido.txt");
    }

    private void go(int delta) {
        index = Math.max(0, Math.min(total - 1, index + delta));
        layout.show(cards, Integer.toString(index));
        updateState();
    }

    private void updateState() {
        back.setEnabled(index > 0);
        next.setText(index == total - 1 ? "Começar  ✓" : "Próximo  ›");
        StringBuilder dots = new StringBuilder();
        for (int i=0;i<total;i++) dots.append(i==index ? "● " : "○ ");
        progress.setText(dots.toString().trim());
    }

    private void finish(boolean completed) {
        if (completed || dontShow.isSelected()) {
            try {
                Files.createDirectories(marker(root).getParent());
                Files.writeString(marker(root), "ok", StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
        }
        dispose();
    }

    private JPanel slideWelcome() {
        return slide(
            "Bem-vindo ao Koda Cut",
            "Um fluxo simples para transformar seus materiais em um vídeo pronto.",
            new String[]{
                "1. Adicione o vídeo e os arquivos que podem entrar na edição.",
                "2. Copie as instruções do projeto para um assistente de IA compatível.",
                "3. Converse normalmente sobre o estilo que você deseja.",
                "4. Cole a edição gerada no Koda Cut e renderize."
            },
            "COMECE SEM COMPLICAÇÃO",
            "Você não precisa saber programação nem editar JSON manualmente.",
            0
        );
    }

    private JPanel slideAssets() {
        return slide(
            "Adicione seus arquivos",
            "O Koda Cut organiza tudo e cria IDs fáceis para a edição.",
            new String[]{
                "Vídeo principal: a gravação que será editada.",
                "Imagens e PNGs: logos, memes, artes e elementos visuais.",
                "Áudios e músicas: efeitos, trilhas e sons do seu projeto.",
                "B-rolls: vídeos extras para ilustrar assuntos e esconder cortes."
            },
            "EXEMPLO",
            "logo.png → image:logo   •   impacto.wav → audio:impacto   •   praia.mp4 → video:praia",
            1
        );
    }

    private JPanel slideAssistant() {
        return slide(
            "Copie as instruções do projeto",
            "O app prepara uma explicação pronta para o assistente entender a lógica do Koda Cut.",
            new String[]{
                "Use “Copiar instruções para IA” para explicar como o aplicativo funciona.",
                "Use “Copiar projeto para IA” para enviar o mapa dos arquivos disponíveis.",
                "O assistente deve usar apenas os arquivos listados no mapa.",
                "Nenhuma marca externa precisa aparecer dentro do Koda Cut."
            },
            "IMPORTANTE",
            "Você continua dono dos seus arquivos e escolhe qual serviço compatível deseja usar.",
            2
        );
    }

    private JPanel slideConversation() {
        return slide(
            "Descreva como quer o vídeo",
            "Converse de forma natural. O importante é explicar o resultado que você imagina.",
            new String[]{
                "Gameplay: “quero cortes rápidos, zoom em reações e poucos efeitos sonoros.”",
                "Opinião: “ritmo médio, cortes nas pausas, imagens de apoio e música baixa.”",
                "Podcast: “gere cortes verticais com legenda completa e gancho forte.”",
                "Produto: “faça um vídeo curto, limpo e focado nos detalhes do produto.”"
            },
            "DICA",
            "Você pode pedir mudanças quantas vezes quiser antes de gerar a edição final.",
            3
        );
    }

    private JPanel slideRender() {
        return slide(
            "Cole a edição e renderize",
            "Quando estiver satisfeito, traga as instruções de volta para o Koda Cut.",
            new String[]{
                "Cole a edição gerada no campo de edição automática.",
                "Confira formato, legenda, áudio e qualidade.",
                "Escolha Horizontal, Vertical, Quadrado ou gerar todos.",
                "Clique em Renderizar. O processamento acontece localmente no seu computador."
            },
            "PRONTO",
            "Depois, encontre os vídeos finalizados na pasta de saída escolhida.",
            4
        );
    }

    private JPanel slide(String title, String subtitle, String[] bullets, String callTitle, String callText, int artMode) {
        JPanel rootPanel = new JPanel(new BorderLayout(24,20));
        rootPanel.setBackground(BG);
        rootPanel.setBorder(new EmptyBorder(28,34,22,34));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel brand = new JLabel("K  Koda Cut");
        brand.setForeground(GOLD_SOFT);
        brand.setFont(new Font("SansSerif", Font.BOLD, 24));
        top.add(brand, BorderLayout.WEST);
        JLabel quick = new JLabel("Tutorial rápido  •  " + (artMode + 1) + "/" + total);
        quick.setForeground(MUTED);
        top.add(quick, BorderLayout.EAST);
        rootPanel.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1,2,28,0));
        center.setOpaque(false);

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(title);
        h.setForeground(FG);
        h.setFont(new Font("SansSerif", Font.BOLD, 34));
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(h);
        copy.add(Box.createVerticalStrut(8));

        JTextArea sub = text(subtitle, 16, MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(sub);
        copy.add(Box.createVerticalStrut(20));

        for (String bullet : bullets) {
            JPanel row = new JPanel(new BorderLayout(10,0));
            row.setOpaque(false);
            JLabel icon = new JLabel("●");
            icon.setForeground(GOLD);
            icon.setFont(new Font("SansSerif", Font.BOLD, 13));
            row.add(icon, BorderLayout.WEST);
            JTextArea t = text(bullet, 14, FG);
            row.add(t, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            copy.add(row);
            copy.add(Box.createVerticalStrut(4));
        }

        copy.add(Box.createVerticalGlue());
        JPanel call = new JPanel(new BorderLayout(8,4));
        call.setBackground(new Color(27,24,17));
        call.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100,78,22)),
            new EmptyBorder(12,14,12,14)
        ));
        JLabel ct = new JLabel(callTitle);
        ct.setForeground(GOLD_SOFT);
        ct.setFont(new Font("SansSerif", Font.BOLD, 12));
        call.add(ct, BorderLayout.NORTH);
        JTextArea cb = text(callText, 13, FG);
        call.add(cb, BorderLayout.CENTER);
        call.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(call);

        center.add(copy);
        center.add(new IllustrationPanel(artMode));
        rootPanel.add(center, BorderLayout.CENTER);
        return rootPanel;
    }

    private static JTextArea text(String s, int size, Color color) {
        JTextArea a = new JTextArea(s);
        a.setOpaque(false);
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setForeground(color);
        a.setFont(new Font("SansSerif", Font.PLAIN, size));
        a.setBorder(null);
        return a;
    }

    private static void stylePrimary(AbstractButton b) {
        b.setFocusPainted(false);
        b.setBackground(GOLD);
        b.setForeground(Color.BLACK);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(10,18,10,18));
    }

    private static void styleSecondary(AbstractButton b) {
        b.setFocusPainted(false);
        b.setBackground(FIELD);
        b.setForeground(FG);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(10,16,10,16));
    }

    private static final class IllustrationPanel extends JPanel {
        private final int mode;
        IllustrationPanel(int mode) {
            this.mode = mode;
            setOpaque(false);
            setPreferredSize(new Dimension(420,420));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g=(Graphics2D)graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g.setColor(CARD);
            g.fillRoundRect(8,8,w-16,h-16,26,26);
            g.setColor(new Color(70,58,26));
            g.drawRoundRect(8,8,w-16,h-16,26,26);

            if (mode==0) drawWorkflow(g,w,h);
            else if (mode==1) drawAssets(g,w,h);
            else if (mode==2) drawCopy(g,w,h);
            else if (mode==3) drawChat(g,w,h);
            else drawRender(g,w,h);
            g.dispose();
        }
        private void title(Graphics2D g,String s,int x,int y){
            g.setColor(GOLD_SOFT); g.setFont(new Font("SansSerif",Font.BOLD,18)); g.drawString(s,x,y);
        }
        private void drawWorkflow(Graphics2D g,int w,int h){
            title(g,"Seu fluxo de edição",32,50);
            String[] labels={"Arquivos","Instruções","Conversa","Render"};
            int y=100;
            for(int i=0;i<labels.length;i++){
                g.setColor(i==3?new Color(60,48,18):FIELD);
                g.fillRoundRect(48,y, w-96,58,16,16);
                g.setColor(i==3?GOLD_SOFT:FG);
                g.setFont(new Font("SansSerif",Font.BOLD,15));
                g.drawString((i+1)+"  "+labels[i],70,y+35);
                if(i<labels.length-1){
                    g.setColor(GOLD); g.drawLine(w/2,y+58,w/2,y+78);
                }
                y+=82;
            }
        }
        private void drawAssets(Graphics2D g,int w,int h){
            title(g,"Biblioteca do projeto",32,50);
            String[] types={"VÍDEO","IMAGEM / PNG","ÁUDIO","B-ROLL"};
            int x=32,y=88, bw=(w-78)/2,bh=112;
            for(int i=0;i<4;i++){
                int cx=x+(i%2)*(bw+14), cy=y+(i/2)*(bh+14);
                g.setColor(FIELD); g.fillRoundRect(cx,cy,bw,bh,18,18);
                g.setColor(GOLD); g.setFont(new Font("SansSerif",Font.BOLD,12)); g.drawString(types[i],cx+14,cy+26);
                g.setColor(new Color(75,75,75)); g.fillRoundRect(cx+14,cy+40,bw-28,50,12,12);
                g.setColor(MUTED); g.setFont(new Font("SansSerif",Font.PLAIN,11)); g.drawString(i==0?"clipe.mp4":i==1?"logo.png":i==2?"impacto.wav":"apoio.mp4",cx+22,cy+70);
            }
        }
        private void drawCopy(Graphics2D g,int w,int h){
            title(g,"Projeto preparado para compartilhar",32,50);
            g.setColor(FIELD); g.fillRoundRect(32,84,w-64,230,18,18);
            g.setColor(FG); g.setFont(new Font("Monospaced",Font.PLAIN,12));
            String[] lines={"video:principal = meu_video.mp4","image:logo = logo.png","audio:impacto = impacto.wav","video:apoio = apoio.mp4"};
            int y=122; for(String s:lines){ g.drawString(s,50,y); y+=32; }
            g.setColor(GOLD); g.fillRoundRect(80,334,w-160,48,14,14);
            g.setColor(Color.BLACK); g.setFont(new Font("SansSerif",Font.BOLD,13));
            g.drawString("COPIAR PROJETO",w/2-58,364);
        }
        private void drawChat(Graphics2D g,int w,int h){
            title(g,"Converse sobre a edição",32,50);
            bubble(g,54,92,w-115,84,true,"Quero um vídeo dinâmico, com cortes nas pausas, zoom leve e imagens de apoio.");
            bubble(g,92,194,w-130,108,false,"Entendido. Vou manter a fala como prioridade e usar somente os arquivos disponíveis no projeto.");
            g.setColor(FIELD); g.fillRoundRect(44,330,w-88,50,18,18);
            g.setColor(MUTED); g.setFont(new Font("SansSerif",Font.PLAIN,12)); g.drawString("Descreva como quer o seu vídeo...",62,360);
        }
        private void bubble(Graphics2D g,int x,int y,int bw,int bh,boolean gold,String s){
            g.setColor(gold?new Color(93,72,23):FIELD); g.fillRoundRect(x,y,bw,bh,18,18);
            g.setColor(gold?GOLD_SOFT:FG); g.setFont(new Font("SansSerif",Font.PLAIN,12));
            drawWrapped(g,s,x+14,y+25,bw-28,18);
        }
        private void drawRender(Graphics2D g,int w,int h){
            title(g,"Pronto para renderizar",32,50);
            g.setColor(FIELD); g.fillRoundRect(32,84,w-64,190,18,18);
            g.setColor(MUTED); g.setFont(new Font("SansSerif",Font.PLAIN,12));
            g.drawString("Formato",52,116);
            chip(g,52,132,98,"Horizontal",true);
            chip(g,160,132,82,"Vertical",false);
            chip(g,252,132,82,"Quadrado",false);
            g.drawString("Qualidade",52,207);
            chip(g,52,223,75,"Eco",false);
            chip(g,137,223,104,"Balanceado",true);
            chip(g,251,223,75,"Alta",false);
            g.setColor(GOLD); g.fillRoundRect(58,310,w-116,58,16,16);
            g.setColor(Color.BLACK); g.setFont(new Font("SansSerif",Font.BOLD,15)); g.drawString("▶  RENDERIZAR VÍDEO",w/2-82,346);
        }
        private void chip(Graphics2D g,int x,int y,int bw,String s,boolean active){
            g.setColor(active?new Color(80,62,20):new Color(42,42,42)); g.fillRoundRect(x,y,bw,42,12,12);
            g.setColor(active?GOLD_SOFT:FG); g.setFont(new Font("SansSerif",Font.BOLD,11)); g.drawString(s,x+12,y+26);
        }
        private void drawWrapped(Graphics2D g,String text,int x,int y,int maxWidth,int lineHeight){
            String[] words=text.split(" "); String line="";
            for(String word:words){
                String test=line.isEmpty()?word:line+" "+word;
                if(g.getFontMetrics().stringWidth(test)>maxWidth){
                    g.drawString(line,x,y); y+=lineHeight; line=word;
                } else line=test;
            }
            if(!line.isEmpty()) g.drawString(line,x,y);
        }
    }
}
