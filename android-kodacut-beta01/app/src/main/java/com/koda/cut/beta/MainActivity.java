package com.koda.cut.beta;

import android.app.Activity;
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
  final List<Asset> assets=new ArrayList<>();
  final Map<String,LinearLayout> lists=new LinkedHashMap<>();
  final int BG=Color.rgb(10,10,10), PANEL=Color.rgb(24,24,24), FIELD=Color.rgb(36,36,36), GOLD=Color.rgb(212,175,55), TEXT=Color.rgb(245,245,245), MUTED=Color.rgb(165,165,165);

  @Override public void onCreate(Bundle b){
    super.onCreate(b); load(); setContentView(ui()); refresh();
  }

  ScrollView ui(){
    ScrollView s=new ScrollView(this); s.setBackgroundColor(BG);
    LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(dp(18),dp(18),dp(18),dp(28)); r.setBackgroundColor(BG); s.addView(r);
    r.addView(t("Koda Cut",28,true,TEXT));
    TextView badge=t("ANDROID • BETA 0.1",12,true,GOLD); badge.setPadding(0,dp(2),0,dp(14)); r.addView(badge);
    TextView intro=t("Primeiro teste Android: adicione cada arquivo no setor correto. O app cria automaticamente os IDs e o mapa para a IA.",14,false,MUTED); intro.setPadding(0,0,0,dp(16)); r.addView(intro);
    sector(r,"main","VÍDEO PRINCIPAL","Adicionar vídeo","video/*",VIDEO,false);
    sector(r,"images","IMAGENS / PNGs","Adicionar imagens","image/*",IMAGES,true);
    sector(r,"sfx","EFEITOS SONOROS","Adicionar efeitos","audio/*",SFX,true);
    sector(r,"music","MÚSICAS","Adicionar músicas","audio/*",MUSIC,true);
    sector(r,"broll","B-ROLL","Adicionar B-roll","video/*",BROLL,true);
    Button copy=btn("COPIAR MAPA PARA IA",true); copy.setOnClickListener(v->copy()); r.addView(copy,lp(12));
    Button share=btn("COMPARTILHAR PROJETO",false); share.setOnClickListener(v->share()); r.addView(share,lp(8));
    Button clear=btn("LIMPAR PROJETO",false); clear.setOnClickListener(v->{assets.clear();save();refresh();toast("Projeto limpo.");}); r.addView(clear,lp(8));
    return s;
  }

  void sector(LinearLayout root,String key,String title,String label,String mime,int req,boolean multi){
    LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(14),dp(14),dp(14),dp(14)); c.setBackgroundColor(PANEL);
    c.addView(t(title,15,true,TEXT));
    LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(0,dp(8),0,dp(8)); c.addView(l); lists.put(key,l);
    Button b=btn("+ "+label,false); b.setOnClickListener(v->pick(mime,req,multi)); c.addView(b);
    root.addView(c,lp(12));
  }

  void pick(String mime,int req,boolean multi){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType(mime); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multi);
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(i,req);
  }

  @Override protected void onActivityResult(int req,int result,Intent data){
    super.onActivityResult(req,result,data); if(result!=RESULT_OK||data==null)return;
    String sec=sec(req); if(sec==null)return; if("main".equals(sec)) assets.removeIf(a->"main".equals(a.sector));
    if(data.getClipData()!=null){ClipData cd=data.getClipData(); for(int x=0;x<cd.getItemCount();x++)add(sec,cd.getItemAt(x).getUri());}
    else if(data.getData()!=null)add(sec,data.getData());
    save(); refresh();
  }

  void add(String sec,Uri uri){
    try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
    String name=name(uri); if(name==null||name.isBlank())name="arquivo";
    for(Asset a:assets)if(a.uri.equals(uri.toString())&&a.sector.equals(sec))return;
    assets.add(new Asset(sec,name,uri.toString(),id(sec,name)));
  }

  void refresh(){
    for(Map.Entry<String,LinearLayout> e:lists.entrySet()){
      LinearLayout l=e.getValue(); l.removeAllViews(); boolean any=false;
      for(Asset a:new ArrayList<>(assets)){
        if(!e.getKey().equals(a.sector))continue; any=true;
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView tv=t(a.name+"\n"+a.id,12,false,TEXT); tv.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1)); row.addView(tv);
        Button x=btn("×",false); x.setOnClickListener(v->{assets.remove(a);save();refresh();}); row.addView(x); l.addView(row);
      }
      if(!any)l.addView(t("Nenhum arquivo adicionado.",12,false,MUTED));
    }
  }

  String prompt(){
    StringBuilder s=new StringBuilder("MODO KODA CUT ANDROID — DIRETOR DE EDIÇÃO\n\n");
    s.append("Você vai me ajudar a planejar uma edição para o Koda Cut Android.\n");
    s.append("Converse comigo até entender referências, ritmo, cortes, zooms, legendas, imagens, B-roll, efeitos e música.\n");
    s.append("Use SOMENTE os IDs do MAPA DE ARQUIVOS. Não invente assets.\n");
    s.append("Quando eu disser \"pode começar a editar\", gere o KodaScript JSON final em um único bloco de código.\n\nMAPA DE ARQUIVOS:\n");
    if(assets.isEmpty())s.append("(nenhum arquivo adicionado)\n");
    else for(Asset a:assets)s.append(a.id).append(" -> ").append(a.name).append(" [").append(label(a.sector)).append("]\n");
    return s.toString();
  }

  void copy(){
    ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Koda Project",prompt())); toast("Mapa copiado.");
  }
  void share(){
    Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,prompt()); startActivity(Intent.createChooser(i,"Enviar projeto para"));
  }

  String id(String sec,String name){
    String b=name; int d=b.lastIndexOf('.'); if(d>0)b=b.substring(0,d); b=slug(b);
    if("main".equals(sec))return "video:principal";
    String p=("images".equals(sec)?"image:":"sfx".equals(sec)?"audio:":"music".equals(sec)?"music:":"video:")+b;
    String c=p; int n=2; while(exists(c))c=p+"_"+n++; return c;
  }
  boolean exists(String id){for(Asset a:assets)if(a.id.equals(id))return true;return false;}
  String slug(String v){String s=Normalizer.normalize(v,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","_").replaceAll("^_+|_+$",""); return s.isBlank()?"arquivo":s;}
  String name(Uri u){try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)return c.getString(i);}}catch(Exception ignored){} return u.getLastPathSegment();}
  String sec(int r){return r==VIDEO?"main":r==IMAGES?"images":r==SFX?"sfx":r==MUSIC?"music":r==BROLL?"broll":null;}
  String label(String s){return "main".equals(s)?"vídeo principal":"images".equals(s)?"imagem/PNG":"sfx".equals(s)?"efeito sonoro":"music".equals(s)?"música":"B-roll";}

  void save(){try{JSONArray a=new JSONArray();for(Asset x:assets){JSONObject o=new JSONObject();o.put("s",x.sector);o.put("n",x.name);o.put("u",x.uri);o.put("i",x.id);a.put(o);}getSharedPreferences("koda",0).edit().putString("assets",a.toString()).apply();}catch(Exception ignored){}}
  void load(){try{JSONArray a=new JSONArray(getSharedPreferences("koda",0).getString("assets","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);assets.add(new Asset(o.optString("s"),o.optString("n"),o.optString("u"),o.optString("i")));}}catch(Exception ignored){}}

  Button btn(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(12);b.setTextColor(primary?Color.BLACK:TEXT);b.setBackgroundColor(primary?GOLD:FIELD);return b;}
  TextView t(String s,int size,boolean bold,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);return v;}
  LinearLayout.LayoutParams lp(int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);return p;}
  int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
  static class Asset{final String sector,name,uri,id;Asset(String s,String n,String u,String i){sector=s;name=n;uri=u;id=i;}}
}
