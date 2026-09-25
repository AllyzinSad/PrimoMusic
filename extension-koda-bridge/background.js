const BASE = "http://127.0.0.1:17777";

async function api(path, options = {}) {
  const res = await fetch(BASE + path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : {}; } catch { data = { raw: text }; }
  if (!res.ok) {
    const message = data?.error || data?.raw || ("HTTP " + res.status);
    throw new Error(message);
  }
  return data;
}

async function getHealth() {
  return api("/health");
}

async function getFiles() {
  return api("/api/files");
}

async function saveProject(kodaScript) {
  const body = typeof kodaScript === "string" ? kodaScript : JSON.stringify(kodaScript);
  return api("/api/projects", { method: "POST", body });
}

function chooseMainVideo(files) {
  const all = Array.isArray(files?.files) ? files.files : [];
  const preferred = all.filter(f =>
    f.type === "video" &&
    typeof f.relative === "string" &&
    /^videos[\\/]/i.test(f.relative)
  );
  return preferred[0] || all.find(f => f.type === "video") || null;
}

async function startRender(projectName, options = {}) {
  const files = await getFiles();
  const video = chooseMainVideo(files);
  if (!video) throw new Error("Nenhum vídeo encontrado na pasta videos.");

  const payload = {
    video: video.name,
    project: projectName,
    outputMode: options.outputMode || "horizontal",
    verticalMode: options.verticalMode || "blur",
    quality: options.quality || "balanceado",
    captionMode: options.captionMode || "destaques",
    language: options.language || "pt",
    style: options.style || "clean"
  };

  const job = await api("/api/render", {
    method: "POST",
    body: JSON.stringify(payload)
  });

  return { ...job, video };
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  (async () => {
    try {
      if (message.type === "health") {
        sendResponse({ ok: true, data: await getHealth() });
        return;
      }

      if (message.type === "project_map") {
        sendResponse({ ok: true, data: await getFiles() });
        return;
      }

      if (message.type === "send_kodascript") {
        const saved = await saveProject(message.kodaScript);
        const started = await startRender(saved.project, message.options || {});
        sendResponse({
          ok: true,
          data: {
            project: saved.project,
            jobId: started.jobId,
            video: started.video
          }
        });
        return;
      }

      if (message.type === "job_status") {
        sendResponse({
          ok: true,
          data: await api("/api/jobs?id=" + encodeURIComponent(message.jobId))
        });
        return;
      }

      if (message.type === "cancel_job") {
        sendResponse({
          ok: true,
          data: await api("/api/cancel?id=" + encodeURIComponent(message.jobId), { method: "POST", body: "{}" })
        });
        return;
      }

      sendResponse({ ok: false, error: "Comando desconhecido." });
    } catch (err) {
      sendResponse({ ok: false, error: err?.message || String(err) });
    }
  })();
  return true;
});
