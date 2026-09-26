(() => {
  if (window.__kodaBridgeLoaded) return;
  window.__kodaBridgeLoaded = true;

  let activeJobId = null;
  let pollTimer = null;

  function el(tag, cls, text) {
    const node = document.createElement(tag);
    if (cls) node.className = cls;
    if (text != null) node.textContent = text;
    return node;
  }

  const root = el("div", "koda-bridge");
  const head = el("div", "koda-head");
  const brand = el("div", "koda-brand", "Koda Bridge");
  const badge = el("div", "koda-badge", "BETA");
  head.append(brand, badge);

  const state = el("div", "koda-state", "Verificando Koda Connect...");
  const detail = el("div", "koda-detail", "Abra o Koda Connect no PC.");

  const sendMap = el("button", "koda-btn primary", "ENVIAR PROJETO AO CHAT");
  const sendEdit = el("button", "koda-btn", "ENVIAR EDIÇÃO PARA KODA");
  const cancel = el("button", "koda-btn danger hidden", "CANCELAR EDIÇÃO");

  const progressWrap = el("div", "koda-progress-wrap hidden");
  const progressBar = el("div", "koda-progress-bar");
  progressWrap.append(progressBar);

  root.append(head, state, detail, progressWrap, sendMap, sendEdit, cancel);
  document.documentElement.appendChild(root);

  function setState(text, detailText = "", mode = "") {
    state.textContent = text;
    detail.textContent = detailText;
    root.dataset.mode = mode;
  }

  function send(message) {
    return new Promise(resolve => chrome.runtime.sendMessage(message, resolve));
  }

  async function checkHealth() {
    const r = await send({ type: "health" });
    if (r?.ok) {
      setState("Koda Connect conectado", "Pronto para conversar e editar.", "ok");
    } else {
      setState("Koda Connect desconectado", "Abra o Koda Connect e tente novamente.", "error");
    }
  }

  function projectPrompt(files) {
    const list = Array.isArray(files?.files) ? files.files : [];
    const lines = list.length
      ? list.map(f => `${f.id} -> ${f.name}`)
      : ["(nenhum arquivo encontrado)"];

    return [
      "MODO KODA CONNECT — DIRETOR DE EDIÇÃO",
      "",
      "Você está atuando como diretor de edição para um projeto que será executado localmente no PC do usuário pelo Koda Connect + Koda Cut.",
      "O Koda Connect NÃO decide a criatividade da edição. Você é o cérebro da edição; o Koda Connect apenas executa as instruções estruturadas.",
      "",
      "SEU PAPEL NESTE CHAT:",
      "- converse normalmente com o usuário até entender exatamente o resultado desejado;",
      "- entenda referências enviadas pelo usuário, inclusive links e exemplos de estilo;",
      "- pergunte apenas o que for realmente necessário;",
      "- defina ritmo, cortes, zooms, freezes, shakes, textos, legendas, imagens, PNGs, B-roll, efeitos sonoros, música e formato;",
      "- preserve fala, legibilidade e elementos importantes do vídeo;",
      "- use timestamps reais quando eles estiverem disponíveis;",
      "- use SOMENTE IDs existentes no MAPA DE ARQUIVOS abaixo;",
      "- nunca invente assets, nomes de arquivos ou IDs;",
      "- se um asset não existir, avise o usuário antes da execução;",
      "- durante a conversa, NÃO gere o KodaScript final antes da autorização do usuário.",
      "",
      "GATILHO DE EXECUÇÃO:",
      "Quando o usuário disser algo equivalente a “pode começar a editar”, “pode editar” ou “gerar edição”, considere a edição aprovada.",
      "Nesse momento, gere APENAS um KodaScript JSON válido em UM único bloco de código.",
      "Não coloque explicações antes ou depois do JSON, porque o Koda Bridge localizará esse bloco automaticamente.",
      "",
      "COMPATIBILIDADE KODASCRIPT — BETA:",
      "- o JSON deve representar a edição do vídeo principal;",
      "- ações podem incluir cortes/trechos, texto, zoom, shake, freeze frame, elementos visuais, B-roll e áudios;",
      "- para assets, referencie sempre o campo asset usando exatamente um ID do mapa, por exemplo image:logo, audio:impacto ou video:broll_praia;",
      "- efeitos e elementos devem possuir início/tempo coerentes com o vídeo;",
      "- mantenha volumes razoáveis e música de fundo baixa quando houver voz;",
      "- não inclua comandos de sistema, caminhos absolutos do PC ou arquivos fora do mapa;",
      "- se não houver informação suficiente para gerar uma edição executável, continue conversando em vez de inventar dados.",
      "",
      "EXEMPLO DE EVENTO COMPATÍVEL:",
      "{",
      "  \"inicio\": 12.4,",
      "  \"fim\": 15.0,",
      "  \"texto\": \"QUE ISSO?\",",
      "  \"zoom\": 1.18,",
      "  \"elementos\": [{\"asset\": \"image:meme_chocado\", \"posicao\": \"bottom-left\", \"largura\": 380}],",
      "  \"audios\": [{\"asset\": \"audio:impacto\", \"at\": 0.15, \"volume\": 0.8}]",
      "}",
      "",
      "IMPORTANTE:",
      "O exemplo acima serve apenas para demonstrar campos. Não use os IDs do exemplo se eles não estiverem no mapa real.",
      "",
      "MAPA DE ARQUIVOS AUTORIZADOS:",
      ...lines,
      "",
      "Agora converse comigo normalmente sobre como quero editar este projeto."
    ].join("\n");
  }

  async function copyText(text) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      const ta = document.createElement("textarea");
      ta.value = text;
      ta.style.position = "fixed";
      ta.style.opacity = "0";
      document.body.appendChild(ta);
      ta.select();
      const ok = document.execCommand("copy");
      ta.remove();
      return ok;
    }
  }

  function findComposer() {
    return document.querySelector('textarea[data-id="root"]') ||
      document.querySelector("textarea") ||
      document.querySelector('[contenteditable="true"][data-lexical-editor="true"]') ||
      document.querySelector('div[contenteditable="true"]');
  }

  function placeInComposer(text) {
    const composer = findComposer();
    if (!composer) return false;

    composer.focus();
    if (composer.tagName === "TEXTAREA") {
      const setter = Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, "value")?.set;
      setter ? setter.call(composer, text) : composer.value = text;
      composer.dispatchEvent(new Event("input", { bubbles: true }));
      return true;
    }

    composer.textContent = text;
    composer.dispatchEvent(new InputEvent("input", { bubbles: true, inputType: "insertText", data: text }));
    return true;
  }

  sendMap.addEventListener("click", async () => {
    sendMap.disabled = true;
    setState("Lendo projeto...", "Buscando vídeos, imagens e áudios na Workspace.", "working");
    const r = await send({ type: "project_map" });
    sendMap.disabled = false;

    if (!r?.ok) {
      setState("Não consegui ler o projeto", r?.error || "Verifique o Koda Connect.", "error");
      return;
    }

    const prompt = projectPrompt(r.data);
    const inserted = placeInComposer(prompt);
    if (inserted) {
      setState("Projeto colocado no chat", "Revise e envie a mensagem para começar a conversa.", "ok");
    } else {
      await copyText(prompt);
      setState("Projeto copiado", "Cole na caixa de mensagem do ChatGPT.", "ok");
    }
  });

  function codeCandidates() {
    const nodes = [...document.querySelectorAll("pre code, pre")];
    const seen = new Set();
    const out = [];
    for (const n of nodes.reverse()) {
      const text = (n.innerText || n.textContent || "").trim();
      if (!text || seen.has(text)) continue;
      seen.add(text);
      out.push(text);
    }
    return out;
  }

  function cleanFence(text) {
    return text
      .replace(/^\s*```(?:json|javascript|js)?\s*/i, "")
      .replace(/\s*```\s*$/i, "")
      .trim();
  }

  function findLatestJson() {
    for (const raw of codeCandidates()) {
      const text = cleanFence(raw);
      if (!text.startsWith("{")) continue;
      try {
        JSON.parse(text);
        return text;
      } catch {}
    }
    return null;
  }

  async function startStatusPolling(jobId) {
    activeJobId = jobId;
    cancel.classList.remove("hidden");
    progressWrap.classList.remove("hidden");

    clearInterval(pollTimer);
    pollTimer = setInterval(async () => {
      const r = await send({ type: "job_status", jobId });
      if (!r?.ok) {
        setState("Falha ao consultar edição", r?.error || "", "error");
        return;
      }

      const j = r.data;
      const p = Number(j.progress || 0);
      progressBar.style.width = Math.max(0, Math.min(100, p)) + "%";
      setState(j.stage || "Editando...", `Progresso: ${p}%`, j.status === "failed" ? "error" : "working");

      if (["completed", "failed", "cancelled"].includes(j.status)) {
        clearInterval(pollTimer);
        pollTimer = null;
        cancel.classList.add("hidden");

        if (j.status === "completed") {
          setState("Edição concluída", j.output || "Confira a pasta outputs.", "ok");
        } else if (j.status === "cancelled") {
          setState("Edição cancelada", "Nenhum novo render será iniciado.", "error");
        } else {
          setState("Erro na edição", j.error || "Veja os logs do Koda Connect.", "error");
        }
      }
    }, 1800);
  }

  sendEdit.addEventListener("click", async () => {
    const json = findLatestJson();
    if (!json) {
      setState("KodaScript não encontrado", "Peça ao ChatGPT para gerar a edição em um bloco JSON.", "error");
      return;
    }

    const files = await send({ type: "project_map" });
    const list = files?.data?.files || [];
    const main = list.find(f => f.type === "video" && /^videos[\\/]/i.test(f.relative || "")) ||
                 list.find(f => f.type === "video");

    if (!main) {
      setState("Nenhum vídeo encontrado", "Coloque o vídeo na pasta videos da Workspace.", "error");
      return;
    }

    if (!confirm(`Iniciar edição usando o vídeo “${main.name}”?`)) return;

    sendEdit.disabled = true;
    setState("Enviando edição...", "Koda Connect está recebendo o KodaScript.", "working");
    const r = await send({ type: "send_kodascript", kodaScript: json });
    sendEdit.disabled = false;

    if (!r?.ok) {
      setState("Não foi possível iniciar", r?.error || "Veja o Koda Connect.", "error");
      return;
    }

    setState("Edição iniciada", `Vídeo: ${r.data.video.name}`, "working");
    startStatusPolling(r.data.jobId);
  });

  cancel.addEventListener("click", async () => {
    if (!activeJobId) return;
    await send({ type: "cancel_job", jobId: activeJobId });
  });

  checkHealth();
  setInterval(checkHealth, 12000);
})();
