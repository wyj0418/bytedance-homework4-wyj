package com.wyj.network

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import okhttp3.*
import com.google.gson.GsonBuilder
import okhttp3.EventListener
import okhttp3.Response
import java.io.IOException

class MainActivity : AppCompatActivity() {
    var requestBtn: Button? = null
    var output: TextView? = null
    var input: EditText? = null


    //使用OKHttp的Intercetpor
    //参考课程pdf
    class TimeConsumeInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val startTime = System.currentTimeMillis()
            val resp = chain.proceed(chain.request())
            val endTime = System.currentTimeMillis()
            val url = chain.request().url.toString()
            Log.e("LogInterceptor", "request:$url cost time ${endTime - startTime}")
            return resp
        }
    }

    //使用OKHttp的EventListener
    //handler
    val okhttpListener = object : EventListener() {
        override fun dnsStart(call: Call, domainName: String) {
            super.dnsStart(call, domainName)
            handler.post {
                output?.text = output?.text.toString() + "\nDns Search:" + domainName
            }
        }

        override fun responseBodyStart(call: Call) {
            super.responseBodyStart(call)
            handler.post {
                output?.text = output?.text.toString() + "\nReaponse Start"
            }
        }
    }

    //val client: OkHttpClient = OkHttpClient.Builder().eventListener(okhttpLister).builder()
    val client: OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(TimeConsumeInterceptor())
        .eventListener(okhttpListener).build()


    //引入OKHttp
    //参考demo
    fun request(url: String, callback: Callback) {
        val request: Request = Request.Builder()
            .url(url)
            .header("User-Agent", "Sjtu-Android-OKHttp")
            .build()
        client.newCall(request).enqueue(callback)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestBtn = findViewById(R.id.send_request)
        output = findViewById(R.id.translate_output)
        input = findViewById(R.id.translate_input)
        input?.setOnClickListener(View.OnClickListener { v ->
            if (v.id == input?.getId()) {
                input?.setCursorVisible(true)
            }
        })
        input?.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            input?.setCursorVisible(false)
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(
                    input?.applicationWindowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
            false
        }
        )
        requestBtn?.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
            translate()
        }
    }

    private val handler = Handler()

    //解析返回值
    //参考pdf
    val gson = GsonBuilder().create()
    fun translate() {
        var res = ""
        val input: String = input?.text.toString()
        if (input == "") {
            handler.post {
                output?.text = ""
            }
        }
        val url = "https://dict.youdao.com/jsonapi?q=" + input
        request(url, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                output?.text = e.message
            }

            override fun onResponse(call: Call, response: Response) {

                val bodyString = response.body?.string()
                val youdaoBean = gson.fromJson(bodyString, youdao::class.java)
                var i = 0
                while (i < youdaoBean.web_trans?.web_translation?.get(0)?.trans?.size!!.toInt()) {
                    res += (i + 1).toString() + "."
                    res += youdaoBean.web_trans?.web_translation?.get(0)?.trans?.get(i)?.value.toString() + "\n"
                    i++
                }
                handler.post {
                    output?.text = res
                }
            }
        })

    }
}

