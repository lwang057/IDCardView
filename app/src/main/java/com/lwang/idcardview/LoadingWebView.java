package com.lwang.idcardview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;


public class LoadingWebView extends WebView {

    private ProgressBar mProgressBar;
    private Context context;
    private int currentProgress;
    private boolean isAnimStart;

    public LoadingWebView(Context context) {
        super(context, null);
        initContext(context);
        this.context = context;
    }

    public LoadingWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initContext(context);
    }

    public LoadingWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initContext(context);
    }

    private void initContext(Context context) {
        //支持内容重新布局
        getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        //设置编码
        getSettings().setDefaultTextEncodingName("UTF-8");
        //设置是否支持javascript
        getSettings().setJavaScriptEnabled(true);
        //支持通过JS打开新窗口
        getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        //支持缩放，默认为true
        getSettings().setSupportZoom(false);
        //设置可以缩放
        getSettings().setBuiltInZoomControls(true);
        //任意比例缩放调整到适合webview的大小
        getSettings().setUseWideViewPort(false);
        //缩放至屏幕的大小
        getSettings().setLoadWithOverviewMode(true);

        //水平与垂直都不显示滚动条
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);

        // 开启 DOM storage API 功能
        getSettings().setDomStorageEnabled(true);
        //开启 Application Caches 功能
        getSettings().setAppCacheEnabled(true);
        //根据cache-control决定是否从网络上取数据
        getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        //设置可以访问文件
        getSettings().setAllowFileAccess(true);

        // 禁止即在网页顶出现一个空白，又自动回去。
        setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    /**
     * 加载网页url
     *
     * @param url
     */
    public void loadMessageUrl(String url) {
        loadUrl(url);
        addJavascriptInterface(new AndroidtoJs(), "IDCardRead");
        setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
    }

    /**
     * 显示身份证信息
     *
     * @param data
     */
    public void setIDCardData(Object data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript("showIDCard(" + data + ")", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                }
            });
        } else {
            loadUrl("javascript:showIDCard(" + data + ")");
        }
    }

    /**
     * 显示IC卡信息
     *
     * @param data
     */
    public void setICCardData(Object data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript("showICCard(" + data + ")", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                }
            });
        } else {
            loadUrl("javascript:showICCard(" + data + ")");
        }
    }

    /**
     * 添加进度条
     */
    public void addProgressBar() {
        mProgressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        mProgressBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 5, 0, 0));
        mProgressBar.setProgressDrawable(getContext().getResources().getDrawable(R.drawable.bg_pb_web_loading));
        addView(mProgressBar);//添加进度条至LoadingWebView中
        setWebChromeClient(new WebChromeClient());//设置setWebChromeClient对象
    }

    public class WebChromeClient extends android.webkit.WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (null == mProgressBar) return;
            currentProgress = mProgressBar.getProgress();
            if (newProgress == 100 && !isAnimStart == true) {
                isAnimStart = true;
                mProgressBar.setProgress(newProgress);
                // 开启属性动画让进度条平滑消失
                startDismissAnimation(mProgressBar.getProgress());
            } else {
                mProgressBar.setVisibility(VISIBLE);
                //mProgressBar.setProgress(newProgress);
                // 开启属性动画让进度条平滑递增
                startProgressAnimation(newProgress);
            }
            super.onProgressChanged(view, newProgress);
        }
    }

    /**
     * progressBar递增动画
     */
    private void startProgressAnimation(int newProgress) {
        ObjectAnimator animator = ObjectAnimator.ofInt(mProgressBar, "progress", currentProgress, newProgress);
        animator.setDuration(1500);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void startDismissAnimation(final int progress) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mProgressBar, "alpha", 1.0f, 0.0f);
        anim.setDuration(1000);  // 动画时长
        anim.setInterpolator(new DecelerateInterpolator());     // 减速
        // 关键, 添加动画进度监听器
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fraction = valueAnimator.getAnimatedFraction();      // 0.0f ~ 1.0f
                int offset = 100 - progress;
                if (null != mProgressBar) {
                    mProgressBar.setProgress((int) (progress + offset * fraction));
                }
            }
        });

        anim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                // 动画结束
                mProgressBar.setProgress(0);
                mProgressBar.setVisibility(View.GONE);
                isAnimStart = false;
            }
        });
        anim.start();
    }

    public class AndroidtoJs extends Object {

        // 定义JS需要调用的方法，JS调用的方法必须加入@JavascriptInterface注解
        @JavascriptInterface
        public void setOnReadClick() {
            if (onIDCardClick != null) {
                onIDCardClick.setClick();
            }
        }

        @JavascriptInterface
        public void setOnICCardClick() {
            if (onICCardClick != null) {
                onICCardClick.setClick();
            }
        }
    }

    private OnIDCardClick onIDCardClick;

    public interface OnIDCardClick {
        public void setClick();
    }

    public void setOnIDCardClick(OnIDCardClick onIDCardClick) {
        this.onIDCardClick = onIDCardClick;
    }


    private OnICCardClick onICCardClick;

    public interface OnICCardClick {
        public void setClick();
    }

    public void setOnICCardClick(OnICCardClick onICCardClick) {
        this.onICCardClick = onICCardClick;
    }

}
