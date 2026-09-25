package kodacut;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KodaConnectServer {
    private final Path root;
    private final int port;
    private HttpServer server;
    private final String token;

    private volatile String renderState = "idle";
    private volatile String renderMessage = "Nenhuma renderização em andamento.";
    private volatile Process renderProcess;

    public KodaConnectServer(Path root, int port) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        this.port = port;
        this.token = loadOrCreateToken();
    }

    public synchronized void start() throws IOException {
        if (server != null) return;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/health", this::health);
        server.createContext("/api/projects", ex -> secure(ex, this::projects));
        server.createContext("/api/assets", ex -> secure(ex, this::assets));
        server.createContext("/api/render/status", ex -> secure(ex, this::renderStatus));
        server.createContext("/api/render", ex -> secure(ex, this::render));
        server.createContext("/api/cancel-render", ex -> secure(ex, this::cancelRender));
        server.createContext("/api/capabilities", ex -> secure(ex, this::capabilities));

        server.start();
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        Process p = renderProcess;
        if (p != null && p.isAlive()) p.destroy();
    }

    public boolean isRunning() { return server != null; }
    public int getPort() { return port; }
    public String getToken() { return token; }

    private void secure(HttpExchange ex, ExchangeHandler next) throws IOException {
        String provided = ex.getRequestHeaders().getFirst("X-Koda-Token");
        if (provided == null || !constantTimeEquals(token, provided)) {
            sendJson(ex, 401, "{"ok":false,"error":"unauthorized"}");
            return;
        }
        next.handle(ex);
    }

    private void health(HttpExchange ex) throws IOException {
        sendJson(ex, 200, "{"ok":true,"name":"Koda Connect","version":"0.5","local":true}");
    }

    private void capabilities(HttpExchange ex) throws IOException {
        requireMethod(ex, "GET");
        sendJson(ex, 200, """
            {
              "ok": true,
              "capabilities": [
                "list_projects",
                "list_assets",
                "render_video",
                "render_status",
                "cancel_render"
              ]
            }
            """);
    }

    private void projects(HttpExchange ex) throws IOException {
        requireMethod(ex, "GET");
        Path dir = root.resolve("projetos");
        Files.createDirectories(dir);
        List<Path> files = new ArrayList<>();
        try (var s = Files.list(dir)) {
            s.filter(Files::isRegularFile).sorted().forEach(files::add);
        }
        StringBuilder sb = new StringBuilder("{"ok":true,"projects":[");
        for (int i=0;i<files.size();i++) {
            if (i>0) sb.append(',');
            Path p = files.get(i);
            sb.append("{"name":"").append(esc(p.getFileName().toString())).append("",")
              .append(""path":"").append(esc(p.toString())).append(""}");
        }
        sb.append("]}");
        sendJson(ex, 200, sb.toString());
    }

    private void assets(HttpExchange ex) throws IOException {
        requireMethod(ex, "GET");
        List<Path> roots = List.of(
            root.resolve("biblioteca/videos"),
            root.resolve("biblioteca/imagens"),
            root.resolve("biblioteca/audios")
        );
        StringBuilder sb = new StringBuilder("{"ok":true,"assets":[");
        boolean first = true;
        for (Path dir : roots) {
            Files.createDirectories(dir);
            try (var s = Files.walk(dir, 1)) {
                for (Iterator<Path> it=s.filter(Files::isRegularFile).iterator(); it.hasNext();) {
                    Path p = it.next();
                    if (!first) sb.append(',');
                    first = false;
                    String type = classify(p);
                    sb.append("{"id":"").append(esc(assetId(type, p))).append("",")
                      .append(""type":"").append(type).append("",")
                      .append(""name":"").append(esc(p.getFileName().toString())).append("",")
                      .append(""path":"").append(esc(p.toString())).append(""}");
                }
            }
        }
        sb.append("]}");
        sendJson(ex, 200, sb.toString());
    }

    private void renderStatus(HttpExchange ex) throws IOException {
        requireMethod(ex, "GET");
        sendJson(ex, 200,
            "{"ok":true,"state":""+esc(renderState)+"","message":""+esc(renderMessage)+""}");
    }

    private synchronized void render(HttpExchange ex) throws IOException {
        requireMethod(ex, "POST");
        if (renderProcess != null && renderProcess.isAlive()) {
            sendJson(ex, 409, "{"ok":false,"error":"render_already_running"}");
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String,String> data = parseFlatJson(body);
        String video = data.getOrDefault("video", "").trim();
        String project = data.getOrDefault("project", root.resolve("projetos/ultimo.json").toString()).trim();
        String outputMode = data.getOrDefault("outputMode", "horizontal");
        String verticalMode = data.getOrDefault("verticalMode", "blur");
        String quality = data.getOrDefault("quality", "balanceado");
        String outputDir = data.getOrDefault("outputDir", root.resolve("final").toString());

        if (video.isEmpty()) {
            sendJson(ex, 400, "{"ok":false,"error":"video_required"}");
            return;
        }

        Path videoPath = resolveClientPath(video);
        Path projectPath = resolveClientPath(project);
        Path outPath = resolveClientPath(outputDir);

        if (!Files.exists(videoPath) || !Files.isRegularFile(videoPath)) {
            sendJson(ex, 400, "{"ok":false,"error":"video_not_found"}");
            return;
        }
        if (!Files.exists(projectPath) || !Files.isRegularFile(projectPath)) {
            sendJson(ex, 400, "{"ok":false,"error":"project_not_found"}");
            return;
        }

        Files.createDirectories(outPath);
        Path manifest = root.resolve("projetos/assets.json");
        if (!Files.exists(manifest)) {
            Files.writeString(manifest, "{"video:principal":{"type":"video","path":""+
                esc(videoPath.toString())+"","name":""+esc(videoPath.getFileName().toString())+""}}",
                StandardCharsets.UTF_8);
        }

        List<String> cmd = new ArrayList<>();
        Collections.addAll(cmd,
            "powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
            root.resolve("scripts/editor.ps1").toString(),
            "-Video",videoPath.toString(),
            "-Project",projectPath.toString(),
            "-Manifest",manifest.toString(),
            "-OutputMode",sanitizeEnum(outputMode, Set.of("horizontal","vertical","quadrado","horizontal_vertical","todos"), "horizontal"),
            "-VerticalMode",sanitizeEnum(verticalMode, Set.of("blur","crop"), "blur"),
            "-Quality",sanitizeEnum(quality, Set.of("eco","balanceado","qualidade"), "balanceado"),
            "-OutputDir",outPath.toString(),
            "-CaptionMode",data.getOrDefault("captionMode","destaques"),
            "-AutoTranscribe",data.getOrDefault("autoTranscribe","true"),
            "-Language",data.getOrDefault("language","pt"),
            "-SafeZone",data.getOrDefault("safeZone","true"),
            "-NoiseReduction",data.getOrDefault("noiseReduction","true"),
            "-NormalizeAudio",data.getOrDefault("normalizeAudio","true"),
            "-Ducking",data.getOrDefault("ducking","true"),
            "-VoiceVolume",data.getOrDefault("voiceVolume","1.0"),
            "-DefaultMusicVolume",data.getOrDefault("musicVolume","0.08"),
            "-Style",data.getOrDefault("style","personalizado")
        );

        renderState = "starting";
        renderMessage = "Preparando renderização.";

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(root.toFile());
        pb.redirectErrorStream(true);

        try {
            renderProcess = pb.start();
        } catch (IOException e) {
            renderState = "error";
            renderMessage = e.getMessage();
            throw e;
        }

        Thread worker = new Thread(() -> {
            renderState = "rendering";
            renderMessage = "Renderização em andamento.";
            String last = "";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(renderProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.isBlank()) last = line;
                }
                int code = renderProcess.waitFor();
                if (code == 0) {
                    renderState = "done";
                    renderMessage = "Renderização concluída.";
                } else {
                    renderState = "error";
                    renderMessage = last.isBlank() ? "Renderização falhou com código " + code : last;
                }
            } catch (Exception e) {
                renderState = "error";
                renderMessage = e.getMessage();
            } finally {
                renderProcess = null;
            }
        }, "koda-connect-render");
        worker.setDaemon(true);
        worker.start();

        sendJson(ex, 202, "{"ok":true,"state":"starting"}");
    }

    private synchronized void cancelRender(HttpExchange ex) throws IOException {
        requireMethod(ex, "POST");
        if (renderProcess != null && renderProcess.isAlive()) {
            renderProcess.destroy();
            renderState = "cancelled";
            renderMessage = "Renderização cancelada.";
            sendJson(ex, 200, "{"ok":true,"cancelled":true}");
        } else {
            sendJson(ex, 200, "{"ok":true,"cancelled":false}");
        }
    }

    private Path resolveClientPath(String raw) throws IOException {
        Path p = Paths.get(raw);
        if (!p.isAbsolute()) p = root.resolve(p);
        p = p.toAbsolutePath().normalize();

        Path allowedFile = root.resolve("config/allowed-folders.txt");
        Files.createDirectories(allowedFile.getParent());
        if (!Files.exists(allowedFile)) {
            Files.writeString(allowedFile,
                root.resolve("videos").toAbsolutePath().normalize()+System.lineSeparator()+
                root.resolve("biblioteca").toAbsolutePath().normalize()+System.lineSeparator()+
                root.resolve("projetos").toAbsolutePath().normalize()+System.lineSeparator()+
                root.resolve("final").toAbsolutePath().normalize()+System.lineSeparator(),
                StandardCharsets.UTF_8);
        }

        List<String> lines = Files.readAllLines(allowedFile, StandardCharsets.UTF_8);
        boolean allowed = false;
        for (String line : lines) {
            if (line.isBlank()) continue;
            try {
                Path base = Paths.get(line.trim()).toAbsolutePath().normalize();
                if (p.startsWith(base)) { allowed = true; break; }
            } catch (Exception ignored) {}
        }
        if (!allowed) throw new IOException("Acesso negado: caminho fora das pastas permitidas.");
        return p;
    }

    private String loadOrCreateToken() throws IOException {
        Path file = root.resolve("config/koda-connect-token.txt");
        Files.createDirectories(file.getParent());
        if (Files.exists(file)) {
            String existing = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (!existing.isEmpty()) return existing;
        }
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        String t = sb.toString();
        Files.writeString(file, t, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return t;
    }

    private static void requireMethod(HttpExchange ex, String expected) throws IOException {
        if (!expected.equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{"ok":false,"error":"method_not_allowed"}");
            throw new StopExchange();
        }
    }

    private static Map<String,String> parseFlatJson(String json) {
        Map<String,String> out = new LinkedHashMap<>();
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"((?:\\\\.|[^\"])*)\"|([^,}\\s]+))");
        Matcher m = p.matcher(json == null ? "" : json);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(2) != null ? m.group(2) : m.group(3);
            out.put(key, unesc(val));
        }
        return out;
    }

    private static String sanitizeEnum(String value, Set<String> allowed, String fallback) {
        return allowed.contains(value) ? value : fallback;
    }

    private static String classify(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = n.lastIndexOf('.');
        String e = dot >= 0 ? n.substring(dot+1) : "";
        if (Set.of("mp4","mov","mkv","webm","avi","m4v","wmv","ts").contains(e)) return "video";
        if (Set.of("png","jpg","jpeg","webp","bmp","gif").contains(e)) return "image";
        if (Set.of("mp3","wav","m4a","aac","flac","ogg","opus","wma").contains(e)) return "audio";
        return "file";
    }

    private static String assetId(String type, Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0,dot);
        n = java.text.Normalizer.normalize(n, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+","")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+","_")
            .replaceAll("^_+|_+$","");
        if (n.isBlank()) n = "arquivo";
        return type + ":" + n;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x=a.getBytes(StandardCharsets.UTF_8), y=b.getBytes(StandardCharsets.UTF_8);
        int diff=x.length^y.length;
        for (int i=0;i<Math.max(x.length,y.length);i++) {
            byte xb=i<x.length?x[i]:0, yb=i<y.length?y[i]:0;
            diff |= xb ^ yb;
        }
        return diff==0;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace(""","\\"").replace("\r","\\r").replace("\n","\\n");
    }

    private static String unesc(String s) {
        if (s == null) return "";
        return s.replace("\\"",""").replace("\\\\","\\");
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type","application/json; charset=utf-8");
        h.set("Cache-Control","no-store");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    @FunctionalInterface
    private interface ExchangeHandler { void handle(HttpExchange ex) throws IOException; }

    private static final class StopExchange extends IOException {}
}
