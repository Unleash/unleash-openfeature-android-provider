package io.getunleash.openfeature.android.example

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val greeting =
            TextView(this).apply {
                text = getString(R.string.hello_android)
                gravity = Gravity.CENTER
                textSize = 22f
            }

        setContentView(greeting)
    }
}
