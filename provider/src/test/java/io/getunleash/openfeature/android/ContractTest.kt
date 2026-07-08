package io.getunleash.openfeature.android

import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.ProviderEvaluation
import dev.openfeature.sdk.Value
import io.getunleash.android.Unleash
import io.getunleash.android.data.Payload
import io.getunleash.android.data.Toggle
import io.getunleash.android.data.UnleashContext
import io.getunleash.android.data.Variant
import io.getunleash.android.events.UnleashListener
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ContractTest {
    @Test
    fun verifierContract() {
        val specRoot = File("../verifier/spec")
        val contract = JSONObject(File(specRoot, "contract.json").readText())
        val toggles = loadFrontendToggles(File(specRoot, "fixtures/frontend-toggles.json"))
        val provider = UnleashOpenFeatureProvider(FrontendFixtureUnleash(toggles))

        for (scenario in contract.getJSONArray("scenarios").jsonObjects()) {
            if (!appliesToAndroid(scenario)) {
                continue
            }

            val details = evaluate(provider, scenario)
            val expected = scenario.getJSONObject("expect")

            assertJsonEquals(
                scenario.getString("id"),
                expected.get("value"),
                details.value.toJsonCompatible()
            )
            if (expected.has("variant")) {
                assertEquals(
                    scenario.getString("id"),
                    expected.getString("variant"),
                    details.variant
                )
            }
        }
    }

    private fun evaluate(
        provider: UnleashOpenFeatureProvider,
        scenario: JSONObject
    ): ProviderEvaluation<*> {
        val flagKey = scenario.getString("flagKey")
        val context = scenario.optJSONObject("context")?.toEvaluationContext()

        return when (scenario.getString("type")) {
            "boolean" -> provider.getBooleanEvaluation(
                flagKey,
                scenario.getBoolean("default"),
                context
            )
            "string" -> provider.getStringEvaluation(
                flagKey,
                scenario.getString("default"),
                context
            )
            "number" -> provider.getDoubleEvaluation(
                flagKey,
                scenario.getDouble("default"),
                context
            )
            "object" -> provider.getObjectEvaluation(
                flagKey,
                scenario.get("default").toOpenFeatureValue(),
                context
            )
            else -> error("Unsupported contract scenario type: ${scenario.getString("type")}")
        }
    }
}

private fun appliesToAndroid(scenario: JSONObject): Boolean {
    val requires = scenario.optJSONArray("requires") ?: return true
    return (0 until requires.length()).none { index ->
        requires.getString(index) in setOf("localEval", "perCallContext")
    }
}

private fun loadFrontendToggles(file: File): Map<String, FixtureToggle> {
    val toggles = JSONObject(file.readText()).getJSONArray("toggles")
    return toggles.jsonObjects().associate { toggle ->
        val name = toggle.getString("name")
        name to FixtureToggle(
            enabled = toggle.getBoolean("enabled"),
            variant = toggle.getJSONObject("variant").toVariant()
        )
    }
}

private fun JSONObject.toVariant(): Variant =
    Variant(
        name = getString("name"),
        enabled = getBoolean("enabled"),
        featureEnabled = getBoolean("feature_enabled"),
        payload = optJSONObject("payload")?.let { payload ->
            Payload(
                type = payload.getString("type"),
                value = payload.getString("value")
            )
        }
    )

private fun JSONObject.toEvaluationContext(): ImmutableContext {
    val targetingKey = optString("targetingKey", "")
    val attributes = mutableMapOf<String, Value>()
    for (key in keys()) {
        if (key != "targetingKey") {
            attributes[key] = get(key).toOpenFeatureValue()
        }
    }
    return ImmutableContext(targetingKey = targetingKey, attributes = attributes)
}

private fun Any?.toOpenFeatureValue(): Value =
    when (this) {
        null,
        JSONObject.NULL -> Value.Null
        is Boolean -> Value.Boolean(this)
        is Int -> Value.Integer(this)
        is Number -> Value.Double(toDouble())
        is String -> Value.String(this)
        is JSONArray -> Value.List(values().map { it.toOpenFeatureValue() })
        is JSONObject -> Value.Structure(
            stringKeys().associateWith { key -> get(key).toOpenFeatureValue() }
        )
        else -> error("Unsupported JSON value: $this")
    }

