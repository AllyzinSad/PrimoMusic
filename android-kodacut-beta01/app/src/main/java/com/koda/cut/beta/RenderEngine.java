package com.koda.cut.beta;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.StreamInformation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RenderEngine {
    public interface Callback {
        void onStage(String text);
        void onProgress(int percent);
        void onCompleted(Uri outputUri);
        void onCancelled();
        void onError(String message);
    }

    public static class AssetRef {
        public final String id;
        public final String name;
        public final String uri;

        public AssetRef(String id, String name, String uri) {
            this.id = id;
            this.name = name;
            this.uri = uri;
        }
    }

    private final Context context;
    private final Map<String, AssetRef> assets;
    private final LinkedHashMap<String, String> fontFiles = new LinkedHashMap<>();
    private long activeSessionId = -1;

    public RenderEngine(Context context, Map<String, AssetRef> assets) {
        this.context = context.getApplicationContext();
        this.assets = assets;

        fontFiles.put("font:anton", "Anton-Regular.ttf");
        fontFiles.put("font:bebas_neue", "BebasNeue-Regular.ttf");
        fontFiles.put("font:montserrat", "Montserrat.ttf");
        fontFiles.put("font:poppins", "Poppins-SemiBold.ttf");
        fontFiles.put("font:oswald", "Oswald.ttf");
        fontFiles.put("font:bangers", "Bangers-Regular.ttf");
    }

    public boolean isRunning() {
        return activeSessionId != -1;
    }

    public void cancel() {
        if (activeSessionId != -1) {
            FFmpegKit.cancel(activeSessionId);
        }
    }

    public void render(String script, Callback callback) {
        if (isRunning()) {
            callback.onError("Já existe um render em andamento.");
            return;
        }

        new Thread(() -> {
            try {
                callback.onStage("Preparando arquivos...");
                RenderPlan plan = buildPlan(script);

                callback.onStage("Iniciando motor de vídeo...");

                FFmpegSession session = FFmpegKit.executeAsync(
                    plan.command,
                    completed -> {
                        activeSessionId = -1;
                        ReturnCode code = completed.getReturnCode();

                        if (ReturnCode.isSuccess(code)) {
                            try {
                                callback.onProgress(100);
                                callback.onStage("Salvando vídeo...");
                                Uri saved = saveToGallery(plan.outputFile);
                                callback.onCompleted(saved);
                            } catch (Exception e) {
                                callback.onError("Render concluído, mas não consegui salvar: " + e.getMessage());
                            }
                        } else if (ReturnCode.isCancel(code)) {
                            callback.onCancelled();
                        } else {
                            String output = completed.getOutput();
                            String fail = completed.getFailStackTrace();
                            String detail = output != null && !output.trim().isEmpty() ? tail(output, 2200) : fail;
                            callback.onError(detail == null ? "O motor de vídeo retornou erro." : "FFmpeg:\n" + detail);
                        }
                    },
                    log -> {},
                    statistics -> {
                        double ms = statistics.getTime();
                        int p = (int)Math.max(0, Math.min(99,
                            (ms * 100.0) / Math.max(1L, plan.durationMs)));
                        callback.onProgress(p);
                    }
                );

                activeSessionId = session.getSessionId();

            } catch (Exception e) {
                activeSessionId = -1;
                callback.onError(e.getMessage() == null ? e.toString() : e.getMessage());
            }
        }).start();
    }

    private RenderPlan buildPlan(String script) throws Exception {
        JSONObject root = new JSONObject(script);
        JSONArray timeline = root.getJSONArray("timeline");

        AssetRef main = assets.get("video:principal");
        if (main == null) {
            throw new Exception("Adicione um vídeo principal.");
        }

        double clipStart = 0.0;
        double clipEnd = -1.0;
        int clipCount = 0;

        for (int i = 0; i < timeline.length(); i++) {
            JSONObject event = timeline.getJSONObject(i);
            if ("clip".equals(event.optString("action"))) {
                clipCount++;
                clipStart = event.optDouble("start", 0.0);
                clipEnd = event.optDouble("end", -1.0);
            }
        }

        if (clipCount > 1) {
            throw new Exception("Esta primeira versão de render aceita apenas um evento clip.");
        }

        File cache = new File(context.getCacheDir(), "koda_render");
        if (!cache.exists() && !cache.mkdirs()) {
            throw new Exception("Não consegui preparar o cache de render.");
        }
        clearOldFiles(cache);

        File mainFile = copyUri(Uri.parse(main.uri), main.name, cache, "main");

        if (clipEnd < 0) {
            clipEnd = probeDuration(mainFile);
        }
        if (clipEnd <= clipStart) {
            throw new Exception("Intervalo do vídeo principal inválido.");
        }

        final double duration = clipEnd - clipStart;
        final long durationMs = (long)(duration * 1000.0);

        String format = root.optString("format", "9:16");
        int outW = 1080;
        int outH = 1920;

        if ("16:9".equals(format)) {
            outW = 1920;
            outH = 1080;
        } else if ("1:1".equals(format)) {
            outW = 1080;
            outH = 1080;
        }

        boolean hasMainAudio = hasAudio(mainFile);

        StringBuilder command = new StringBuilder();
        command.append("-y ");
        command.append("-ss ").append(fmt(clipStart))
            .append(" -t ").append(fmt(duration))
            .append(" -i ").append(q(mainFile.getAbsolutePath())).append(" ");

        List<InputEvent> visualInputs = new ArrayList<>();
        List<InputEvent> audioInputs = new ArrayList<>();
        int inputIndex = 1;

        for (int i = 0; i < timeline.length(); i++) {
            JSONObject event = timeline.getJSONObject(i);
            String action = event.optString("action");

            if ("overlay".equals(action)) {
                String assetId = event.optString("asset", "");
                if (assetId.startsWith("image:")) {
                    AssetRef ref = assets.get(assetId);
                    if (ref == null) throw new Exception("Asset não encontrado: " + assetId);

                    File file = copyUri(Uri.parse(ref.uri), ref.name, cache, "img_" + i);
                    command.append("-loop 1 -i ").append(q(file.getAbsolutePath())).append(" ");
                    visualInputs.add(new InputEvent(i, inputIndex++, event));
                }
            }

            if ("sfx".equals(action) || "music".equals(action)) {
                String assetId = event.optString("asset", "");
                AssetRef ref = assets.get(assetId);
                if (ref == null) throw new Exception("Asset não encontrado: " + assetId);

                File file = copyUri(Uri.parse(ref.uri), ref.name, cache, "aud_" + i);
                command.append("-i ").append(q(file.getAbsolutePath())).append(" ");
                audioInputs.add(new InputEvent(i, inputIndex++, event));
            }
        }

        Map<String, File> fonts = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fontFiles.entrySet()) {
            fonts.put(entry.getKey(), copyFont(entry.getValue(), cache));
        }

        StringBuilder filters = new StringBuilder();

        filters.append("[0:v]scale=").append(outW).append(":").append(outH)
            .append(":force_original_aspect_ratio=decrease,pad=")
            .append(outW).append(":").append(outH)
            .append(":(ow-iw)/2:(oh-ih)/2:black,setsar=1[v0];");

        String currentVideo = "v0";
        int stage = 1;

        for (int i = 0; i < timeline.length(); i++) {
            JSONObject event = timeline.getJSONObject(i);
            String action = event.optString("action");

            if ("zoom".equals(action)) {
                double start = Math.max(0, event.optDouble("start", 0) - clipStart);
                double end = Math.max(start, event.optDouble("end", start + 1) - clipStart);
                double scale = event.optDouble("scale", 1.12);

                scale = Math.max(1.0, Math.min(2.0, scale));

                int zw = even((int)Math.round(outW * scale));
                int zh = even((int)Math.round(outH * scale));

                String a = "za" + stage;
                String b = "zb" + stage;
                String z = "zoom" + stage;
                String next = "v" + stage;

                filters.append("[").append(currentVideo).append("]split=2[")
                    .append(a).append("][").append(b).append("];");

                filters.append("[").append(b).append("]scale=")
                    .append(zw).append(":").append(zh)
                    .append(",crop=").append(outW).append(":").append(outH)
                    .append(":(iw-ow)/2:(ih-oh)/2[").append(z).append("];");

                filters.append("[").append(a).append("][").append(z)
                    .append("]overlay=0:0:enable='between(t,")
                    .append(fmt(start)).append(",").append(fmt(end))
                    .append(")'[").append(next).append("];");

                currentVideo = next;
                stage++;
            }

            if ("overlay".equals(action) &&
                event.optString("asset", "").startsWith("image:")) {

                InputEvent input = findEvent(visualInputs, i);
                if (input == null) continue;

                double at = Math.max(0, event.optDouble("at", 0) - clipStart);
                double length = Math.max(0.05, event.optDouble("duration", 1.5));
                double end = at + length;
                int width = event.optInt("width", Math.max(240, outW / 3));

                String ov = "ov" + stage;
                String next = "v" + stage;

                filters.append("[").append(input.inputIndex).append(":v]")
                    .append("scale=").append(width).append(":-1[").append(ov).append("];");

                String[] xy = overlayPosition(event.optString("position", "bottom-right"));

                filters.append("[").append(currentVideo).append("][").append(ov)
                    .append("]overlay=").append(xy[0]).append(":").append(xy[1])
                    .append(":enable='between(t,").append(fmt(at)).append(",")
                    .append(fmt(end)).append(")':eof_action=repeat[")
                    .append(next).append("];");

                currentVideo = next;
                stage++;
            }

            if ("text".equals(action)) {
                double start = Math.max(0, event.optDouble("start", 0) - clipStart);
                double end = Math.max(start, event.optDouble("end", start + 2) - clipStart);
                String value = event.optString("text", "");
                String fontId = event.optString("font", "font:poppins");
                File font = fonts.get(fontId);

                if (font == null) font = fonts.get("font:poppins");

                int size = event.optInt("size", Math.max(42, outW / 16));
                String next = "v" + stage;

                filters.append("[").append(currentVideo).append("]drawtext=")
                    .append("fontfile=").append(filterEscape(font.getAbsolutePath())).append(":")
                    .append("text=").append(filterEscape(escapeDrawText(value))).append(":")
                    .append("fontsize=").append(size).append(":")
                    .append("fontcolor=white:borderw=4:bordercolor=black@0.85:")
                    .append("x=(w-text_w)/2:y=h-text_h-180:")
                    .append("enable='between(t,").append(fmt(start)).append(",")
                    .append(fmt(end)).append(")'[").append(next).append("];");

                currentVideo = next;
                stage++;
            }
        }

        List<String> audioLabels = new ArrayList<>();

        if (hasMainAudio) {
            filters.append("[0:a]aresample=44100,asetpts=PTS-STARTPTS[abase];");
        } else {
            filters.append("anullsrc=r=44100:cl=stereo,atrim=0:")
                .append(fmt(duration)).append("[abase];");
        }
        audioLabels.add("abase");

        int audioNumber = 0;

        for (InputEvent input : audioInputs) {
            JSONObject event = input.event;
            String action = event.optString("action");
            double volume = Math.max(0, Math.min(2.0, event.optDouble("volume", 0.8)));
            String label = "ae" + audioNumber++;

            if ("sfx".equals(action)) {
                double at = Math.max(0, event.optDouble("at", 0) - clipStart);
                long delay = (long)(at * 1000.0);

                filters.append("[").append(input.inputIndex).append(":a]")
                    .append("aresample=44100,adelay=").append(delay).append("|").append(delay)
                    .append(",volume=").append(fmt(volume))
                    .append("[").append(label).append("];");
            } else {
                double start = Math.max(0, event.optDouble("start", 0) - clipStart);
                double end = Math.max(start,
                    event.optDouble("end", duration + clipStart) - clipStart);
                double length = Math.max(0.05, end - start);
                long delay = (long)(start * 1000.0);

                filters.append("[").append(input.inputIndex).append(":a]")
                    .append("aresample=44100,atrim=0:").append(fmt(length))
                    .append(",adelay=").append(delay).append("|").append(delay)
                    .append(",volume=").append(fmt(volume))
                    .append("[").append(label).append("];");
            }

            audioLabels.add(label);
        }

        if (audioLabels.size() == 1) {
            filters.append("[abase]anull[aout];");
        } else {
            for (String label : audioLabels) {
                filters.append("[").append(label).append("]");
            }
            filters.append("amix=inputs=").append(audioLabels.size())
                .append(":duration=first:dropout_transition=2[aout];");
        }

        if (filters.length() > 0 && filters.charAt(filters.length() - 1) == ';') {
            filters.setLength(filters.length() - 1);
        }

        File output = new File(cache,
            "koda_render_" + System.currentTimeMillis() + ".mp4");

        command.append("-filter_complex ").append(dq(filters.toString())).append(" ");
        command.append("-map [").append(currentVideo).append("] -map [aout] ");
        command.append("-c:v mpeg4 -q:v 4 -pix_fmt yuv420p ");
        command.append("-c:a aac -b:a 192k -ar 44100 ");
        command.append("-t ").append(fmt(duration)).append(" -movflags +faststart ");
        command.append(q(output.getAbsolutePath()));

        return new RenderPlan(command.toString(), output, durationMs);
    }

    private boolean hasAudio(File file) {
        try {
            MediaInformationSession session =
                FFprobeKit.getMediaInformation(file.getAbsolutePath());

            MediaInformation info = session.getMediaInformation();
            if (info == null || info.getStreams() == null) return false;

            for (StreamInformation stream : info.getStreams()) {
                if ("audio".equalsIgnoreCase(stream.getType())) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private double probeDuration(File file) {
        try {
            MediaInformationSession session =
                FFprobeKit.getMediaInformation(file.getAbsolutePath());

            MediaInformation info = session.getMediaInformation();
            if (info != null && info.getDuration() != null) {
                return Double.parseDouble(info.getDuration());
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private Uri saveToGallery(File source) throws Exception {
        if (!source.exists() || source.length() == 0) {
            throw new Exception("Arquivo de saída não foi criado.");
        }

        String stamp =
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String name = "KodaCut_" + stamp + ".mp4";

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/KodaCut");
            values.put(MediaStore.Video.Media.IS_PENDING, 1);

            ContentResolver resolver = context.getContentResolver();
            Uri collection =
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY);

            Uri item = resolver.insert(collection, values);
            if (item == null) throw new Exception("Não consegui criar o vídeo na galeria.");

            try (
                OutputStream out = resolver.openOutputStream(item);
                InputStream in = new FileInputStream(source)
            ) {
                if (out == null) throw new Exception("Não consegui abrir a saída.");
                copy(in, out);
            }

            ContentValues done = new ContentValues();
            done.put(MediaStore.Video.Media.IS_PENDING, 0);
            resolver.update(item, done, null, null);
            return item;
        }

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (dir == null) dir = context.getFilesDir();

        File dest = new File(dir, name);

        try (
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(dest)
        ) {
            copy(in, out);
        }

        return Uri.fromFile(dest);
    }

    private File copyUri(
        Uri uri,
        String originalName,
        File dir,
        String prefix
    ) throws Exception {
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            ext = originalName.substring(dot);
        }

        File out = new File(
            dir,
            prefix + "_" + Math.abs(uri.toString().hashCode()) + ext
        );

        try (
            InputStream in = context.getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(out)
        ) {
            if (in == null) throw new Exception("Não consegui abrir " + originalName);
            copy(in, os);
        }

        return out;
    }

    private File copyFont(String assetName, File dir) throws Exception {
        File out = new File(dir, assetName);

        try (
            InputStream in = context.getAssets().open("fonts/" + assetName);
            OutputStream os = new FileOutputStream(out)
        ) {
            copy(in, os);
        }

        return out;
    }

    private void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[1024 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private void clearOldFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile()) file.delete();
        }
    }

    private InputEvent findEvent(List<InputEvent> list, int index) {
        for (InputEvent event : list) {
            if (event.eventIndex == index) return event;
        }
        return null;
    }

    private String[] overlayPosition(String position) {
        switch (position) {
            case "top-left":
                return new String[]{"40", "40"};
            case "top-right":
                return new String[]{"W-w-40", "40"};
            case "bottom-left":
                return new String[]{"40", "H-h-80"};
            case "center":
                return new String[]{"(W-w)/2", "(H-h)/2"};
            default:
                return new String[]{"W-w-40", "H-h-80"};
        }
    }

    private int even(int value) {
        return value % 2 == 0 ? value : value + 1;
    }

    private String fmt(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private String q(String text) {
        return "'" + text.replace("'", "'\\''") + "'";
    }

    private String dq(String text) {
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"") + "\"";
    }

    private String filterEscape(String text) {
        return text
            .replace("\\", "\\\\")
            .replace(":", "\\:")
            .replace("'", "\\'");
    }

    private String escapeDrawText(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace(":", "\\:")
            .replace("%", "\\%");
    }

    private String tail(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ?
            value :
            value.substring(value.length() - max);
    }

    private static class InputEvent {
        final int eventIndex;
        final int inputIndex;
        final JSONObject event;

        InputEvent(int eventIndex, int inputIndex, JSONObject event) {
            this.eventIndex = eventIndex;
            this.inputIndex = inputIndex;
            this.event = event;
        }
    }

    private static class RenderPlan {
        final String command;
        final File outputFile;
        final long durationMs;

        RenderPlan(String command, File outputFile, long durationMs) {
            this.command = command;
            this.outputFile = outputFile;
            this.durationMs = durationMs;
        }
    }
}
