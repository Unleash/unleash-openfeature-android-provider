package io.getunleash.openfeature.android

import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.Hook
import dev.openfeature.sdk.ProviderEvaluation
import dev.openfeature.sdk.ProviderMetadata
import dev.openfeature.sdk.Reason
import dev.openfeature.sdk.Value
import dev.openfeature.sdk.exceptions.ErrorCode
import io.getunleash.android.Unleash
import io.getunleash.android.data.Payload
import io.getunleash.android.data.Variant
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class UnleashOpenFeatureProvider(
    private val client: Unleash,
    override val hooks: List<Hook<*>> = emptyList(),
) : FeatureProvider {
    override val metadata: ProviderMetadata = Metadata("unleash-openfeature-android-provider")

    override suspend fun initialize(initialContext: EvaluationContext?) {
        client.setContextAsync(initialContext.toUnleashContext())
    }

    override fun shutdown() {
        client.close()
    }

    override suspend fun onContextSet(
        oldContext: EvaluationContext?,
        newContext: EvaluationContext,
    ) {
        client.setContext(newContext.toUnleashContext())
    }

    override fun getBooleanEvaluation(
        key: String,
        defaultValue: Boolean,
        context: EvaluationContext?,
    ): ProviderEvaluation<Boolean> =
        ProviderEvaluation(
            value = client.isEnabled(key),
            reason = Reason.UNKNOWN.toString(),
        )

    override fun getStringEvaluation(
        key: String,
        defaultValue: String,
        context: EvaluationContext?,
    ): ProviderEvaluation<String> =
        resolvePayload(key, defaultValue, setOf("string", "csv")) { payload ->
            payload.value
        }

    override fun getIntegerEvaluation(
        key: String,
        defaultValue: Int,
        context: EvaluationContext?,
    ): ProviderEvaluation<Int> =
        resolvePayload(key, defaultValue, setOf("number")) { payload ->
            payload.value.toInt()
        }

    override fun getDoubleEvaluation(
        key: String,
        defaultValue: Double,
        context: EvaluationContext?,
    ): ProviderEvaluation<Double> =
        resolvePayload(key, defaultValue, setOf("number")) { payload ->
            payload.value.toDouble()
        }

    override fun getObjectEvaluation(
        key: String,
        defaultValue: Value,
        context: EvaluationContext?,
    ): ProviderEvaluation<Value> =
        resolvePayload(key, defaultValue, setOf("json")) { payload ->
            payload.value.toOpenFeatureValue()
        }

    private fun <T> resolvePayload(
        key: String,
        defaultValue: T,
        expectedPayloadTypes: Set<String>,
        convert: (Payload) -> T,
    ): ProviderEvaluation<T> {
        val variant = client.getVariant(key)
        val payload = variant.payload

        if (!variant.enabled || payload == null) {
            return ProviderEvaluation(
                value = defaultValue,
                variant = variant.name,
                reason = Reason.UNKNOWN.toString(),
            )
        }

        if (payload.type !in expectedPayloadTypes) {
            return ProviderEvaluation(
                value = defaultValue,
                variant = variant.name,
                reason = Reason.ERROR.toString(),
                errorCode = ErrorCode.TYPE_MISMATCH,
                errorMessage = "Variant payload type '${payload.type}' is not one of $expectedPayloadTypes",
            )
        }

        return try {
            ProviderEvaluation(
                value = convert(payload),
                variant = variant.name,
                reason = Reason.UNKNOWN.toString(),
            )
        } catch (error: NumberFormatException) {
            parseError(defaultValue, variant, error.message)
        } catch (error: IllegalArgumentException) {
            parseError(defaultValue, variant, error.message)
        }
    }

    private fun <T> parseError(
        defaultValue: T,
        variant: Variant,
        message: String?,
    ): ProviderEvaluation<T> =
        ProviderEvaluation(
            value = defaultValue,
            variant = variant.name,
            reason = Reason.ERROR.toString(),
            errorCode = ErrorCode.PARSE_ERROR,
            errorMessage = message,
        )

    private data class Metadata(
        override val name: String?,
    ) : ProviderMetadata
}

private fun String.toOpenFeatureValue(): Value =
    when (val value = JSONTokener(this).nextValue()) {
        is JSONArray -> value.toOpenFeatureValue()
        is JSONObject -> value.toOpenFeatureValue()
        JSONObject.NULL -> Value.Null
        is Boolean -> Value.Boolean(value)
        is Int -> Value.Integer(value)
        is Number -> Value.Double(value.toDouble())
        is String -> Value.String(value)
        else -> throw IllegalArgumentException("Unsupported JSON payload value: $value")
    }

private fun JSONArray.toOpenFeatureValue(): Value =
    Value.List(
        (0 until length()).map { index ->
            when (val value = get(index)) {
                is JSONArray -> value.toOpenFeatureValue()
                is JSONObject -> value.toOpenFeatureValue()
                JSONObject.NULL -> Value.Null
                is Boolean -> Value.Boolean(value)
                is Int -> Value.Integer(value)
                is Number -> Value.Double(value.toDouble())
                is String -> Value.String(value)
                else -> throw IllegalArgumentException("Unsupported JSON array value: $value")
            }
        },
    )

private fun JSONObject.toOpenFeatureValue(): Value {
    val structure = mutableMapOf<String, Value>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        structure[key] =
            when (val value = get(key)) {
                is JSONArray -> value.toOpenFeatureValue()
                is JSONObject -> value.toOpenFeatureValue()
                JSONObject.NULL -> Value.Null
                is Boolean -> Value.Boolean(value)
                is Int -> Value.Integer(value)
                is Number -> Value.Double(value.toDouble())
                is String -> Value.String(value)
                else -> throw IllegalArgumentException("Unsupported JSON object value: $value")
            }
    }
    return Value.Structure(structure)
}
