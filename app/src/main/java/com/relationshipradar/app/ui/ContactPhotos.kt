package com.relationshipradar.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Loads contact photos off the main thread and keeps them in memory for the session. */
object ContactPhotos {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    suspend fun load(context: Context, lookupKey: String, preferHiRes: Boolean): ImageBitmap? {
        val key = "$lookupKey:$preferHiRes"
        cache[key]?.let { return it }
        if (missing.contains(key)) return null
        val bmp = withContext(Dispatchers.IO) {
            try {
                val lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
                val contactUri = ContactsContract.Contacts.lookupContact(context.contentResolver, lookupUri) ?: return@withContext null
                ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, contactUri, preferHiRes)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            } catch (_: Throwable) { null }
        }
        if (bmp != null) cache[key] = bmp else missing.add(key)
        return bmp
    }

    @Composable
    fun remember(lookupKey: String?, hiRes: Boolean = false): ImageBitmap? {
        val ctx = LocalContext.current
        val state by produceState<ImageBitmap?>(null, lookupKey, hiRes) {
            value = lookupKey?.let { load(ctx, it, hiRes) }
        }
        return state
    }
}

/** Bundled 3D avatars (Fluent Emoji, MIT). Index 1..24. */
object BundledAvatars {
    const val COUNT = 24
    fun resId(context: Context, n: Int): Int =
        context.resources.getIdentifier("avatar_%02d".format(n.coerceIn(1, COUNT)), "drawable", context.packageName)
    fun key(n: Int) = "avatar:$n"
    fun parse(avatar: String?): Int? = avatar?.removePrefix("avatar:")?.takeIf { avatar.startsWith("avatar:") }?.toIntOrNull()
}
