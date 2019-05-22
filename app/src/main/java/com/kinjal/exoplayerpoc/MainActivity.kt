package com.kinjal.exoplayerpoc

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    /**
     * This method handles play button click
     */
    fun play(view: View) {
        if (view.id == R.id.button) {

            val url = editText.text.toString()

            if (url.isNotEmpty()) {
                editText.error = null
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra(PlayerActivity.KEY_URL, url)
                startActivity(intent)
            } else {
                editText.error = "Please enter video url"
            }
        }
    }

}
