package io.getunleash.openfeature.android

import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.Value
import io.getunleash.android.data.UnleashContext

internal fun EvaluationContext?.toUnleashContext(): UnleashContext {
    if (this == null) {
        return UnleashContext()
    }

    val builder = UnleashContext.newBuilder()
    val properties = mutableMapOf<String, String>()

    for ((key, value) in asMap()) {
        val stringValue = value.asUnleashString() ?: continue
        when (key) {
            "userId" -> builder.userId(stringValue)
            "sessionId" -> builder.sessionId(stringValue)
            "remoteAddress" -> builder.remoteAddress(stringValue)
            else -> properties[key] = stringValue
        }
    }

    val targetingKey = getTargetingKey()
    if (targetingKey.isNotEmpty()) {
        builder.userId(targetingKey)
    }

    return builder.properties(properties).build()
}

private fun Value.asUnleashString(): String? =
    when (this) {
        is Value.Boolean -> boolean.toString()
        is Value.Date -> date.toString()
        is Value.Double -> double.toString()
        is Value.Integer -> integer.toString()
        is Value.String -> string
        is Value.List,
        Value.Null,
        is Value.Structure -> null
    }
