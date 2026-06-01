package app.eob.me.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

fun <R> List<Flow<*>>.combineAll(transform: suspend (Array<Any?>) -> R): Flow<R> {
    @Suppress("UNCHECKED_CAST")
    return combine(map { flow -> flow as Flow<Any?> }) { values ->
        transform(values)
    }
}
