package com.cwuom.chatroom;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.impl.FullScreenPopupView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    private WebView mWvWebView;
    private BasePopupView mXpLoad;
    private BasePopupView mXpNetLoad;
    private int intPdShow = 0;
    private int pdMsgNumTemp = 0;
    private int pdMsgNum = 0;

    private Handler handler = new Handler();

    // 保活
    private static final String TAG = "[MainActivity]";
    OnePixelManager manager;


    public static class MusicName {
        private static int name = 0;

        public static int getName() {
            return name;
        }

        public static void setName(int name) {
            MusicName.name = name;
        }
    }

    MediaPlayer mp=null;//声明一个MediaPlayer对象
    private void playBGSound(){
        if(mp!=null){
            mp.release();//释放资源
        }
        while (MusicName.getName()==0);
        //播放的曲目取决于MusicName的值
        mp=MediaPlayer.create(MainActivity.this, MusicName.getName());
        mp.start();
    }
    //**

    /*
        while (true){
        if (!checkConnectNetwork(MainActivity.this)) {
            // 判断是否已经弹出
            if(intPdShow == 0){
                mXpNetLoad.show();
                intPdShow = 1;
            }
        }else{
            if(intPdShow == 1){
                mXpNetLoad.dismiss();
                intPdShow = 0;
            }
        }
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        manager = new OnePixelManager();
        manager.registerOnePixelReceiver(this);//注册广播接收者

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if(!checkConnectNetwork(MainActivity.this)){
                        if(intPdShow == 0){
                            mXpNetLoad = new XPopup.Builder(MainActivity.this)
                                    .dismissOnBackPressed(false)
                                    .dismissOnTouchOutside(false)
                                    .hasBlurBg(true)
                                    .asLoading("检测互联网的连接...");
                            mXpNetLoad.show();
                            intPdShow = 1;
                        }
                    }else{
                        // 关闭弹出的窗口
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(intPdShow == 1){
                                    mXpNetLoad.dismiss();
                                    intPdShow = 0;
                                }
                            }
                        });
                    }
                }
            }
        }).start();


        // 创建一个SharedPreferences对象
        SharedPreferences sharedPreferences = getSharedPreferences("data", Context.MODE_PRIVATE);

        // ===================================初次使用===================================

        Boolean isStart = sharedPreferences.getBoolean("start", false);
        if (isStart == Boolean.FALSE) {
            new XPopup.Builder(MainActivity.this)
                    .hasBlurBg(true)
                    .asCustom(new CustomFullScreenPopup(MainActivity.this))
                    .show();
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // 记录已经打开过此程序
        editor.putBoolean("start", true);
        // 提交记录
        editor.commit();


        // =============================================================================



        // *findViewById*
        mWvWebView = findViewById(R.id.wv_webview);

        // 弹窗
        mXpLoad = new XPopup.Builder(MainActivity.this)
                .hasBlurBg(true)
                .asLoading("正在初始化...");


        // 加载到服务器
        mWvWebView.loadUrl("http://47.103.104.183:99/");
        mWvWebView.setWebChromeClient(new WebChromeClient());
        mWvWebView.setWebViewClient(new WebViewClient());
        mWvWebView.getSettings().setJavaScriptEnabled(true); // JS交互

        // 设置字体大小(防止显示不完全)
        WebSettings settings = mWvWebView.getSettings();
        settings.setTextZoom(100);

        // =================================================新消息监听=================================================


        mWvWebView.setWebViewClient(new WebViewClient() {
            @RequiresApi(api = Build.VERSION_CODES.Q)

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // 显示初始化弹窗
                mXpLoad.show();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // 弹窗消失
                mXpLoad.dismiss();
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // http://47.103.104.183:99/act/updates/0/0/on/0/(22)/1/1/0/0/0/no/

                //添加Cookie获取操作
                CookieManager cookieManager = CookieManager.getInstance();
                final String cookieStr = cookieManager.getCookie(request.getUrl() + "");
                // {"unseenalerts":{"liveup":"unseenalerts","total":"10"}}
                try {
                    // 测试输出，方便挑错
                    Log.e("InternetActivity", request.getUrl() + "---" + "\n" + getPageResource(request.getUrl() + "", cookieStr));

//                    Log.e("testUrl",strUrl);

                    if(String.valueOf(request.getUrl()).contains("http://47.103.104.183:99/act/updates/")){
                        pdMsgNum = getMsgNum(String.valueOf(request.getUrl()));

                        if(pdMsgNumTemp != 0){
                            Log.i("日志", pdMsgNum + "<----->" + pdMsgNumTemp);
                            if(pdMsgNumTemp < pdMsgNum){
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        playRaw();
                                        Looper.prepare();
                                        //加载Toast布局
                                        View toastRoot = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_toast, null);
                                        Toast toastStart = new Toast(MainActivity.this);
                                        //获取屏幕高度
                                        WindowManager wm = (WindowManager) MainActivity.this.getSystemService(Context.WINDOW_SERVICE);
                                        int height = wm.getDefaultDisplay().getHeight();
                                        //Toast的Y坐标是屏幕高度的1/3，不会出现不适配的问题
                                        toastStart.setGravity(Gravity.TOP, 0, height / 3);
                                        toastStart.setDuration(Toast.LENGTH_LONG);
                                        toastStart.setView(toastRoot);
                                        toastStart.show();
                                        Looper.loop();
                                    }
                                }).start();

                                // 重新刷新Temp
                                pdMsgNumTemp = getMsgNum(String.valueOf(request.getUrl()));

                                return super.shouldInterceptRequest(view, request);
                            }

                            // 重新刷新Temp
                            pdMsgNumTemp = getMsgNum(String.valueOf(request.getUrl()));
                        }


                        // 重新刷新Temp
                        pdMsgNumTemp = getMsgNum(String.valueOf(request.getUrl()));

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                // http://47.103.104.183:99/act/updates/0/0/on/0/0/0/0/0/0/0/no/
                return super.shouldInterceptRequest(view, request);
            }
        });

//        // http://47.103.104.183:99/act/updates/0/0/on/0/0/0/0/0/0/0/no/
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true){
//                    try {
//                        CookieManager cookieManager = CookieManager.getInstance();
//                        final String cookieStr = cookieManager.getCookie("http://47.103.104.183:99/act/updates/");
//                        Log.e("报告:", getPageResource("http://47.103.104.183:99/act/updates/", cookieStr));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();



        // ============================================================================================================


    }

    public void playRaw(){
        MusicName.setName(R.raw.alert005);
        Thread music_thread;
        music_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                playBGSound();//播放背景音乐
            }
        });
        music_thread.start();//开启线程
    }

    public void ifSendMsg(){
        TextView mTextView;
        //加载Toast布局
        View toastRoot = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_toast, null);
        Toast toastStart = new Toast(MainActivity.this);
        //获取屏幕高度
        WindowManager wm = (WindowManager) MainActivity.this.getSystemService(Context.WINDOW_SERVICE);
        int height = wm.getDefaultDisplay().getHeight();
        //Toast的Y坐标是屏幕高度的1/3，不会出现不适配的问题
        toastStart.setGravity(Gravity.TOP, 0, height / 3);
        toastStart.setDuration(Toast.LENGTH_LONG);
        toastStart.setView(toastRoot);
        toastStart.show();
    }

    // 返回上一个页面
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWvWebView.canGoBack()) {
            mWvWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);//退出界面
    }


    // 自定义弹窗
    public class CustomFullScreenPopup extends FullScreenPopupView {
        public CustomFullScreenPopup(@NonNull Context context) {
            super(context);
        }
        @Override
        protected int getImplLayoutId() {
            return R.layout.layout_xpopup;
        }
        @Override
        protected void onCreate() {
            super.onCreate();
            //初始化
            findViewById(R.id.btn_ok).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss(); // 关闭弹窗
                }
            });
        }

        class MyHandler extends Handler {
            private final WeakReference<MainActivity> mTarget;

            public MyHandler(MainActivity activity) {
                mTarget = new WeakReference<MainActivity>(activity);
            }

            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    if(intPdShow == 0){
                        mXpNetLoad.show();
                        intPdShow = 1;
                    }
                }
                if (msg.what == 1){
                    if(intPdShow == 1){
                        mXpNetLoad.dismiss();
                        intPdShow = 0;
                    }
                }
            }
        }
    }
    //判断是否联网函数
    private boolean checkConnectNetwork(Context context) {

        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = conn.getActiveNetworkInfo();
        if (net != null && net.isConnected()) {
            return true;
        }
        return false;
    }

    // 获取网页源码 需要传入cookie
    public String getPageResource(String pageURL, String cookie_) throws IOException {

        String urlPath = pageURL;
        String cookie = cookie_;
        URL url = new URL(urlPath);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Cookie", cookie);
        conn.setDoInput(true);
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        return  ""+sb;
    }

    // 获取消息条数
    public int getMsgNum(String str){
        String[] split = str.split("\\/");
        Log.e("分割", split[9]);
        return Integer.parseInt(split[9]);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        manager.unregisterOnePixelReceiver(this);//Activity退出时解注册
    }

}
