package io.getunleash.openfeature.android

import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.ProviderEvaluation
import dev.openfeature.sdk.Value
import dev.openfeature.sdk.exceptions.ErrorCode
import io.getunleash.android.Unleash
import io.getunleash.android.data.Payload
import io.getunleash.android.data.Toggle
import io.getunleash.android.data.UnleashContext
import io.getunleash.android.data.Variant
import io.getunleash.android.events.UnleashListener
import io.getunleash.android.events.UnleashReadyListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class UnleashOpenFeatureProviderTest {
    @Test
    fun initializeSetsTheInitialContextAsync() {
        val client = FakeUnleash()
        val provider = UnleashOpenFeatureProvider(clientFactory = { client })
        val context =
            ImmutableContext(
                targetingKey = "target-user",
                attributes =
                    mapOf(
                        "userId" to Value.String("attribute-user"),
                        "sessionId" to Value.Integer(42),
                        "remoteAddress" to Value.String("127.0.0.1"),
                        "plan" to Value.String("pro"),
                        "nested" to Value.Structure(mapOf("ignored" to Value.String("value"))),
                    ),
            )

        runSuspend { provider.initialize(context) }

        assertEquals(
            UnleashContext(
                userId = "target-user",
                sessionId = "42",
                remoteAddress = "127.0.0.1",
                properties = mapOf("plan" to "pro"),
            ),
            client.asyncContexts.single(),
        )
    }

    @Test
    fun initializeStartsUnleashAfterSettingTheContext() {
        val client = FakeUnleash()
        val provider = UnleashOpenFeatureProvider(clientFactory = { client })

        runSuspend { provider.initialize(null) }

        assertEquals(listOf("setContextAsync", "start"), client.calls)
    }

    @Test
    fun initializeWaitsForTheFirstFetch() {
        val client = FakeUnleash(readiness = Readiness.ON_LISTENER)
        val provider = UnleashOpenFeatureProvider(clientFactory = { client })

        runSuspend { provider.initialize(null) }

        assertTrue(client.isReady())
        assertTrue("ready listener was leaked", client.listeners.isEmpty())
    }

    @Test
    fun initializeGivesUpWaitingWhenNoTogglesArrive() {
        val client = FakeUnleash(readiness = Readiness.NEVER)
        val provider =
            UnleashOpenFeatureProvider(
                clientFactory = { client },
                initializationTimeout = 50.milliseconds,
            )

        runSuspend { provider.initialize(null) }

        assertFalse(client.isReady())
        assertTrue("ready listener was leaked", client.listeners.isEmpty())
    }

    @Test
    fun initializeResumesWhenReadinessArrivesWhileWaiting() {
        val client = FakeUnleash(readiness = Readiness.NEVER)
        val provider = UnleashOpenFeatureProvider(clientFactory = { client })

        runBlocking {
            withTimeout(2_000) {
                val initializing = launch { provider.initialize(null) }
                while (client.listeners.isEmpty()) {
                    yield()
                }
                client.becomeReady()
                initializing.join()
            }
        }

        assertTrue(client.isReady())
        assertTrue("ready listener was leaked", client.listeners.isEmpty())
    }

    @Test
    fun onContextSetRefreshesUnleashContextSynchronously() {
        val client = FakeUnleash()
        val provider = UnleashOpenFeatureProvider(clientFactory = { client })

        runSuspend {
            provider.onContextSet(
                oldContext = null,
                newContext =
                    ImmutableContext(
                        attributes = mapOf("userId" to Value.String("7")),
                    ),
            )
        }

        assertEquals("7", client.contexts.single().userId)
    }

    @Test
    fun stringEvaluationResolvesCsvPayloadAsRawString() {
        val provider =
            UnleashOpenFeatureProvider(
                clientFactory = {
                    FakeUnleash(
                        variants =
                            mapOf(
                                "flag" to
                                    Variant(
                                        name = "variant",
                                        enabled = true,
                                        payload = Payload("csv", "a,b,c"),
                                    ),
                            ),
                    )
                },
            )

        val details = provider.getStringEvaluation("flag", "default", null)

        assertEquals("a,b,c", details.value)
        assertEquals("variant", details.variant)
    }

    @Test
    fun objectEvaluationResolvesJsonArrayPayload() {
        val provider =
            UnleashOpenFeatureProvider(
                clientFactory = {
                    FakeUnleash(
                        variants =
                            mapOf(
                                "flag" to
                                    Variant(
                                        name = "variant",
                                        enabled = true,
                                        payload = Payload("json", "[1,2,3]"),
                                    ),
                            ),
                    )
                },
            )

        val details = provider.getObjectEvaluation("flag", Value.Null, null)

        assertEquals(
            Value.List(listOf(Value.Integer(1), Value.Integer(2), Value.Integer(3))),
            details.value,
        )
    }

    @Test
    fun emptyNumberPayloadReturnsParseError() {
        val provider =
            UnleashOpenFeatureProvider(
                clientFactory = {
                    FakeUnleash(
                        variants =
                            mapOf(
                                "flag" to
                                    Variant(
                                        name = "variant",
                                        enabled = true,
                                        payload = Payload("number", ""),
                                    ),
                            ),
                    )
                },
            )

        val details: ProviderEvaluation<Int> = provider.getIntegerEvaluation("flag", 5, null)

        assertEquals(5, details.value)
        assertEquals(ErrorCode.PARSE_ERROR, details.errorCode)
    }

    @Test
    fun disabledVariantReturnsDefaultValue() {
        val provider =
            UnleashOpenFeatureProvider(
                clientFactory = {
                    FakeUnleash(
                        variants =
                            mapOf(
                                "flag" to
                                    Variant(
                                        name = "disabled",
                                        enabled = false,
                                        payload = Payload("string", "live"),
                                    ),
                            ),
                    )
                },
            )

        val details = provider.getStringEvaluation("flag", "default", null)

        assertEquals("default", details.value)
        assertEquals("disabled", details.variant)
    }
}

