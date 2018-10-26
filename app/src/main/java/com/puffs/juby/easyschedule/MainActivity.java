package com.puffs.juby.easyschedule;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class MainActivity extends AppCompatActivity
{
    private WebView webView;
    private String sClass;
    private String courseNum;
    private String section;
    private String begin;
    private String end;
    private String days;
    private String building;
    private String Room;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = (WebView) findViewById(R.id.webView);
    }

    public void submitInfo(View view) throws InterruptedException, IOException
    {

        EditText txt = (EditText) findViewById(R.id.pass_edit);
        Editable edit = txt.getText();
        final String pass = edit.toString();

        txt = (EditText) findViewById(R.id.user_edit);
        edit = txt.getText();
        final String user = edit.toString();

        //webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.addJavascriptInterface(new MyJavaScriptInterface(), "HtmlHandler");
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url)
            {
                super.onPageFinished(view, url);
                if(url.equals("https://myproxy01.apps.lsu.edu/reg%5CSchedule.nsf/SRRVSCH"))
                {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            webView.evaluateJavascript("javascript:window.HtmlHandler.handleHtml" +
                                    "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');", null);
                        }
                    }, 8000);

                }

            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                view.loadUrl(url);
                return true;
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon)
            {
                super.onPageStarted(view, url, favicon);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress)
            {
                if(progress == 100)
                {
                    if(view.getUrl().equals("https://sso.paws.lsu.edu/login"))
                    {
                        webView.evaluateJavascript("javascript:(function(){document.getElementById('username').value ='"
                                + user
                                + "';document.getElementById('password').value ='"
                                + pass + "';document.getElementById('LoginButton').click();})()",null);
                    }
                }
            }
        });

        webView.loadUrl("https://sso.paws.lsu.edu/login");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("https://myproxy01.apps.lsu.edu/reg%5CSchedule.nsf/SRRVSCH");
            }
        }, 8000);




    }

    private class MyJavaScriptInterface {
        @JavascriptInterface
        public void handleHtml(String html) {
            // Use jsoup on this String here to search for your content.
            Document doc = Jsoup.parse(html);

            // Now you can, for example, retrieve a div with id="username" here
            Elements elements = doc.getElementsByTag("font");
            int steps = 1;
            for(int i = 26; elements.get(i).equals("Total Hours"))
            {
                if(elements.get(i).hasText())
                {
                    switch (steps)
                    {
                        case 1: sClass = elements.get(i).text();
                                steps++;
                                break;
                        case 2: courseNum = elements.get(i).text();
                                steps++;
                                break;
                    }
                }
            }
        }
    }
}


