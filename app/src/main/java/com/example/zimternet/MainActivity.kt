package app.zimternet.traveleurope

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixlib.JNIKiwixInt;
import org.kiwix.kiwixlib.JNIKiwixReader;
import org.kiwix.kiwixlib.JNIKiwixString;
import java.io.File
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.net.URLDecoder


class MainActivity : AppCompatActivity() {

    var currentJNIReader: JNIKiwixReader? = null
    var mWebView: WebView? = null


    private fun isPermissionGranted(permission:String):Boolean =  ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wv = findViewById<WebView>(R.id.webView)
        mWebView = wv;

//        mWebView!!.loadData("<h1>Loading</h1>", "text/html", "UTF-8")

        if (isPermissionGranted(READ_EXTERNAL_STORAGE)) {
            loadContent();
        } else {
            showPermissionReasonAndRequest(
//                "Notice",
                "Hi, we will request the READ EXTERNAL STORAGE permission. \n" +
                        "This is required for showing you the content, \n" +
                        "please grant it.",
                READ_EXTERNAL_STORAGE,
                1
            )
        }
    }

    private fun showPermissionReasonAndRequest(
//        title: String,
        message: String,
        permission: String,
        requestCode: Int
    ) {
        val dialogBuilder = AlertDialog.Builder(this)

        // set message of alert dialog
        dialogBuilder.setMessage(message)
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton("Sure", DialogInterface.OnClickListener {
                    dialog, id ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(permission),
                        requestCode
                    )
            })
            // negative button text and action
            .setNegativeButton("Nope", DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
            })


        val alert = dialogBuilder.create()

//        alert.setTitle(title)

        alert.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        thePermissions: Array<String>,
        theGrantResults: IntArray
    ) {
        // It's not our expect permission
        if (requestCode != 1) return
        if (isPermissionGranted(READ_EXTERNAL_STORAGE)) {
            // Success case: Get the permission
            // Do something and return
            loadContent()
            return
        }

//        if (isUserCheckNeverAskAgain()) {
//            // NeverAskAgain case - Never Ask Again has been checked
//            // Do something and return
//            return
//        }

        // Failure case: Not getting permission
        // Do something here
    }

    override fun onBackPressed() {
        if(mWebView!= null) {
            val canGoBack = mWebView!!.canGoBack()
            if (mWebView != null && canGoBack) {
                mWebView!!.goBack()
                return
            }
        }
        finish()
    }

    private fun loadContent(){
        JNIKiwix()

        val files = obbDir.listFiles()
        Log.d("ZIMT", "Files: $files")

        val versionCode = BuildConfig.VERSION_CODE

//        val filePath = obbDir.absolutePath + "/main." + versionCode + "." + packageName + ".obb"
        val filePath = files[0].absolutePath
        Log.d("ZIMT", "Reading file from: $filePath")


        if (!File(filePath).exists()) {
            Log.e("ZIMT", "Unable to find the ZIM file $filePath");
        }

        currentJNIReader = JNIKiwixReader(filePath);
        Log.d("ZIMT", "Read file")

        val mainPage = currentJNIReader!!.mainPage
        Log.d("ZIMT", "Home Page:\n$mainPage")


        mWebView!!.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                view!!.visibility = WebView.INVISIBLE;
            }

            override fun onPageFinished(view: WebView, url: String) {

//                spinner.setVisibility(View.GONE)

                view.visibility = WebView.VISIBLE
                super.onPageFinished(view, url)

            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest):WebResourceResponse {
                val url = request.url.toString()
                Log.d("ZIMT", "URL: $url")
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
                    )
                }

                return response
            }
        }

        mWebView!!.loadUrl("file://$mainPage");
    }
}
