package kodaconnect;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KodaConnect extends JFrame {
    private static final Color BG = new Color(10,10,10);
    private static final Color PANEL = new Color(22,22,22);
    private static final Color FIELD = new Color(33,33,33);
    private static final Color FG = new Color(245,245,245);
    private static final Color MUTED = new Color(155,155,155);
    private static final Color GOLD = new Color(212,175,55);

    private final Path root;
    private final Path workspace;
    private final int port = 17777;

    private HttpServer server;
    private final ExecutorService serverPool = Executors.newCachedThreadPool();
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    private final JLabel serverState = stateLabel("Iniciando...");
    private final JLabel engineState = stateLabel("Procurando...");
    private final JLabel workspaceState = stateLabel("Preparando...");
    private final JLabel bridgeState = stateLabel("Local");
    private final JLabel currentStage = new JLabel("Aguardando pedido");
    private final JProgressBar progress = new JProgressBar(0,100);
    private final JTextArea logArea = new JTextArea();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KodaConnect app = new KodaConnect();
            app.setVisible(true);
        });
    }

    public KodaConnect() {
        root = detectRoot();
        workspace = root.resolve("KodaConnectWorkspace");
        ensureWorkspace();
        buildUi();
        detectEngine();
        startServer();
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

    private void ensureWorkspace() {
        String[] dirs = {"videos","images","audio","music","broll","projects","outputs","temp","logs"};
        try {
            Files.createDirectories(workspace);
            for (String d : dirs) Files.createDirectories(workspace.resolve(d));
            workspaceState.setText("Pronta");
        } catch (IOException e) {
            workspaceState.setText("Erro");
            append("[ERRO] Workspace: " + e.getMessage());
        }
    }

    private void buildUi() {
        setTitle("Koda Connect • Beta");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980,680));
        setSize(1120,760);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.setBorder(new EmptyBorder(18,22,12,22));
        JLabel brand = new JLabel("K  Koda Connect");
        brand.setForeground(FG);
        brand.setFont(new Font("SansSerif",Font.BOLD,28));
        header.add(brand,BorderLayout.WEST);
        JLabel beta = new JLabel("BETA • conexão local para edição assistida");
        beta.setForeground(GOLD);
        beta.setFont(new Font("SansSerif",Font.BOLD,12));
        header.add(beta,BorderLayout.EAST);
        add(header,BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(14,14));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(0,22,20,22));

        JPanel statuses = new JPanel(new GridLayout(1,4,10,10));
        statuses.setOpaque(false);
        statuses.add(statusCard("Servidor local",serverState));
        statuses.add(statusCard("Motor de edição",engineState));
        statuses.add(statusCard("Workspace",workspaceState));
        statuses.add(statusCard("Ponte",bridgeState));
        center.add(statuses,BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1,2,14,0));
        body.setOpaque(false);

        JPanel left = panel();
        left.setLayout(new BoxLayout(left,BoxLayout.Y_AXIS));
        left.add(title("Conexão",20));
        left.add(Box.createVerticalStrut(8));
        left.add(info("Abra o Koda Connect e deixe esta janela rodando. O servidor local recebe pedidos de edição e usa o motor do Koda Cut no seu próprio PC."));
        left.add(Box.createVerticalStrut(18));

        JButton test = button("TESTAR CONEXÃO LOCAL",true);
        test.addActionListener(e -> testLocal());
        JButton copy = button("COPIAR ENDEREÇO LOCAL",false);
        copy.addActionListener(e -> copy("http://127.0.0.1:"+port));
        JButton open = button("ABRIR WORKSPACE",false);
        open.addActionListener(e -> open(workspace));
        JButton copyConfig = button("COPIAR CONFIGURAÇÃO DO CONECTOR",false);
        copyConfig.addActionListener(e -> copy(connectorInfo()));

        left.add(test);
        left.add(Box.createVerticalStrut(8));
        left.add(copy);
        left.add(Box.createVerticalStrut(8));
        left.add(open);
        left.add(Box.createVerticalStrut(8));
        left.add(copyConfig);
        left.add(Box.createVerticalStrut(20));

        left.add(title("Fluxo do beta",15));
        left.add(Box.createVerticalStrut(8));
        left.add(info("1. Coloque vídeos e assets na Workspace.\n2. O conector lista os arquivos autorizados.\n3. O chat envia o plano/KodaScript.\n4. Koda Connect inicia o render.\n5. O chat consulta o progresso até terminar."));

        JPanel right = panel();
        right.setLayout(new BorderLayout(0,12));

        JPanel progressBox = new JPanel();
        progressBox.setOpaque(false);
        progressBox.setLayout(new BoxLayout(progressBox,BoxLayout.Y_AXIS));
        JLabel pt = title("Status da edição",20);
        progressBox.add(pt);
        progressBox.add(Box.createVerticalStrut(8));
        currentStage.setForeground(FG);
        currentStage.setFont(new Font("SansSerif",Font.BOLD,15));
        progressBox.add(currentStage);
        progressBox.add(Box.createVerticalStrut(10));
        progress.setStringPainted(true);
        progress.setValue(0);
        progress.setForeground(GOLD);
        progress.setBackground(FIELD);
        progressBox.add(progress);
        right.add(progressBox,BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setBackground(new Color(15,15,15));
        logArea.setForeground(new Color(220,220,220));
        logArea.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));
        right.add(new JScrollPane(logArea),BorderLayout.CENTER);

        JButton clear = button("LIMPAR LOG",false);
        clear.addActionListener(e -> logArea.setText(""));
        right.add(clear,BorderLayout.SOUTH);

        body.add(left);
        body.add(right);
        center.add(body,BorderLayout.CENTER);
        add(center,BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { stopServer(); }
        });
    }

    private JPanel statusCard(String name, JLabel state) {
        JPanel p = panel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        JLabel n = new JLabel(name);
        n.setForeground(MUTED);
        n.setFont(new Font("SansSerif",Font.PLAIN,12));
        state.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(n);
        p.add(Box.createVerticalStrut(6));
        p.add(state);
        return p;
    }

    private static JLabel stateLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif",Font.BOLD,14));
        return l;
    }

    private JPanel panel() {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(18,18,18,18));
        return p;
    }

    private JLabel title(String text,int size) {
        JLabel l = new JLabel(text);
        l.setForeground(FG);
        l.setFont(new Font("SansSerif",Font.BOLD,size));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextArea info(String text) {
        JTextArea a = new JTextArea(text);
        a.setEditable(false);
        a.setOpaque(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setForeground(MUTED);
        a.setFont(new Font("SansSerif",Font.PLAIN,13));
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        a.setMaximumSize(new Dimension(Integer.MAX_VALUE,120));
        return a;
    }

    private JButton button(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));
        b.setFont(new Font("SansSerif",Font.BOLD,12));
        b.setBorder(new EmptyBorder(11,14,11,14));
        if (primary) {
            b.setBackground(GOLD);
            b.setForeground(Color.BLACK);
        } else {
            b.setBackground(FIELD);
            b.setForeground(FG);
        }
        return b;
    }

    private void detectEngine() {
        Path editor = root.resolve("scripts").resolve("editor.ps1");
        if (Files.exists(editor)) {
            engineState.setText("Encontrado");
            engineState.setForeground(new Color(130,220,150));
        } else {
            engineState.setText("Não encontrado");
            engineState.setForeground(new Color(235,145,100));
        }
    }

    private void startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1",port),0);
            server.createContext("/health", this::health);
            server.createContext("/api/status", this::status);
            server.createContext("/api/files", this::files);
            server.createContext("/api/projects", this::projects);
            server.createContext("/api/render", this::render);
            server.createContext("/api/jobs", this::jobStatus);
            server.createContext("/api/cancel", this::cancelJob);
            server.setExecutor(serverPool);
            server.start();
            serverState.setText("Conectado • " + port);
            serverState.setForeground(new Color(130,220,150));
            append("[OK] Servidor local: http://127.0.0.1:"+port);
            append("[OK] Workspace: " + workspace);
        } catch (IOException e) {
            serverState.setText("Erro");
            serverState.setForeground(new Color(235,100,100));
            append("[ERRO] Servidor: " + e.getMessage());
        }
    }

    private void stopServer() {
        if (server != null) server.stop(0);
        serverPool.shutdownNow();
    }

    private void health(HttpExchange ex) throws IOException {
        if (!method(ex,"GET")) return;
        json(ex,200,"{\"ok\":true,\"app\":\"Koda Connect\",\"version\":\"beta\",\"port\":"+port+"}");
    }

    private void status(HttpExchange ex) throws IOException {
        if (!method(ex,"GET")) return;
        json(ex,200,"{\"server\":\"online\",\"engine\":"+q(Files.exists(root.resolve("scripts/editor.ps1"))?"ready":"missing")+",\"workspace\":"+q(workspace.toString())+"}");
    }

    private void files(HttpExchange ex) throws IOException {
        if (!method(ex,"GET")) return;
        List<FileEntry> entries = scanWorkspace();
        StringBuilder sb = new StringBuilder("{\"files\":[");
        for (int i=0;i<entries.size();i++) {
            if (i>0) sb.append(',');
            FileEntry f=entries.get(i);
            sb.append("{\"id\":").append(q(f.id)).append(",\"type\":").append(q(f.type)).append(",\"name\":").append(q(f.path.getFileName().toString())).append(",\"relative\":").append(q(workspace.relativize(f.path).toString())).append("}");
        }
        sb.append("]}");
        json(ex,200,sb.toString());
    }

    private void projects(HttpExchange ex) throws IOException {
        if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
            List<String> names = new ArrayList<>();
            try (var s=Files.list(workspace.resolve("projects"))) {
                s.filter(Files::isRegularFile).filter(p->p.getFileName().toString().toLowerCase().endsWith(".json")).forEach(p->names.add(p.getFileName().toString()));
            }
            json(ex,200,"{\"projects\":"+jsonArray(names)+"}");
            return;
        }
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            String body = readBody(ex);
            if (!body.trim().startsWith("{")) { json(ex,400,"{\"error\":\"Expected KodaScript JSON body\"}"); return; }
            String name = "project-"+System.currentTimeMillis()+".json";
            Files.writeString(workspace.resolve("projects").resolve(name),body,StandardCharsets.UTF_8);
            json(ex,201,"{\"ok\":true,\"project\":"+q(name)+"}");
            return;
        }
        json(ex,405,"{\"error\":\"method_not_allowed\"}");
    }

    private void render(HttpExchange ex) throws IOException {
        if (!method(ex,"POST")) return;
        if (!Files.exists(root.resolve("scripts/editor.ps1"))) {
            json(ex,503,"{\"error\":\"Koda Cut editor engine not found\"}");
            return;
        }
        String body=readBody(ex);
        String videoName = field(body,"video");
        String projectName = field(body,"project");
        String outputMode = optional(field(body,"outputMode"),"horizontal");
        String verticalMode = optional(field(body,"verticalMode"),"blur");
        String quality = optional(field(body,"quality"),"balanceado");
        String captionMode = optional(field(body,"captionMode"),"destaques");
        String language = optional(field(body,"language"),"pt");
        String style = optional(field(body,"style"),"clean");

        Path video = resolveAllowedVideo(videoName);
        Path project = resolveProject(projectName);
        if (video==null || project==null) {
            json(ex,400,"{\"error\":\"video or project not found in authorized workspace\"}");
            return;
        }

        String id=UUID.randomUUID().toString();
        JobState job=new JobState(id,video.getFileName().toString());
        jobs.put(id,job);
        CompletableFuture.runAsync(() -> runRender(job,video,project,outputMode,verticalMode,quality,captionMode,language,style));
        json(ex,202,"{\"ok\":true,\"jobId\":"+q(id)+",\"status\":\"queued\"}");
    }

    private void jobStatus(HttpExchange ex) throws IOException {
        if (!method(ex,"GET")) return;
        Map<String,String> query=query(ex.getRequestURI());
        String id=query.get("id");
        JobState j=id==null?null:jobs.get(id);
        if (j==null) { json(ex,404,"{\"error\":\"job_not_found\"}"); return; }
        json(ex,200,j.toJson());
    }

    private void cancelJob(HttpExchange ex) throws IOException {
        if (!method(ex,"POST")) return;
        String id=query(ex.getRequestURI()).get("id");
        JobState j=id==null?null:jobs.get(id);
        if (j==null) { json(ex,404,"{\"error\":\"job_not_found\"}"); return; }
        j.cancelRequested=true;
        if (j.process!=null) j.process.destroy();
        j.stage="Cancelando";
        json(ex,200,j.toJson());
    }

    private void runRender(JobState job, Path video, Path project, String outputMode, String verticalMode, String quality, String captionMode, String language, String style) {
        try {
            setJob(job,"Preparando projeto",5);
            Path manifest=writeManifest(video);
            Path out=workspace.resolve("outputs").resolve("job-"+job.id);
            Files.createDirectories(out);

            List<String> cmd=new ArrayList<>();
            Collections.addAll(cmd,
                "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
                root.resolve("scripts/editor.ps1").toString(),
                "-Video",video.toAbsolutePath().toString(),
                "-Project",project.toAbsolutePath().toString(),
                "-Manifest",manifest.toAbsolutePath().toString(),
                "-OutputMode",outputMode,
                "-VerticalMode",verticalMode,
                "-Quality",quality,
                "-OutputDir",out.toAbsolutePath().toString(),
                "-CaptionMode",captionMode,
                "-AutoTranscribe","true",
                "-Language",language,
                "-SafeZone","true",
                "-NoiseReduction","true",
                "-NormalizeAudio","true",
                "-Ducking","true",
                "-VoiceVolume","1.0",
                "-DefaultMusicVolume","0.08",
                "-Style",style
            );

            ProcessBuilder pb=new ProcessBuilder(cmd);
            pb.directory(root.toFile());
            pb.redirectErrorStream(true);
            job.process=pb.start();
            setJob(job,"Analisando e preparando edição",15);

            try (BufferedReader br=new BufferedReader(new InputStreamReader(job.process.getInputStream(),StandardCharsets.UTF_8))) {
                String line;
                while ((line=br.readLine())!=null) {
                    job.lastLog=line;
                    append("[JOB "+job.id.substring(0,8)+"] "+line);
                    String l=line.toLowerCase(Locale.ROOT);
                    if (l.contains("whisper") || l.contains("transcri")) setJob(job,"Transcrevendo áudio",28);
                    else if (l.contains("legenda") || l.contains("subtitle")) setJob(job,"Aplicando legendas",48);
                    else if (l.contains("render") || l.contains("ffmpeg")) setJob(job,"Renderizando vídeo",72);
                    if (job.cancelRequested) break;
                }
            }
            int code=job.process.waitFor();
            job.process=null;
            if (job.cancelRequested) {
                job.status="cancelled"; job.stage="Cancelado"; job.progress=0; updateUi(job); return;
            }
            if (code==0) {
                setJob(job,"Finalizando arquivo",96);
                job.status="completed";
                job.stage="Concluído";
                job.progress=100;
                job.output=out.toString();
                updateUi(job);
                append("[OK] Render concluído: "+out);
            } else {
                job.status="failed";
                job.stage="Erro na renderização";
                job.error="process_exit_"+code;
                updateUi(job);
            }
        } catch (Exception e) {
            job.status="failed";
            job.stage="Erro";
            job.error=e.getMessage();
            updateUi(job);
            append("[ERRO] Job "+job.id+": "+e.getMessage());
        }
    }

    private Path writeManifest(Path video) throws IOException {
        List<FileEntry> entries=scanWorkspace();
        StringBuilder sb=new StringBuilder("{\n  \"video:principal\":{\"type\":\"video\",\"path\":"+q(video.toAbsolutePath().toString())+",\"name\":"+q(video.getFileName().toString())+"}");
        for (FileEntry f:entries) {
            if (f.path.equals(video)) continue;
            sb.append(",\n  ").append(q(f.id)).append(":{\"type\":").append(q(f.type)).append(",\"path\":").append(q(f.path.toAbsolutePath().toString())).append(",\"name\":").append(q(f.path.getFileName().toString())).append("}");
        }
        sb.append("\n}\n");
        Path p=workspace.resolve("temp").resolve("assets-"+System.currentTimeMillis()+".json");
        Files.writeString(p,sb.toString(),StandardCharsets.UTF_8);
        return p;
    }

    private List<FileEntry> scanWorkspace() {
        List<FileEntry> out=new ArrayList<>();
        scanType(out,workspace.resolve("videos"),"video");
        scanType(out,workspace.resolve("images"),"image");
        scanType(out,workspace.resolve("audio"),"audio");
        scanType(out,workspace.resolve("music"),"audio");
        scanType(out,workspace.resolve("broll"),"video");
        return out;
    }

    private void scanType(List<FileEntry> out, Path dir, String type) {
        try (var s=Files.list(dir)) {
            s.filter(Files::isRegularFile).sorted().forEach(p->out.add(new FileEntry(type+":"+slug(stem(p.getFileName().toString())),type,p)));
        } catch (IOException ignored) {}
    }

    private Path resolveAllowedVideo(String name) {
        if (name==null) return null;
        for (Path dir:List.of(workspace.resolve("videos"),workspace.resolve("broll"))) {
            Path p=dir.resolve(name).normalize();
            if (p.startsWith(dir) && Files.isRegularFile(p)) return p;
        }
        return null;
    }

    private Path resolveProject(String name) {
        if (name==null) return null;
        Path dir=workspace.resolve("projects");
        Path p=dir.resolve(name).normalize();
        return p.startsWith(dir) && Files.isRegularFile(p)?p:null;
    }

    private void setJob(JobState job,String stage,int value) {
        job.status="running";
        job.stage=stage;
        job.progress=Math.max(job.progress,value);
        updateUi(job);
    }

    private void updateUi(JobState job) {
        SwingUtilities.invokeLater(() -> {
            currentStage.setText(job.stage);
            progress.setValue(job.progress);
        });
    }

    private void testLocal() {
        if (server==null) {
            JOptionPane.showMessageDialog(this,"Servidor não está ativo.","Koda Connect",JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this,"Servidor local funcionando em 127.0.0.1:"+port+"\nMotor: "+engineState.getText()+"\nWorkspace: pronta","Koda Connect",JOptionPane.INFORMATION_MESSAGE);
    }

    private String connectorInfo() {
        return "KODA CONNECT BETA\n"+
            "Base URL local: http://127.0.0.1:"+port+"\n"+
            "Health: GET /health\n"+
            "Arquivos: GET /api/files\n"+
            "Projetos: GET/POST /api/projects\n"+
            "Render: POST /api/render\n"+
            "Progresso: GET /api/jobs?id={jobId}\n"+
            "Cancelar: POST /api/cancel?id={jobId}\n\n"+
            "Observação: esta é a API local do beta. Uma ponte segura/MCP remota ainda é necessária para um serviço de chat fora do PC acessar este endereço.";
    }

    private boolean method(HttpExchange ex,String expected) throws IOException {
        if (!expected.equalsIgnoreCase(ex.getRequestMethod())) {
            json(ex,405,"{\"error\":\"method_not_allowed\"}");
            return false;
        }
        return true;
    }

    private String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
    }

    private void json(HttpExchange ex,int code,String body) throws IOException {
        byte[] data=body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        ex.sendResponseHeaders(code,data.length);
        try(OutputStream os=ex.getResponseBody()){ os.write(data); }
    }

    private Map<String,String> query(URI uri) {
        Map<String,String> m=new HashMap<>();
        String q=uri.getRawQuery();
        if(q==null) return m;
        for(String part:q.split("&")){
            int i=part.indexOf('=');
            String k=i>=0?part.substring(0,i):part;
            String v=i>=0?part.substring(i+1):"";
            m.put(dec(k),dec(v));
        }
        return m;
    }

    private String field(String json,String name) {
        Pattern p=Pattern.compile("\\\""+Pattern.quote(name)+"\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
        Matcher m=p.matcher(json);
        return m.find()?unescape(m.group(1)):null;
    }

    private String optional(String v,String fallback){ return v==null||v.isBlank()?fallback:v; }
    private String dec(String s){ return URLDecoder.decode(s,StandardCharsets.UTF_8); }
    private String unescape(String s){ return s.replace("\\\"", "\"").replace("\\\\", "\\"); }

    private String jsonArray(List<String> values) {
        StringBuilder sb=new StringBuilder("[");
        for(int i=0;i<values.size();i++){ if(i>0)sb.append(','); sb.append(q(values.get(i))); }
        return sb.append(']').toString();
    }

    private String q(String s) {
        if(s==null) return "null";
        return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n") + "\"";
    }

    private String stem(String n){ int i=n.lastIndexOf('.'); return i>0?n.substring(0,i):n; }

    private String slug(String input) {
        String s=Normalizer.normalize(input,Normalizer.Form.NFD).replaceAll("\\p{M}","");
        s=s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$","");
        return s.isBlank()?"arquivo":s;
    }

    private void copy(String s) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s),null);
        JOptionPane.showMessageDialog(this,"Copiado.","Koda Connect",JOptionPane.INFORMATION_MESSAGE);
    }

    private void open(Path p) {
        try { Desktop.getDesktop().open(p.toFile()); }
        catch(Exception e){ append("[ERRO] Abrir pasta: "+e.getMessage()); }
    }

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s+System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private record FileEntry(String id,String type,Path path) {}

    private static final class JobState {
        final String id;
        final String video;
        final String created=Instant.now().toString();
        volatile String status="queued";
        volatile String stage="Na fila";
        volatile int progress=0;
        volatile String output="";
        volatile String error="";
        volatile String lastLog="";
        volatile boolean cancelRequested=false;
        volatile Process process;

        JobState(String id,String video){this.id=id;this.video=video;}

        String toJson() {
            return "{\"jobId\":"+qs(id)+",\"video\":"+qs(video)+",\"status\":"+qs(status)+",\"stage\":"+qs(stage)+",\"progress\":"+progress+",\"output\":"+qs(output)+",\"error\":"+qs(error)+",\"created\":"+qs(created)+"}";
        }

        private static String qs(String s) {
            if(s==null) return "null";
            return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n") + "\"";
        }
    }
}