private fun runSuspend(block: suspend () -> Unit) {
    runBlocking {
        block()
    }
}

private enum class Readiness {
    ON_START,
    ON_LISTENER,
    NEVER,
}

private class FakeUnleash(
    private val variants: Map<String, Variant> = emptyMap(),
    private val readiness: Readiness = Readiness.ON_START,
) : Unleash {
    val contexts = mutableListOf<UnleashContext>()
    val asyncContexts = mutableListOf<UnleashContext>()
    val listeners = mutableListOf<UnleashListener>()
    val calls = mutableListOf<String>()
    private var ready = false

    @Deprecated(
        "Use isEnabled(toggleName: String) instead. See https://github.com/Unleash/unleash-android-sdk/issues/141",
        replaceWith = ReplaceWith("isEnabled(toggleName)"),
    )
    override fun isEnabled(
        toggleName: String,
        defaultValue: Boolean,
    ): Boolean = defaultValue

    override fun isEnabled(toggleName: String): Boolean = false

    @Deprecated(
        "Use getVariant(toggleName: String) instead. See https://github.com/Unleash/unleash-android-sdk/issues/141",
        replaceWith = ReplaceWith("getVariant(toggleName)"),
    )
    override fun getVariant(
        toggleName: String,
        defaultValue: Variant,
    ): Variant = variants[toggleName] ?: defaultValue

    override fun getVariant(toggleName: String): Variant = variants[toggleName] ?: Variant("disabled")

    override fun setContext(context: UnleashContext) {
        contexts += context
    }

    override fun setContextWithTimeout(
        context: UnleashContext,
        timeout: Long,
    ) {
        contexts += context
    }

    override fun setContextAsync(context: UnleashContext) {
        asyncContexts += context
        calls += "setContextAsync"
    }

    override fun addUnleashEventListener(listener: UnleashListener) {
        listeners += listener
        if (readiness == Readiness.ON_LISTENER) {
            becomeReady()
        }
    }

    override fun removeUnleashEventListener(listener: UnleashListener) {
        listeners -= listener
    }

    override fun refreshTogglesNow() {
    }

    override fun refreshTogglesNowAsync() {
    }

    override fun sendMetricsNow() {
    }

    override fun sendMetricsNowAsync() {
    }

    override fun isReady(): Boolean = ready

    override fun start(
        eventListeners: List<UnleashListener>,
        bootstrapFile: File?,
        bootstrap: List<Toggle>,
    ) {
        calls += "start"
        if (readiness == Readiness.ON_START) {
            becomeReady()
        }
    }

    fun becomeReady() {
        ready = true
        listeners.filterIsInstance<UnleashReadyListener>().forEach { it.onReady() }
    }

    override fun close() {
    }
}
