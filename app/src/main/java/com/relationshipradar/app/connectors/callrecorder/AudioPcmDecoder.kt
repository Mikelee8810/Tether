package com.relationshipradar.app.connectors.callrecorder

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.floor
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.withContext

data class DecodedPcm(
    val samples: ShortArray,
    val sampleRate: Int = TARGET_SAMPLE_RATE,
    val channelCount: Int = 1,
    /** Offset of the first sample in the full recording; 0 for a whole-recording decode. */
    val startOffsetMillis: Long = 0,
) {
    val durationMillis: Long = samples.size * 1_000L / sampleRate

    companion object {
        const val TARGET_SAMPLE_RATE = 16_000
    }
}

/**
 * Streaming decoder for Call Recorder's provider URIs.
 *
 * Audio is decoded through Android's platform codecs, downmixed to mono, and resampled to the
 * 16 kHz signed PCM expected by local speech models. The chunked entry points emit bounded
 * [DecodedPcm] windows so peak memory stays flat no matter how long the call is; the caller
 * consumes (e.g. transcribes) each window before the next one is produced.
 */
class AudioPcmDecoder(private val context: Context) {
    /** Whole-recording decode. Retained for short fixtures/probes; production code uses chunks. */
    fun decodeTo16kMono(uri: Uri): DecodedPcm {
        val chunks = ArrayList<DecodedPcm>()
        decodeTo16kMonoChunks(uri, chunkSamples = Int.MAX_VALUE) { chunks += it }
        return chunks.firstOrNull() ?: DecodedPcm(ShortArray(0))
    }

    /**
     * Decodes [uri] in bounded chunks of at most [chunkSamples] 16 kHz mono samples, invoking
     * [onChunk] for each one in order. Returns the total decoded duration in milliseconds.
     */
    fun decodeTo16kMonoChunks(
        uri: Uri,
        chunkSamples: Int = DEFAULT_CHUNK_SAMPLES,
        onChunk: (DecodedPcm) -> Unit,
    ): Long {
        val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
            ?: error("Unable to open recording")
        return descriptor.use {
            decodeChunks(it.fileDescriptor, it.startOffset, it.length, chunkSamples, onChunk)
        }
    }

