package app.zipternet.custom

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.webkit.*
import android.widget.*
import com.google.gson.Gson
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import org.jetbrains.anko.longToast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipFile

private var articleUrl: String? = null

class MainActivity : AppCompatActivity() {

    var zipProvider: ZipProvider? = null
    private var mWebView: WebView? = null
    private var mSearch: AutoCompleteTextView? = null
    private var mToolbar: Toolbar? = null
    private var mHomeButton: ImageButton? = null
    private var mCompleteAdapter: ArrayAdapter<String>? = null

    private var articlesByTitle: MutableMap<String, String> = mutableMapOf()


    private fun isPermissionGranted(permission:String):Boolean =  ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCenter.start(application, "e6a5cf41-408e-483f-bc73-17e9ff50721e", Analytics::class.java, Crashes::class.java)
        setContentView(R.layout.activity_main)

        mToolbar = findViewById(R.id.toolbar)
        mSearch = findViewById(R.id.search)
        mHomeButton = findViewById(R.id.homeButton)
        mWebView = findViewById(R.id.webView)

        mWebView!!.settings.javaScriptEnabled = true

        mWebView!!.loadData("<h1>Loading Contents</h1>", "text/html", "UTF-8")

        if (isPermissionGranted(READ_EXTERNAL_STORAGE) && isPermissionGranted(WRITE_EXTERNAL_STORAGE)) {
            loadContent()
        } else {
            showPermissionReasonAndRequest(
//                "Notice",
                "Hi, we will request the READ EXTERNAL STORAGE permission. \n" +
                        "This is required for showing you the content, \n" +
                        "please grant it.",
                arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE),
                1
            )
        }

    }

    private fun showPermissionReasonAndRequest(
//        title: String,
        message: String,
        permissions: Array<String>,
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
                        permissions,
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
        if(mWebView != null) {
            val canGoBack = mWebView!!.canGoBack()
            if (mWebView != null && canGoBack) {
                mWebView!!.goBack()
                return
            }
        }
        finish()
    }

    private fun guessMimeType(url: String): String? {
        val m = MimeTypeMap.getSingleton()
        val ext = MimeTypeMap.getFileExtensionFromUrl(url) ?: "html"
        return m.getMimeTypeFromExtension(ext)
    }

    private fun loadContent(){

        val files = obbDir.listFiles()
        Log.d("ZIPT", "Using obbDir [$obbDir]")

        if (files.isEmpty()) {
            longToast("Sorry, cannot continue until we have the ZIM file.")
            mWebView!!.loadData("<h1>Sorry: Cannot load contents</h1>", "text/html", "UTF-8")
            return
        }

        val versionCode = BuildConfig.VERSION_CODE

//        val filePath = obbDir.absolutePath + "/main." + versionCode + "." + packageName + ".obb"
        val filePath = files[0].absolutePath
        Log.d("ZIPT", "Reading file from: $filePath")


        if (!File(filePath).exists()) {
            Log.e("ZIPT", "Unable to find the ZIM file $filePath");
        }

        zipProvider = ZipProvider()
        zipProvider!!.setZipFile(File(filePath))

        var mainPage = "index.html"
        var articleListPath = "__articles"

        val configStr = zipProvider!!.getTextContent("config.json")
        if(configStr !== null) {
            val gson = Gson()
            val config = gson.fromJson(configStr, ConfigType::class.java)
            if (config.welcome != null) {
                mainPage = config.welcome!!
            }
            if (config.articleList != null) {
                articleListPath = config.articleList!!
            }
        }

        Log.d("ZIPT", "Home Page:\n$mainPage")

        mWebView!!.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                view!!.visibility = WebView.INVISIBLE;
            }

            override fun onPageFinished(view: WebView, url: String) {

//                spinner.setVisibility(View.GONE)

                view.visibility = WebView.VISIBLE
                mHomeButton!!.isEnabled = !url.endsWith(mainPage)

                super.onPageFinished(view, url)

            }

            override fun shouldOverrideUrlLoading(view: WebView?, _url: String): Boolean {
                val url = URLDecoder.decode(_url)
                Log.d("ZIPT", "URL: $url")
                if(url.startsWith(zipProvider!!.CONTENT_URI)) {
                    if(false && url.endsWith(".pdf")) {
                        val intent = Intent(Intent.ACTION_VIEW)

                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.setDataAndType(Uri.parse(url), "application/pdf")

                        startActivity(intent)
                        return true
                    } else {
                        return false
                    }
                } else {
                    val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(i)
                }

                return true
            }
        }

        val mainArticleUrl = "content://$packageName/$mainPage"
        if (articleUrl != null) {
            mWebView!!.loadUrl(articleUrl)
            Log.d("ZIPT", "Loading article URL: " + articleUrl)
        } else {
            mWebView!!.loadUrl(mainArticleUrl)
            Log.d("ZIPT", "Loading main page :(")
        }

        val articleListEntry = zipProvider!!.getEntry(articleListPath)
        if(articleListEntry != null) {
            val articleListInputStream = zipProvider!!.getInputStream(articleListEntry)
            val articleListTsv = BufferedReader(articleListInputStream!!.reader()).readText().trim()
            for (line in articleListTsv.split("\n")) {
                val (title, path) = line.split("\t")
                articlesByTitle[title] = path
            }

            mCompleteAdapter = ArrayAdapter(this, android.R.layout.select_dialog_item, articlesByTitle.keys.toList())
            mSearch!!.setAdapter(mCompleteAdapter)

            mSearch!!.setOnItemClickListener { parent, view, position, id ->
                val text = (view as TextView).text.toString()
                val articlePath = articlesByTitle[text]
                mWebView!!.loadUrl(zipProvider!!.CONTENT_URI + articlePath)
                mSearch!!.setText("")
                mSearch!!.clearFocus()
            }
        }


        mHomeButton!!.setOnClickListener { view -> mWebView!!.loadUrl(mainArticleUrl)}
    }
}