private fun Any?.toJsonCompatible(): Any? =
    when (this) {
        is Value.Boolean -> boolean
        is Value.Date -> date.toString()
        is Value.Double -> double
        is Value.Integer -> integer
        is Value.List -> list.map { it.toJsonCompatible() }
        Value.Null -> null
        is Value.String -> string
        is Value.Structure -> structure.mapValues { (_, value) -> value.toJsonCompatible() }
        else -> this
    }

private fun JSONArray.values(): List<Any?> =
    (0 until length()).map { index -> get(index) }

private fun JSONArray.jsonObjects(): List<JSONObject> =
    (0 until length()).map { index -> getJSONObject(index) }

private fun JSONObject.stringKeys(): List<String> {
    val result = mutableListOf<String>()
    val keys = keys()
    while (keys.hasNext()) {
        result += keys.next()
    }
    return result
}

private fun assertJsonEquals(scenarioId: String, expected: Any?, actual: Any?) {
    when {
        expected is JSONObject && actual is Map<*, *> -> {
            assertEquals("$scenarioId object keys", expected.stringKeys().toSet(), actual.keys)
            for (key in expected.stringKeys()) {
                assertJsonEquals(scenarioId, expected.get(key), actual[key])
            }
        }
        expected is JSONArray && actual is List<*> -> {
            assertEquals("$scenarioId array length", expected.length(), actual.size)
            for (index in 0 until expected.length()) {
                assertJsonEquals(scenarioId, expected.get(index), actual[index])
            }
        }
        expected is Number && actual is Number -> {
            assertEquals(scenarioId, expected.toDouble(), actual.toDouble(), 0.000001)
        }
        expected == JSONObject.NULL -> {
            assertEquals(scenarioId, null, actual)
        }
        else -> {
            assertEquals(scenarioId, expected, actual)
        }
    }
}

private data class FixtureToggle(
    val enabled: Boolean,
    val variant: Variant
)

private class FrontendFixtureUnleash(
    private val toggles: Map<String, FixtureToggle>
) : Unleash {
    @Deprecated(
        "Use isEnabled(toggleName: String) instead. See https://github.com/Unleash/unleash-android-sdk/issues/141",
        replaceWith = ReplaceWith("isEnabled(toggleName)")
    )
    override fun isEnabled(toggleName: String, defaultValue: Boolean): Boolean =
        toggles[toggleName]?.enabled ?: defaultValue

    override fun isEnabled(toggleName: String): Boolean =
        toggles[toggleName]?.enabled ?: false

    @Deprecated(
        "Use getVariant(toggleName: String) instead. See https://github.com/Unleash/unleash-android-sdk/issues/141",
        replaceWith = ReplaceWith("getVariant(toggleName)")
    )
    override fun getVariant(toggleName: String, defaultValue: Variant): Variant =
        if (toggles[toggleName]?.enabled == true) {
            toggles.getValue(toggleName).variant
        } else {
            defaultValue
        }

    override fun getVariant(toggleName: String): Variant =
        if (toggles[toggleName]?.enabled == true) {
            toggles.getValue(toggleName).variant
        } else {
            Variant("disabled")
        }

    override fun setContext(context: UnleashContext) {
    }

    override fun setContextWithTimeout(context: UnleashContext, timeout: Long) {
    }

    override fun setContextAsync(context: UnleashContext) {
    }

    override fun addUnleashEventListener(listener: UnleashListener) {
    }

    override fun removeUnleashEventListener(listener: UnleashListener) {
    }

    override fun refreshTogglesNow() {
    }

    override fun refreshTogglesNowAsync() {
    }

    override fun sendMetricsNow() {
    }

    override fun sendMetricsNowAsync() {
    }

    override fun isReady(): Boolean = true

    override fun start(
        eventListeners: List<UnleashListener>,
        bootstrapFile: File?,
        bootstrap: List<Toggle>
    ) {
    }

    override fun close() {
    }
}