    /**
     * The chunked decode as a cold flow: decoding runs on the IO dispatcher and each bounded
     * chunk is delivered with backpressure, so a slow consumer (transcription) never lets
     * decoded audio pile up in memory.
     */
    fun decodeTo16kMonoChunkFlow(
        uri: Uri,
        chunkSamples: Int = DEFAULT_CHUNK_SAMPLES,
    ): kotlinx.coroutines.flow.Flow<DecodedPcm> = kotlinx.coroutines.flow.channelFlow {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            decodeTo16kMonoChunks(uri, chunkSamples) { chunk ->
                trySendBlocking(chunk).getOrThrow()
            }
        }
    }

    /** Whole-recording decode from a raw descriptor. Retained for short fixtures/probes. */
    fun decode(
        fileDescriptor: java.io.FileDescriptor,
        startOffset: Long,
        length: Long,
    ): DecodedPcm {
        val chunks = ArrayList<DecodedPcm>()
        decodeChunks(fileDescriptor, startOffset, length, Int.MAX_VALUE) { chunks += it }
        return chunks.firstOrNull() ?: DecodedPcm(ShortArray(0))
    }

    fun decodeChunks(
        fileDescriptor: java.io.FileDescriptor,
        startOffset: Long,
        length: Long,
        chunkSamples: Int = DEFAULT_CHUNK_SAMPLES,
        onChunk: (DecodedPcm) -> Unit,
    ): Long {
        require(chunkSamples > 0) { "chunkSamples must be positive" }
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            if (length >= 0) extractor.setDataSource(fileDescriptor, startOffset, length)
            else extractor.setDataSource(fileDescriptor)

            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("Recording has no audio track")
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mimeType = requireNotNull(inputFormat.getString(MediaFormat.KEY_MIME))
            extractor.selectTrack(trackIndex)

            codec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(inputFormat, null, null, 0)
                start()
            }

            val sink = ChunkSink(chunkSamples, onChunk)
            var resampler: StreamingResampler? = null
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            while (!outputEnded) {
                if (!inputEnded) {
                    val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = requireNotNull(codec.getInputBuffer(inputIndex))
                        val size = extractor.readSampleData(inputBuffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        pcmEncoding = outputFormat.getIntegerOrDefault(
                            MediaFormat.KEY_PCM_ENCODING,
                            AudioFormat.ENCODING_PCM_16BIT,
                        )
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (outputIndex >= 0) {
                        codec.getOutputBuffer(outputIndex)?.let { buffer ->
                            buffer.order(ByteOrder.nativeOrder())
                            buffer.position(bufferInfo.offset)
                            buffer.limit(bufferInfo.offset + bufferInfo.size)
                            val interleaved = readInterleaved(buffer, pcmEncoding)
                            val mono = downmix(interleaved, channelCount)
                            val stream = resampler ?: StreamingResampler(
                                sampleRate,
                                DecodedPcm.TARGET_SAMPLE_RATE,
                            ).also { resampler = it }
                            sink.add(stream.append(mono))
                        }
                        outputEnded = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            resampler?.let { sink.add(it.flush()) }
            return sink.finish()
        } finally {
            try {
                codec?.stop()
            } finally {
                codec?.release()
                extractor.release()
            }
        }
    }

    private fun readInterleaved(buffer: java.nio.ByteBuffer, pcmEncoding: Int): ShortArray =
        when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val count = buffer.remaining() / Float.SIZE_BYTES
                ShortArray(count) {
                    (buffer.float.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
            }
            else -> {
                val count = buffer.remaining() / Short.SIZE_BYTES
                ShortArray(count) { buffer.short }
            }
        }

    private fun downmix(samples: ShortArray, channels: Int): ShortArray {
        if (channels <= 1) return samples
        val frames = samples.size / channels
        return ShortArray(frames) { frame ->
            var sum = 0L
            repeat(channels) { channel -> sum += samples[frame * channels + channel] }
            (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, default: Int): Int =
        if (containsKey(key)) getInteger(key) else default

    /** Accumulates resampled samples and emits fixed-size [DecodedPcm] windows. */
    private class ChunkSink(
        private val chunkSamples: Int,
        private val onChunk: (DecodedPcm) -> Unit,
    ) {
        private var buffer = ShortArray(minOf(chunkSamples, INITIAL_BUFFER_SAMPLES))
        private var count = 0
        private var emittedSamples = 0L
        private var totalSamples = 0L

        fun add(samples: ShortArray) {
            var offset = 0
            while (offset < samples.size) {
                val room = chunkSamples - count
                val take = minOf(room, samples.size - offset)
                ensureCapacity(count + take)
                samples.copyInto(buffer, count, offset, offset + take)
                count += take
                totalSamples += take
                offset += take
                if (count == chunkSamples) emit()
            }
        }

        /** Emits any trailing partial chunk and returns the total duration in milliseconds. */
        fun finish(): Long {
            if (count > 0) emit()
            return totalSamples * 1_000L / DecodedPcm.TARGET_SAMPLE_RATE
        }

        private fun emit() {
            val chunk = buffer.copyOf(count)
            onChunk(
                DecodedPcm(
                    samples = chunk,
                    startOffsetMillis = emittedSamples * 1_000L / DecodedPcm.TARGET_SAMPLE_RATE,
                ),
            )
            emittedSamples += count
            count = 0
        }

        private fun ensureCapacity(needed: Int) {
            if (needed <= buffer.size) return
            var size = buffer.size
            while (size < needed && size < chunkSamples) size = minOf(chunkSamples, size * 2)
            buffer = buffer.copyOf(size)
        }

        private companion object {
            const val INITIAL_BUFFER_SAMPLES = 16_000
        }
    }

    /**
     * Linear-interpolation resampler that carries its state across buffers, so chunk
     * boundaries never restart the sample phase. Holds at most one codec buffer of
     * not-yet-consumed source samples plus the current output window.
     */
    private class StreamingResampler(
        private val sourceRate: Int,
        private val targetRate: Int,
    ) {
        private val ratio = if (targetRate == sourceRate) 1.0 else sourceRate.toDouble() / targetRate.toDouble()
        private val pending = ArrayDeque<Short>()
        private var pendingBaseIndex = 0L
        private var totalSourceSamples = 0L
        private var nextOutputIndex = 0L

        fun append(mono: ShortArray): ShortArray {
            if (ratio == 1.0) return mono
            mono.forEach { pending.addLast(it) }
            totalSourceSamples += mono.size
            // An output sample is final once both interpolation neighbours have arrived.
            return drain(availableOutputs(final = false))
        }

        fun flush(): ShortArray {
            if (ratio == 1.0) return ShortArray(0)
            return drain(availableOutputs(final = true)).also { pending.clear() }
        }

        private fun availableOutputs(final: Boolean): Int {
            val totalOutputs = (totalSourceSamples * targetRate.toDouble() / sourceRate).toInt()
            var count = totalOutputs - nextOutputIndex.toInt()
            if (!final && count > 0) {
                // Hold back outputs whose upper neighbour has not been decoded yet.
                val lastSafeOutput = ((totalSourceSamples - 1) / ratio).toInt()
                count = minOf(count, (lastSafeOutput - nextOutputIndex + 1).toInt().coerceAtLeast(0))
            }
            return count.coerceAtLeast(0)
        }

        private fun drain(count: Int): ShortArray {
            if (count == 0) return ShortArray(0)
            val out = ShortArray(count) { i ->
                val outputIndex = nextOutputIndex + i
                val sourcePosition = outputIndex * ratio
                val lowerIndex = floor(sourcePosition).toLong()
                val fraction = sourcePosition - lowerIndex
                val lower = sampleAt(lowerIndex)
                val upper = sampleAt(minOf(lowerIndex + 1, totalSourceSamples - 1))
                (lower + (upper - lower) * fraction)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            nextOutputIndex += count
            // Drop source samples no future output can reference, keeping the boundary sample.
            val keepFrom = floor(nextOutputIndex * ratio).toLong()
            while (pendingBaseIndex < keepFrom && pending.size > 1) {
                pending.removeFirst()
                pendingBaseIndex++
            }
            return out
        }

        private fun sampleAt(sourceIndex: Long): Short =
            pending[(sourceIndex - pendingBaseIndex).toInt()]
    }

    companion object {
        /** 20 seconds of 16 kHz mono audio per chunk: big enough for Whisper, small in memory. */
        const val DEFAULT_CHUNK_SAMPLES = DecodedPcm.TARGET_SAMPLE_RATE * 20
        const val TIMEOUT_US = 10_000L
    }
}
