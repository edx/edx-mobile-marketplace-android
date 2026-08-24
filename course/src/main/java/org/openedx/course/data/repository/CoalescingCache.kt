package org.openedx.course.data.repository

import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap


class CoalescingCache<K, V>(
    private val fetch: suspend (K) -> V,
    private val persist: (suspend (K, V) -> Unit)? = null
) {
    private val cache = ConcurrentHashMap<K, V>()
    private val pending = ConcurrentHashMap<K, CompletableDeferred<V>>()

    /**
     * Returns cached value for the key, or null if not cached.
     */
    fun getCached(key: K): V? = cache[key]

    /**
     * Manually sets a cached value.
     */
    fun setCached(key: K, value: V) {
        cache[key] = value
    }

    /**
     * Removes all cached values.
     */
    fun clear() {
        cache.clear()
    }


    suspend fun getOrFetch(key: K, forceRefresh: Boolean = false): V {
        if (!forceRefresh) {
            cache[key]?.let { return it }
        }

        val (deferred, isOwner) = getOrCreateDeferred(key)
        return if (isOwner) {
            try {
                val result = fetch(key)
                cache[key] = result
                persist?.invoke(key, result)
                deferred.complete(result)
                result
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
                throw e
            } finally {
                pending.remove(key)
            }
        } else {
            deferred.await()
        }
    }

    private fun getOrCreateDeferred(key: K): Pair<CompletableDeferred<V>, Boolean> {
        pending[key]?.let { return it to false }
        val deferred = CompletableDeferred<V>()
        val existing = pending.putIfAbsent(key, deferred)
        return if (existing != null) existing to false else deferred to true
    }
}
