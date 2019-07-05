package com.example.zimternet

import android.Manifest
import android.annotation.TargetApi
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.TextView

import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixlib.JNIKiwixException;
import org.kiwix.kiwixlib.JNIKiwixInt;
import org.kiwix.kiwixlib.JNIKiwixReader;
import org.kiwix.kiwixlib.JNIKiwixSearcher;
import org.kiwix.kiwixlib.JNIKiwixString;
import java.io.File
import java.io.OutputStream
import java.security.Permission
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.net.URLDecoder


class MainActivity : AppCompatActivity() {

    var currentJNIReader: JNIKiwixReader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        JNIKiwix()

//        ActivityCompat.requestPermissions(this,
//            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//            1)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        val files = downloadsDir.listFiles()
        Log.d("ZIMT", "Files: "+files);
        val fileName = "/storage/emulated/0/Download/test.zim"
        Log.d("ZIMT", "Reading file")


        if (!File(fileName).exists()) {
            Log.e("ZIMT", "Unable to find the ZIM file " + fileName);
        }

        currentJNIReader = JNIKiwixReader(fileName);
        Log.d("ZIMT", "Read file")

        val mainPage = currentJNIReader!!.mainPage
        Log.d("ZIMT", "Home Page:\n" + mainPage)


        val title = JNIKiwixString()
        val mimeType = JNIKiwixString()
        val size = JNIKiwixInt()
        val content = currentJNIReader!!.getContent(mainPage, title, mimeType, size)

        val mt = mimeType.value

        val contentStr = String(content)

        val wv = findViewById<WebView>(R.id.webView)

        wv.webViewClient = object : WebViewClient() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest):WebResourceResponse {
                val url = request.url.toString()
                Log.d("ZIMT", "URL: " + url)
                var response = super.shouldInterceptRequest(view, request)
                if(url.startsWith("file://")) {
                    val title = JNIKiwixString()
                    val mimeType = JNIKiwixString()
                    val size = JNIKiwixInt()
                    var articleUrl = URLDecoder.decode(url.replace("file://", ""))
                    articleUrl = articleUrl.replace("a/", "A/")
                    var data = currentJNIReader!!.getContent(articleUrl, title, mimeType, size)
                    if(data.size == 0) {
                        articleUrl = articleUrl.replace("A/", "")
                        data = currentJNIReader!!.getContent(articleUrl, title, mimeType, size)
                    }

                    response = WebResourceResponse(
                        mimeType.value,
                        "utf-8",
                        ByteArrayInputStream(data)
                    );
                }

                return response;
            }
        }

        wv.loadUrl("file://" + mainPage);
    }
}
