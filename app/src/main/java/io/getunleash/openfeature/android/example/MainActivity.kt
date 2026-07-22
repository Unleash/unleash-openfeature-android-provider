package io.getunleash.openfeature.android.example

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import dev.openfeature.sdk.OpenFeatureAPI
import io.getunleash.android.UnleashConfig
import io.getunleash.openfeature.android.UnleashOpenFeatureProvider
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : Activity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val greeting =
            TextView(this).apply {
                text = getString(R.string.loading_flag)
                gravity = Gravity.CENTER
                textSize = 22f
            }

        setContentView(greeting)

        scope.launch {
            runCatching {
                val config =
                    UnleashConfig
                        .newBuilder("openfeature-android-example")
                        .proxyUrl("https://app.unleash-hosted.com/demo/api/frontend")
                        .clientKey("replace-with-your-client-key")
                        .build()

                val provider =
                    UnleashOpenFeatureProvider(
                        androidContext = applicationContext,
                        unleashConfig = config,
                    )

                OpenFeatureAPI.setProviderAndWait(provider)

                val client = OpenFeatureAPI.getClient("android-example")
                client.getBooleanValue("example-flag", false)
            }.onSuccess { enabled ->
                greeting.text = getString(R.string.flag_status, enabled.toString())
            }.onFailure { error ->
                greeting.text = getString(R.string.flag_error, error.message ?: error::class.java.simpleName)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
