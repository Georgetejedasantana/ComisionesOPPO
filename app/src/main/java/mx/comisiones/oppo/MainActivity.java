package mx.comisiones.oppo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.*;
import android.widget.Toast;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.JSONObject;
import java.io.OutputStream;
import java.util.Collections;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final String HOST = "comisiones-oppo-george-demo.vercel.app";
    private static final String HOME = "https://" + HOST + "/";
    private static final int SAVE_CSV = 100;
    private static final int MAX_CSV = 1024 * 1024;
    private WebView web;
    private byte[] pendingCsv;
    private String pendingNonce;
    private boolean csvSupported;

    private boolean trusted(Uri uri) {
        return uri != null && "https".equals(uri.getScheme()) && HOST.equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(0xff071c14);
        setContentView(web);
        WebView.setWebContentsDebuggingEnabled(false);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (trusted(u)) return false;
                if (r.isForMainFrame() && "https".equals(u.getScheme()) && r.hasGesture()) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, u)); }
                    catch (ActivityNotFoundException ignored) { toast("No hay navegador disponible."); }
                }
                return true;
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) new AlertDialog.Builder(MainActivity.this)
                    .setTitle("No se pudo conectar")
                    .setMessage("Comprueba tu conexión a internet e inténtalo de nuevo.")
                    .setPositiveButton("Reintentar", (d,w) -> web.loadUrl(HOME))
                    .setNegativeButton("Cerrar", (d,w) -> finish()).show();
            }
            // Default SSL error handling cancels unsafe connections. No bypass.
        });
        csvSupported = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER);
        if (csvSupported) WebViewCompat.addWebMessageListener(web, "NativeCsv", Collections.singleton("https://"+HOST),
            (view, message, origin, mainFrame, reply) -> {
                if (!mainFrame || !trusted(origin) || !trusted(Uri.parse(web.getUrl()==null?"":web.getUrl()))) return;
                try {
                    JSONObject json = new JSONObject(message.getData());
                    if (pendingNonce == null || !pendingNonce.equals(json.optString("nonce"))) return;
                    pendingNonce = null;
                    String data = json.optString("data");
                    if (!data.startsWith("data:") || data.length() > MAX_CSV*2) throw new Exception("Tamaño");
                    int comma = data.indexOf(',');
                    if (comma<0 || !data.substring(0,comma).endsWith(";base64")) throw new Exception("Formato");
                    byte[] bytes = Base64.decode(data.substring(comma+1),Base64.DEFAULT);
                    if (bytes.length > MAX_CSV) throw new Exception("Tamaño");
                    pendingCsv = bytes;
                    Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("text/csv").putExtra(Intent.EXTRA_TITLE,"comisiones-oppo.csv");
                    startActivityForResult(save,SAVE_CSV);
                } catch (Exception e) { pendingCsv=null; toast("No se pudo exportar el CSV."); }
            });
        web.setDownloadListener((url,agent,disposition,mime,length) -> {
            String current = web.getUrl();
            if (!csvSupported || current==null || !trusted(Uri.parse(current)) || url==null || !url.startsWith("blob:https://"+HOST+"/") || pendingCsv!=null) {
                toast("Descarga no disponible. Puedes exportar desde la web en Chrome."); return;
            }
            pendingNonce=UUID.randomUUID().toString();
            String nonce=pendingNonce;
            // Only the trusted top-level page receives the nonce. No general file bridge.
            web.evaluateJavascript("(()=>{const n="+JSONObject.quote(nonce)+";fetch("+JSONObject.quote(url)+").then(r=>r.blob()).then(b=>{if(b.size>1048576)throw Error();const reader=new FileReader();reader.onload=()=>NativeCsv.postMessage(JSON.stringify({nonce:n,data:reader.result}));reader.readAsDataURL(b);}).catch(()=>NativeCsv.postMessage(JSON.stringify({nonce:n,data:''})));})()",null);
        });
        web.loadUrl(HOME);
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if (request!=SAVE_CSV) return;
        byte[] bytes=pendingCsv; pendingCsv=null;
        if (result!=RESULT_OK || data==null || data.getData()==null || bytes==null) return;
        try (OutputStream out=getContentResolver().openOutputStream(data.getData(),"w")) {
            if(out==null) throw new Exception(); out.write(bytes); toast("CSV guardado.");
        } catch(Exception e) { toast("No se pudo guardar el archivo."); }
    }
    @Override public void onBackPressed() {
        if(web.canGoBack()) web.goBack();
        else new AlertDialog.Builder(this).setMessage("¿Salir de Comisiones OPPO?")
            .setPositiveButton("Salir",(d,w)->finish()).setNegativeButton("Continuar",null).show();
    }
    @Override protected void onDestroy() {
        if(web!=null) { web.stopLoading(); web.destroy(); web=null; }
        super.onDestroy();
    }
}
