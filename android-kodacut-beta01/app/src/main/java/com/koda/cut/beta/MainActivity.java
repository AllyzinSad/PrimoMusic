package com.koda.cut.beta;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.*;

public class MainActivity extends Activity {
  static final int VIDEO=101, IMAGES=102, SFX=103, MUSIC=104, BROLL=105;
  static final String PREFS="koda", KEY_ASSETS="assets", KEY_SCRIPT="kodascript";

  final List<Asset> assets=new ArrayList<>();
  final Map<String,LinearLayout> lists=new LinkedHashMap<>();

  final int BG=Color.rgb(10,10,10), PANEL=Color.rgb(24,24,24), FIELD=Color.rgb(36,36,36);
  final int GOLD=Color.rgb(212,175,55), TEXT=Color.rgb(245,245,245), MUTED=Color.rgb(165,165,165);

  TextView scriptStatus, renderStatus;
  ProgressBar renderProgress;
  Button renderButton, cancelRenderButton, openVideoButton;
  RenderEngine renderEngine;
  Uri lastOutputUri;
  String currentScript="";

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    load();
    setContentView(ui());
    refresh();
    refreshScriptStatus();
  }

  ScrollView ui(){
    ScrollView s=new ScrollView(this);
    s.setBackgroundColor(BG);

    LinearLayout r=new LinearLayout(this);
    r.setOrientation(LinearLayout.VERTICAL);
    r.setPadding(dp(18),dp(18),dp(18),dp(28));
    r.setBackgroundColor(BG);
    s.addView(r);

    r.addView(t("Koda Cut",28,true,TEXT));
    TextView badge=t("ANDROID • BETA 0.3 RENDER",12,true,GOLD);
    badge.setPadding(0,dp(2),0,dp(14));
    r.addView(badge);

    TextView intro=t(
      "Editor automático local: organize os arquivos, envie o mapa para a IA, cole o KodaScript e renderize o vídeo.",
      14,false,MUTED
    );
    intro.setPadding(0,0,0,dp(16));
    r.addView(intro);

    sector(r,"main","VÍDEO PRINCIPAL","Adicionar vídeo","video/*",VIDEO,false);
    sector(r,"images","IMAGENS / PNGs","Adicionar imagens","image/*",IMAGES,true);
    sector(r,"sfx","EFEITOS SONOROS","Adicionar efeitos","audio/*",SFX,true);
    sector(r,"music","MÚSICAS","Adicionar músicas","audio/*",MUSIC,true);
    sector(r,"broll","B-ROLL","Adicionar B-roll","video/*",BROLL,true);

    LinearLayout fontCard=new LinearLayout(this);
    fontCard.setOrientation(LinearLayout.VERTICAL);
    fontCard.setPadding(dp(14),dp(14),dp(14),dp(14));
    fontCard.setBackgroundColor(PANEL);
    fontCard.addView(t("PACK DE FONTES",15,true,TEXT));
    TextView fonts=t(
      "font:anton — Impacto forte / títulos\n"+
      "font:bebas_neue — Títulos altos / shorts\n"+
      "font:montserrat — Clean / profissional\n"+
      "font:poppins — Moderno / legendas\n"+
      "font:oswald — Destaque / esportivo\n"+
      "font:bangers — Meme / quadrinhos",
      12,false,MUTED
    );
    fonts.setPadding(0,dp(8),0,0);
    fontCard.addView(fonts);
    rootAdd(r,fontCard,12);

    Button copy=btn("COPIAR MAPA PARA IA",true);
    copy.setOnClickListener(v->copyProject());
    r.addView(copy,lp(12));

    Button share=btn("COMPARTILHAR PROJETO",false);
    share.setOnClickListener(v->shareProject());
    r.addView(share,lp(8));

    Button paste=btn("COLAR KODASCRIPT",true);
    paste.setOnClickListener(v->pasteKodaScript());
    r.addView(paste,lp(16));

    scriptStatus=t("Nenhum KodaScript carregado.",13,false,MUTED);
    scriptStatus.setPadding(dp(4),dp(8),dp(4),dp(4));
    r.addView(scriptStatus);

    Button review=btn("VER RESUMO DO KODASCRIPT",false);
    review.setOnClickListener(v->reviewCurrentScript());
    r.addView(review,lp(8));

    renderButton=btn("RENDERIZAR VÍDEO",true);
    renderButton.setOnClickListener(v->startRender());
    r.addView(renderButton,lp(18));

    renderProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
    renderProgress.setMax(100);
    renderProgress.setProgress(0);
    r.addView(renderProgress,lp(10));

    renderStatus=t("Aguardando render.",13,false,MUTED);
    renderStatus.setPadding(dp(4),dp(6),dp(4),dp(4));
    r.addView(renderStatus);

    cancelRenderButton=btn("CANCELAR RENDER",false);
    cancelRenderButton.setEnabled(false);
    cancelRenderButton.setOnClickListener(v->cancelRender());
    r.addView(cancelRenderButton,lp(8));

    openVideoButton=btn("ABRIR / ASSISTIR VÍDEO",false);
    openVideoButton.setEnabled(false);
    openVideoButton.setOnClickListener(v->openLastVideo());
    r.addView(openVideoButton,lp(8));

    Button clearScript=btn("REMOVER KODASCRIPT",false);
    clearScript.setOnClickListener(v->{
      currentScript="";
      getSharedPreferences(PREFS,0).edit().remove(KEY_SCRIPT).apply();
      refreshScriptStatus();
      toast("KodaScript removido.");
    });
    r.addView(clearScript,lp(8));

    Button clear=btn("LIMPAR PROJETO",false);
    clear.setOnClickListener(v->new AlertDialog.Builder(this)
      .setTitle("Limpar projeto?")
      .setMessage("Isso remove os arquivos cadastrados e o KodaScript salvo neste beta.")
      .setNegativeButton("Cancelar",null)
      .setPositiveButton("Limpar",(d,w)->{
        assets.clear();
        currentScript="";
        getSharedPreferences(PREFS,0).edit().clear().apply();
        refresh();
        refreshScriptStatus();
        toast("Projeto limpo.");
      }).show());
    r.addView(clear,lp(14));

    TextView foot=t(
      "Beta 0.3: primeiro motor de render local. Suporta corte simples, PNG, SFX, música, texto, fontes e zoom simples.",
      12,false,MUTED
    );
    foot.setPadding(0,dp(18),0,0);
    r.addView(foot);

    return s;
  }

  void sector(LinearLayout root,String key,String title,String label,String mime,int req,boolean multi){
    LinearLayout c=new LinearLayout(this);
    c.setOrientation(LinearLayout.VERTICAL);
    c.setPadding(dp(14),dp(14),dp(14),dp(14));
    c.setBackgroundColor(PANEL);

    c.addView(t(title,15,true,TEXT));

    LinearLayout l=new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(0,dp(8),0,dp(8));
    c.addView(l);
    lists.put(key,l);

    Button b=btn("+ "+label,false);
    b.setOnClickListener(v->pick(mime,req,multi));
    c.addView(b);

    root.addView(c,lp(12));
  }

  void pick(String mime,int req,boolean multi){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
    i.addCategory(Intent.CATEGORY_OPENABLE);
    i.setType(mime);
    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multi);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    startActivityForResult(i,req);
  }

  @Override protected void onActivityResult(int req,int result,Intent data){
    super.onActivityResult(req,result,data);
    if(result!=RESULT_OK||data==null)return;

    String sec=sec(req);
    if(sec==null)return;

    if("main".equals(sec)) assets.removeIf(a->"main".equals(a.sector));

    if(data.getClipData()!=null){
      ClipData cd=data.getClipData();
      for(int x=0;x<cd.getItemCount();x++)addWithDuplicateCheck(sec,cd.getItemAt(x).getUri());
    } else if(data.getData()!=null){
      addWithDuplicateCheck(sec,data.getData());
    }

    saveAssets();
    refresh();
  }

  void addWithDuplicateCheck(String sec,Uri uri){
    try{
      getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }catch(Exception ignored){}

    String fileName=name(uri);
    if(fileName==null||fileName.isBlank())fileName="arquivo";

    for(Asset a:assets){
      if(a.uri.equals(uri.toString())&&a.sector.equals(sec)){
        toast("Esse arquivo já está neste setor.");
        return;
      }
    }

    Asset other=null;
    for(Asset a:assets){
      if(a.uri.equals(uri.toString())&&!a.sector.equals(sec)){
        other=a;
        break;
      }
    }

    final String n=fileName;
    if(other!=null){
      final Asset existing=other;
      new AlertDialog.Builder(this)
        .setTitle("Arquivo já usado")
        .setMessage(
          "“"+n+"” já está em "+label(existing.sector)+
          ".\n\nQuer usar o mesmo arquivo também em "+label(sec)+"?"
        )
        .setNegativeButton("Cancelar",null)
        .setPositiveButton("Usar mesmo assim",(d,w)->{
          addNow(sec,uri,n);
          saveAssets();
          refresh();
        }).show();
      return;
    }

    addNow(sec,uri,n);
  }

  void addNow(String sec,Uri uri,String fileName){
    assets.add(new Asset(sec,fileName,uri.toString(),id(sec,fileName)));
  }

  void refresh(){
    for(Map.Entry<String,LinearLayout> e:lists.entrySet()){
      LinearLayout l=e.getValue();
      l.removeAllViews();
      boolean any=false;

      for(Asset a:new ArrayList<>(assets)){
        if(!e.getKey().equals(a.sector))continue;
        any=true;

        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView tv=t(a.name+"\n"+a.id,12,false,TEXT);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        row.addView(tv);

        Button x=btn("×",false);
        x.setOnClickListener(v->{
          assets.remove(a);
          saveAssets();
          refresh();
          refreshScriptStatus();
        });
        row.addView(x);

        l.addView(row);
      }

      if(!any)l.addView(t("Nenhum arquivo adicionado.",12,false,MUTED));
    }
  }

  String projectPrompt(){
    StringBuilder s=new StringBuilder("MODO KODA CUT ANDROID — DIRETOR DE EDIÇÃO\n\n");
    s.append("Você vai me ajudar a planejar uma edição para o Koda Cut Android.\n");
    s.append("Converse comigo até entender referências, ritmo, cortes, zooms, legendas, imagens, B-roll, efeitos e música.\n");
    s.append("Use SOMENTE os IDs do MAPA DE ARQUIVOS. Não invente assets.\n");
    s.append("Quando eu disser \"pode começar a editar\", gere APENAS um KodaScript JSON em um único bloco de código.\n\n");

    s.append("FORMATO KODASCRIPT ANDROID BETA 0.3:\n");
    s.append("{\n");
    s.append("  \"koda_version\": \"android-0.2\",\n");
    s.append("  \"format\": \"9:16\",\n");
    s.append("  \"timeline\": [\n");
    s.append("    {\"action\":\"clip\",\"start\":0.0,\"end\":4.0},\n");
    s.append("    {\"action\":\"overlay\",\"at\":1.2,\"duration\":1.5,\"asset\":\"image:ID_DO_MAPA\",\"position\":\"bottom-right\"},\n");
    s.append("    {\"action\":\"sfx\",\"at\":1.3,\"asset\":\"audio:ID_DO_MAPA\",\"volume\":0.8},\n");
    s.append("    {\"action\":\"music\",\"start\":0.0,\"end\":20.0,\"asset\":\"music:ID_DO_MAPA\",\"volume\":0.12},\n");
    s.append("    {\"action\":\"zoom\",\"start\":2.0,\"end\":3.0,\"scale\":1.15},\n");
    s.append("    {\"action\":\"text\",\"start\":3.0,\"end\":5.0,\"text\":\"TEXTO\",\"font\":\"font:anton\"}\n");
    s.append("  ]\n");
    s.append("}\n\n");

    s.append("REGRAS:\n");
    s.append("- koda_version deve continuar android-0.2 por compatibilidade.\n");
    s.append("- Use no máximo UM evento clip nesta primeira versão de render.\n");
    s.append("- timeline deve ser uma lista JSON.\n");
    s.append("- Todo campo asset deve usar exatamente um ID existente no mapa.\n");
    s.append("- Não use caminhos de arquivo do aparelho.\n");
    s.append("- start/end/at/duration devem estar em segundos.\n");
    s.append("- Para textos, use opcionalmente o campo font com um dos IDs do PACK DE FONTES.\n");
    s.append("- B-roll pode constar no mapa, mas esta primeira versão de render ainda não o executa.\n");
    s.append("- Se ainda não houver informação suficiente, continue conversando em vez de inventar.\n\n");

    s.append("PACK DE FONTES DISPONÍVEIS:\n");
    s.append("font:anton -> Anton [impacto forte / títulos]\n");
    s.append("font:bebas_neue -> Bebas Neue [títulos altos / shorts]\n");
    s.append("font:montserrat -> Montserrat [clean / profissional]\n");
    s.append("font:poppins -> Poppins SemiBold [moderno / legendas]\n");
    s.append("font:oswald -> Oswald [destaque / esportivo]\n");
    s.append("font:bangers -> Bangers [meme / quadrinhos]\n\n");

    s.append("MAPA DE ARQUIVOS:\n");
    if(assets.isEmpty())s.append("(nenhum arquivo adicionado)\n");
    else for(Asset a:assets){
      s.append(a.id).append(" -> ").append(a.name).append(" [").append(label(a.sector)).append("]\n");
    }
    return s.toString();
  }

  void copyProject(){
    ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE))
      .setPrimaryClip(ClipData.newPlainText("Koda Project",projectPrompt()));
    toast("Projeto copiado para a IA.");
  }

  void shareProject(){
    Intent i=new Intent(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_TEXT,projectPrompt());
    startActivity(Intent.createChooser(i,"Enviar projeto para"));
  }

  void pasteKodaScript(){
    ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
    if(!cm.hasPrimaryClip()||cm.getPrimaryClip()==null||cm.getPrimaryClip().getItemCount()==0){
      showError("Área de transferência vazia.");
      return;
    }

    CharSequence cs=cm.getPrimaryClip().getItemAt(0).coerceToText(this);
    if(cs==null||cs.toString().trim().isEmpty()){
      showError("Não encontrei texto para validar.");
      return;
    }

    String raw=stripFence(cs.toString());
    Validation v=validateScript(raw);

    if(!v.ok){
      new AlertDialog.Builder(this)
        .setTitle("KodaScript inválido")
        .setMessage(v.message)
        .setPositiveButton("OK",null)
        .show();
      return;
    }

    currentScript=raw;
    getSharedPreferences(PREFS,0).edit().putString(KEY_SCRIPT,currentScript).apply();
    refreshScriptStatus();
    showSummary(v);
  }

  void reviewCurrentScript(){
    if(currentScript==null||currentScript.isBlank()){
      showError("Cole um KodaScript primeiro.");
      return;
    }
    Validation v=validateScript(currentScript);
    if(!v.ok){
      showError(v.message);
      refreshScriptStatus();
      return;
    }
    showSummary(v);
  }

  void showSummary(Validation v){
    StringBuilder m=new StringBuilder();
    m.append("KodaScript válido ✅\n\n");
    m.append("Versão: ").append(v.version).append("\n");
    m.append("Formato: ").append(v.format).append("\n");
    m.append("Eventos: ").append(v.events).append("\n");
    m.append("Assets referenciados: ").append(v.assetRefs.size()).append("\n\n");

    if(!v.actionCounts.isEmpty()){
      m.append("Ações:\n");
      for(Map.Entry<String,Integer> e:v.actionCounts.entrySet()){
        m.append("• ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
      }
    }

    if(!v.assetRefs.isEmpty()){
      m.append("\nAssets usados:\n");
      for(String id:v.assetRefs)m.append("• ").append(id).append("\n");
    }

    m.append("\nKodaScript pronto para o primeiro motor de render.");

    new AlertDialog.Builder(this)
      .setTitle("Resumo da edição")
      .setMessage(m.toString())
      .setPositiveButton("Entendi",null)
      .show();
  }

  Validation validateScript(String raw){
    Validation v=new Validation();

    try{
      JSONObject root=new JSONObject(raw);

      String version=root.optString("koda_version","");
      if(!"android-0.2".equals(version)){
        v.message="koda_version ausente ou incompatível. Esperado: android-0.2";
        return v;
      }

      JSONArray timeline=root.optJSONArray("timeline");
      if(timeline==null){
        v.message="Campo timeline não encontrado ou não é uma lista.";
        return v;
      }

      Set<String> validIds=new LinkedHashSet<>();
      for(Asset a:assets)validIds.add(a.id);

      LinkedHashSet<String> refs=new LinkedHashSet<>();
      Set<String> validFonts=new LinkedHashSet<>(Arrays.asList(
        "font:anton","font:bebas_neue","font:montserrat",
        "font:poppins","font:oswald","font:bangers"
      ));
      LinkedHashSet<String> fontRefs=new LinkedHashSet<>();
      int clipCount=0;

      for(int i=0;i<timeline.length();i++){
        JSONObject ev=timeline.optJSONObject(i);
        if(ev==null){
          v.message="Evento "+(i+1)+" da timeline não é um objeto JSON.";
          return v;
        }

        String action=ev.optString("action","").trim();
        if(action.isEmpty()){
          v.message="Evento "+(i+1)+" não possui action.";
          return v;
        }

        if("clip".equals(action))clipCount++;
        v.actionCounts.put(action,v.actionCounts.getOrDefault(action,0)+1);

        String timeError=validateTimes(ev,i+1);
        if(timeError!=null){
          v.message=timeError;
          return v;
        }

        collectAssets(ev,refs);
        collectFonts(ev,fontRefs);
      }

      if(clipCount>1){
        v.message="A Beta 0.3 aceita apenas um evento clip no primeiro motor de render.";
        return v;
      }

      List<String> missing=new ArrayList<>();
      for(String id:refs){
        if(!validIds.contains(id))missing.add(id);
      }

      List<String> missingFonts=new ArrayList<>();
      for(String id:fontRefs){
        if(!validFonts.contains(id))missingFonts.add(id);
      }

      if(!missingFonts.isEmpty()){
        v.message="O KodaScript usa fontes que não existem no pack:\\n\\n"+String.join("\\n",missingFonts);
        return v;
      }

      if(!missing.isEmpty()){
        v.message="O KodaScript usa assets que não existem neste projeto:\n\n"+String.join("\n",missing);
        return v;
      }

      v.ok=true;
      v.version=version;
      v.format=root.optString("format","não informado");
      v.events=timeline.length();
      v.assetRefs.addAll(refs);
      v.fontRefs.addAll(fontRefs);
      v.message="OK";
      return v;

    }catch(Exception e){
      v.message="JSON inválido: "+e.getMessage();
      return v;
    }
  }

  String validateTimes(JSONObject ev,int number){
    String[] keys={"start","end","at","duration"};
    for(String key:keys){
      if(ev.has(key)){
        Object x=ev.opt(key);
        if(!(x instanceof Number)){
          return "Evento "+number+": "+key+" precisa ser número.";
        }
        if(((Number)x).doubleValue()<0){
          return "Evento "+number+": "+key+" não pode ser negativo.";
        }
      }
    }

    if(ev.has("start")&&ev.has("end")){
      double start=ev.optDouble("start",-1);
      double end=ev.optDouble("end",-1);
      if(end<start)return "Evento "+number+": end não pode ser menor que start.";
    }
    return null;
  }

  void collectFonts(Object node,Set<String> refs) throws Exception{
    if(node instanceof JSONObject){
      JSONObject o=(JSONObject)node;
      Iterator<String> keys=o.keys();
      while(keys.hasNext()){
        String k=keys.next();
        Object val=o.get(k);
        if("font".equals(k)&&val instanceof String)refs.add((String)val);
        else collectFonts(val,refs);
      }
    } else if(node instanceof JSONArray){
      JSONArray a=(JSONArray)node;
      for(int i=0;i<a.length();i++)collectFonts(a.get(i),refs);
    }
  }

  void collectAssets(Object node,Set<String> refs) throws Exception{
    if(node instanceof JSONObject){
      JSONObject o=(JSONObject)node;
      Iterator<String> keys=o.keys();
      while(keys.hasNext()){
        String k=keys.next();
        Object val=o.get(k);
        if("asset".equals(k)&&val instanceof String)refs.add((String)val);
        else collectAssets(val,refs);
      }
    } else if(node instanceof JSONArray){
      JSONArray a=(JSONArray)node;
      for(int i=0;i<a.length();i++)collectAssets(a.get(i),refs);
    }
  }

  void refreshScriptStatus(){
    if(scriptStatus==null)return;

    if(currentScript==null||currentScript.isBlank()){
      scriptStatus.setText("Nenhum KodaScript carregado.");
      scriptStatus.setTextColor(MUTED);
      if(renderButton!=null)renderButton.setEnabled(false);
      return;
    }

    Validation v=validateScript(currentScript);
    if(v.ok){
      scriptStatus.setText("KodaScript válido • "+v.events+" eventos • pronto para renderizar");
      scriptStatus.setTextColor(GOLD);
      if(renderButton!=null)renderButton.setEnabled(true);
    }else{
      scriptStatus.setText("KodaScript salvo, mas agora está inválido: "+v.message);
      scriptStatus.setTextColor(Color.rgb(255,120,120));
      if(renderButton!=null)renderButton.setEnabled(false);
    }
  }

  String stripFence(String s){
    String x=s.trim();
    if(x.startsWith("```")){
      int nl=x.indexOf('\n');
      if(nl>=0)x=x.substring(nl+1);
      int end=x.lastIndexOf("```");
      if(end>=0)x=x.substring(0,end);
    }
    return x.trim();
  }

  String id(String sec,String name){
    String b=name;
    int d=b.lastIndexOf('.');
    if(d>0)b=b.substring(0,d);
    b=slug(b);

    if("main".equals(sec))return "video:principal";

    String p=("images".equals(sec)?"image:":"sfx".equals(sec)?"audio:":"music".equals(sec)?"music:":"video:")+b;
    String c=p;
    int n=2;
    while(exists(c))c=p+"_"+n++;
    return c;
  }

  boolean exists(String id){
    for(Asset a:assets)if(a.id.equals(id))return true;
    return false;
  }

  String slug(String v){
    String s=Normalizer.normalize(v,Normalizer.Form.NFD)
      .replaceAll("\\p{M}","")
      .toLowerCase(Locale.ROOT)
      .replaceAll("[^a-z0-9]+","_")
      .replaceAll("^_+|_+$","");
    return s.isBlank()?"arquivo":s;
  }

  String name(Uri u){
    try(Cursor c=getContentResolver().query(u,null,null,null,null)){
      if(c!=null&&c.moveToFirst()){
        int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if(i>=0)return c.getString(i);
      }
    }catch(Exception ignored){}
    return u.getLastPathSegment();
  }

  String sec(int r){
    return r==VIDEO?"main":r==IMAGES?"images":r==SFX?"sfx":r==MUSIC?"music":r==BROLL?"broll":null;
  }

  String label(String s){
    return "main".equals(s)?"vídeo principal":
      "images".equals(s)?"imagem/PNG":
      "sfx".equals(s)?"efeito sonoro":
      "music".equals(s)?"música":"B-roll";
  }

  void saveAssets(){
    try{
      JSONArray a=new JSONArray();
      for(Asset x:assets){
        JSONObject o=new JSONObject();
        o.put("s",x.sector);
        o.put("n",x.name);
        o.put("u",x.uri);
        o.put("i",x.id);
        a.put(o);
      }
      getSharedPreferences(PREFS,0).edit().putString(KEY_ASSETS,a.toString()).apply();
    }catch(Exception ignored){}
  }

  void load(){
    currentScript=getSharedPreferences(PREFS,0).getString(KEY_SCRIPT,"");
    try{
      JSONArray a=new JSONArray(getSharedPreferences(PREFS,0).getString(KEY_ASSETS,"[]"));
      for(int i=0;i<a.length();i++){
        JSONObject o=a.getJSONObject(i);
        assets.add(new Asset(o.optString("s"),o.optString("n"),o.optString("u"),o.optString("i")));
      }
    }catch(Exception ignored){}
  }

  void startRender(){
    Validation v=validateScript(currentScript);
    if(!v.ok){
      showError(v.message);
      return;
    }

    LinkedHashMap<String,RenderEngine.AssetRef> map=new LinkedHashMap<>();
    for(Asset a:assets){
      map.put(a.id,new RenderEngine.AssetRef(a.id,a.name,a.uri));
    }

    renderEngine=new RenderEngine(this,map);
    renderButton.setEnabled(false);
    cancelRenderButton.setEnabled(true);
    openVideoButton.setEnabled(false);
    renderProgress.setProgress(0);
    setRenderStatus("Preparando render...",GOLD);

    renderEngine.render(currentScript,new RenderEngine.Callback(){
      @Override public void onStage(String text){
        runOnUiThread(()->setRenderStatus(text,GOLD));
      }

      @Override public void onProgress(int percent){
        runOnUiThread(()->{
          renderProgress.setProgress(percent);
          setRenderStatus("Renderizando... "+percent+"%",GOLD);
        });
      }

      @Override public void onCompleted(Uri outputUri){
        lastOutputUri=outputUri;
        runOnUiThread(()->{
          renderProgress.setProgress(100);
          setRenderStatus("Render concluído ✅",GOLD);
          renderButton.setEnabled(true);
          cancelRenderButton.setEnabled(false);
          openVideoButton.setEnabled(true);
          toast("Vídeo salvo em Movies/KodaCut.");
        });
      }

      @Override public void onCancelled(){
        runOnUiThread(()->{
          setRenderStatus("Render cancelado.",MUTED);
          renderButton.setEnabled(true);
          cancelRenderButton.setEnabled(false);
        });
      }

      @Override public void onError(String message){
        runOnUiThread(()->{
          setRenderStatus("Erro no render.",Color.rgb(255,120,120));
          renderButton.setEnabled(true);
          cancelRenderButton.setEnabled(false);
          showError(message);
        });
      }
    });
  }

  void cancelRender(){
    if(renderEngine!=null&&renderEngine.isRunning()){
      renderEngine.cancel();
      setRenderStatus("Cancelando...",MUTED);
    }
  }

  void openLastVideo(){
    if(lastOutputUri==null){
      toast("Nenhum vídeo renderizado nesta sessão.");
      return;
    }

    try{
      Intent i=new Intent(Intent.ACTION_VIEW);
      i.setDataAndType(lastOutputUri,"video/mp4");
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(i);
    }catch(Exception e){
      toast("Vídeo salvo, mas não encontrei um player compatível.");
    }
  }

  void setRenderStatus(String value,int color){
    if(renderStatus!=null){
      renderStatus.setText(value);
      renderStatus.setTextColor(color);
    }
  }

  void showError(String message){
    new AlertDialog.Builder(this)
      .setTitle("Koda Cut")
      .setMessage(message)
      .setPositiveButton("OK",null)
      .show();
  }

  void rootAdd(LinearLayout root,LinearLayout child,int top){
    root.addView(child,lp(top));
  }

  Button btn(String s,boolean primary){
    Button b=new Button(this);
    b.setText(s);
    b.setAllCaps(false);
    b.setTextSize(12);
    b.setTextColor(primary?Color.BLACK:TEXT);
    b.setBackgroundColor(primary?GOLD:FIELD);
    return b;
  }

  TextView t(String s,int size,boolean bold,int color){
    TextView v=new TextView(this);
    v.setText(s);
    v.setTextSize(size);
    v.setTextColor(color);
    if(bold)v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    return v;
  }

  LinearLayout.LayoutParams lp(int top){
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
    p.topMargin=dp(top);
    return p;
  }

  int dp(int v){
    return Math.round(v*getResources().getDisplayMetrics().density);
  }

  void toast(String s){
    Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
  }

  static class Asset{
    final String sector,name,uri,id;
    Asset(String s,String n,String u,String i){
      sector=s;
      name=n;
      uri=u;
      id=i;
    }
  }

  static class Validation{
    boolean ok=false;
    String message="",version="",format="";
    int events=0;
    final LinkedHashSet<String> assetRefs=new LinkedHashSet<>();
    final LinkedHashSet<String> fontRefs=new LinkedHashSet<>();
    final LinkedHashMap<String,Integer> actionCounts=new LinkedHashMap<>();
  }
}
