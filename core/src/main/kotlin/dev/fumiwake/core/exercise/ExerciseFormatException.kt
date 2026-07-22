package dev.fumiwake.core.exercise

/**
 * Thrown when an exercise YAML document cannot be parsed / deserialized into an [Exercise].
 * Always carries the underlying cause's message so the operator can see what went wrong.
 */
class ExerciseFormatException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
