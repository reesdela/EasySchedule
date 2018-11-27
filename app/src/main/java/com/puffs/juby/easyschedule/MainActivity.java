package com.puffs.juby.easyschedule;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity
{
    private WebView webView;
    private String user = "";
    private String pass = "";
    private ContentResolver cr;
    private ContentValues values = new ContentValues();
    private ArrayList<String> schedInfo = new ArrayList<>();
    private ArrayList<String> secondaryInfo = new ArrayList<>(); //probaly dont acutally need this varibale but for right now keeping it
    private Dialog dialog;
    private int numPass = 0;
    private int startD = 0;
    private ProgressBar progressBar;
    CookieManager manager = CookieManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager.setAcceptCookie(true);
        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setSavePassword(false);
        webView.getSettings().setSaveFormData(false);
        String file = readFromFile();
        if (file.equals("done"))
        {
            dialog = new Dialog(this);
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.popup_window);
            dialog.show();
        }
        progressBar = findViewById(R.id.prog_bar);
        progressBar.setEnabled(false);
        //progressBar.setEnabled(false);
        cr = getApplicationContext().getContentResolver();
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_CALENDAR}, 1);
        for(int i = 0; i < 9; i++)
        {
            secondaryInfo.add("");
        }
    }

    public String readFromFile()
    {
        String receiveString = "";
        try
        {
            InputStream input = getApplicationContext().openFileInput("done.txt");
            if(input != null)
            {
                InputStreamReader inputReader = new InputStreamReader(input);
                BufferedReader bufferedReader = new BufferedReader(inputReader);
                receiveString = bufferedReader.readLine();
                return receiveString;
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return receiveString;
    }

    public void yesSure(View view)
    {
        dialog.dismiss();
    }

    public void noNvm(View view)
    {
        System.exit(0);
    }

    public void done()
    {
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        progressBar.setEnabled(false);
        progressBar.setEnabled(false);
        manager.removeSessionCookies(new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean value) {
            }
        });

    }

    public void start()
    {
        progressBar.setEnabled(true);
        progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    public String getDaysFormat(String days)
    {
        String formatted = "";
        int numTH = StringUtils.countMatches(days, "TH");
        int numT = StringUtils.countMatches(days, "T");

        if(days.contains("M"))
        {
            formatted = formatted.concat("MO,");
        }
        if(numT == 1)
        {
            if(numTH > 0)
            {
                formatted = formatted.concat("TH,");
            }
            else
            {
                formatted = formatted.concat("TU,");
            }

        }
        else if(numT == 2)
        {
            formatted = formatted.concat("TU,");
            formatted = formatted.concat("TH,");
        }
        if(days.contains("W"))
        {
            formatted = formatted.concat("WE,");
        }
        if(days.contains("F"))
        {
            formatted = formatted.concat("FR,");
        }
        if(formatted.endsWith(","))
        {
            formatted = formatted.substring(0, formatted.length()-1);
        }
        String duh = "";

        if(days.length() > 1)
        {
          duh = days.substring(0,2);
        }
        else
        {
           duh = days;
        }

        while(startD == 0)
        {
            switch (duh) {
                case "TH":
                    startD = Calendar.THURSDAY;
                    break;
                case "M":
                    startD = Calendar.MONDAY;
                    break;
                case "T":
                    startD = Calendar.TUESDAY;
                    break;
                case "W":
                    startD = Calendar.WEDNESDAY;
                    break;
                case "F":
                    startD = Calendar.FRIDAY;
                default:
                    duh = days.substring(0, 1);
                    break;
            }
        }

        return formatted;
    }

    public void makeCalendar(ArrayList<String> info)
    {
        String endDate = "";
        String hourDay = convertTime(info.get(3), info.get(5));
        TimeZone timeZone = TimeZone.getTimeZone("America/Chicago");
        Calendar beginTime = Calendar.getInstance(timeZone);
        int we = beginTime.get(Calendar.DAY_OF_WEEK);
        String byDay = getDaysFormat(info.get(6));
        ZonedDateTime date;
        if(Calendar.DAY_OF_WEEK <= startD)
        {
            date = ZonedDateTime.of(LocalDate.now(ZoneId.of("America/Chicago")).plusDays(startD - we), LocalTime.parse(hourDay, DateTimeFormatter.ofPattern("h:m:s a").withLocale(Locale.US)), ZoneId.of("America/Chicago"));
        }
        else
        {
            date = ZonedDateTime.of(LocalDate.now(ZoneId.of("America/Chicago")).minusDays(we - startD), LocalTime.parse(hourDay, DateTimeFormatter.ofPattern("h:m:s a").withLocale(Locale.US)), ZoneId.of("America/Chicago"));
        }



        if(beginTime.get(Calendar.MONTH) >= 8)
        {
            beginTime.setTime(Date.from(date.toInstant()));
            endDate = String.valueOf(beginTime.get(Calendar.YEAR));
            endDate = endDate + "1207T000000Z";
        }
        else
        {
            beginTime.setTime(Date.from(date.toInstant()));
            endDate = String.valueOf(beginTime.get(Calendar.YEAR));
            endDate = endDate +"0520T000000Z";
        }

            long sinMILI = beginTime.getTimeInMillis();



            values.put(CalendarContract.Events.CALENDAR_ID, 3);
            values.put(CalendarContract.Events.DTSTART, sinMILI);
            values.put(CalendarContract.Events.TITLE, info.get(0) + " " + info.get(1));
            values.put(CalendarContract.Events.DESCRIPTION, "You have class at " + info.get(7) + " room " + info.get(8) + "!");

            values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());

            values.put(CalendarContract.Events.DURATION, "+P1H");
            values.put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=" + byDay + ";UNTIL=" + endDate);

            values.put(CalendarContract.Events.HAS_ALARM, 1);
            values.put(CalendarContract.Events.ALL_DAY, false);
            String uString = "content://com.android.calendar/events";
            Uri uri = cr.insert(Uri.parse(uString), values);
            startD = 0;
    }

    public String convertTime(String time, String night)
    {
        StringBuffer sb = new StringBuffer(time);
        int numTime = Integer.parseInt(time);
        if(time.length() > 3)
        {
            if(time.charAt(1) == '2')
            {
                sb.insert(2, ":");
                sb.append(":00 PM");
                time = sb.toString();
            }
            else
            {
                sb.insert(2, ":");
                sb.append(":00 AM");
                time = sb.toString();
            }
        }
        else
        {
            if(numTime >= 700)
            {
                if(!(night == null))
                {
                    sb.insert(1, ":");
                    sb.append(":00 PM");
                    time = sb.toString();
                }
                else
                {
                    sb.insert(1, ":");
                    sb.append(":00 AM");
                    time = sb.toString();
                }
            }
            else
            {
                sb.insert(1, ":");
                sb.append(":00 PM");
                time = sb.toString();
            }
        }

        return time;
    }


    public void submitInfo(View view)
    {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressBar.setEnabled(true);
        Toast toast = Toast.makeText(this, "Please Wait As your Calendar is being Set Up.", Toast.LENGTH_LONG);
        toast.show();
        EditText txt = (EditText) findViewById(R.id.pass_edit);
        Editable edit = txt.getText();
        pass = edit.toString();

        txt = (EditText) findViewById(R.id.user_edit);
        edit = txt.getText();
        user = edit.toString();

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
                if(url.equals("https://mylsu.apps.lsu.edu/group/mycampus/home"))
                {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            webView.loadUrl("https://myproxy01.apps.lsu.edu/reg%5CSchedule.nsf/SRRVSCH");
                            start();

                        }
                    }, 8000);
                }
                if(url.equals("https://sso.paws.lsu.edu/login"))
                {
                    numPass++;
                    webView.evaluateJavascript("javascript:document.getElementById('password').value='" + pass + "';" + "document.getElementById('username').value='" + user + "';" +
                            "document.getElementById('LoginButton').click()", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {

                        }
                    });
                }

            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(webView.getUrl().equals("https://sso.paws.lsu.edu/login") && numPass > 0)
                        {
                            TextView textView = (TextView) findViewById(R.id.invalid);
                            textView.setVisibility(TextView.VISIBLE);
                            progressBar.setVisibility(ProgressBar.INVISIBLE);
                            progressBar.setEnabled(false);
                            webView.stopLoading();
                        }
                    }
                }, 3000);

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
            Document doc = Jsoup.parse(html);
            Elements elements = doc.getElementsByTag("font");
            int steps = 1;
            for(int i = 26; !(elements.get(i).text().equals("Total Hours")); i++)
            {
                if ((elements.get(i).text().length() > 0 && !(elements.get(i).text().contains("."))) || steps == 6)
                {
                    switch (steps)
                    {
                        case 1:
                            schedInfo.add(elements.get(i).text());
                            steps++;
                            break;
                        case 2:
                            schedInfo.add(elements.get(i).text());
                            steps++;
                            break;
                        case 3:
                            schedInfo.add(elements.get(i).text());
                            steps++;
                            break;
                        case 4:
                            if(secondaryInfo.get(0).length() > 0)
                            {
                                secondaryInfo.set(steps-1, elements.get(i).text());
                            }
                            else {
                                schedInfo.add(elements.get(i).text());
                            }
                            steps++;
                            break;
                        case 5:
                            if(secondaryInfo.get(0).length() > 1)
                            {
                                secondaryInfo.set(steps-1, elements.get(i).text());
                            }
                            else {
                                schedInfo.add(elements.get(i).text());
                            }
                            steps++;
                            break;
                        case 6:
                            if(secondaryInfo.get(0).length() > 1)
                            {
                                if(elements.get(i).text().length() > 0)
                                {
                                    secondaryInfo.set(steps-1, elements.get(i).text());
                                }
                                else
                                {
                                    secondaryInfo.set(steps-1, null);
                                }
                            }
                            else {
                                if (elements.get(i).text().length() > 0) {
                                    schedInfo.add(elements.get(i).text());
                                } else {
                                    schedInfo.add(null);
                                }
                            }
                            steps++;
                            break;
                        case 7:
                            if(secondaryInfo.get(0).length() > 1)
                            {
                                secondaryInfo.set(steps-1, elements.get(i).text());
                            }
                            else {
                                schedInfo.add(elements.get(i).text());
                            }
                            steps++;
                            break;
                        case 8:
                            if(secondaryInfo.get(0).length() > 1)
                            {
                                secondaryInfo.set(steps-1,elements.get(i).text());
                            }
                            else {
                                schedInfo.add(elements.get(i).text());
                            }
                            steps++;
                            break;
                        case 9:
                            if(secondaryInfo.get(0).length() > 1)
                            {
                                secondaryInfo.set(steps-1, elements.get(i).text());
                            }
                            else
                            {
                                schedInfo.add(elements.get(i).text());
                            }
                            if(elements.get(i+2).text().length() < 1)
                            {
                                for(int x = 0; x < 3; x++)
                                {
                                    secondaryInfo.set(x, schedInfo.get(x));
                                }
                            }
                            i++;
                            steps++;
                            break;
                        default: break;
                    }

                }
                else
                {
                    if(steps == 1)
                    {
                        steps = 4;
                    }
                    else if(steps == 8)
                    {
                        secondaryInfo.set(steps-1, elements.get(i-12).text());
                        secondaryInfo.set(steps, elements.get(i-11).text());
                        i = i + 2;
                        steps = 10;
                    }
                }
                if(steps == 10)
                {
                    if(secondaryInfo.get(0).length() > 0 && schedInfo.size() < 1)
                    {
                        makeCalendar(secondaryInfo);
                        secondaryInfo = new ArrayList<>();
                        for(int p = 0; p < 9; p++)
                        {
                            secondaryInfo.add("");
                        }
                        steps = 1;
                    }
                    else {
                        makeCalendar(schedInfo);
                        schedInfo = new ArrayList<>();
                        steps = 1;
                    }
                }
            }

            Toast toast = Toast.makeText(getApplicationContext(), "Your Schedule Has Been Created.", Toast.LENGTH_LONG);
            toast.show();
            try {
                OutputStreamWriter streamWriter = new OutputStreamWriter(getApplicationContext().openFileOutput("done.txt", Context.MODE_PRIVATE));
                streamWriter.write("done");
                streamWriter.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            done();

        }
    }
}


