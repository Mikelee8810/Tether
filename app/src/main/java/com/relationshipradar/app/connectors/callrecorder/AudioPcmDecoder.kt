package com.relationshipradar.app.connectors.callrecorder

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.floor

data class DecodedPcm(
    val samples: ShortArray,
    val sampleRate: Int = TARGET_SAMPLE_RATE,
    val channelCount: Int = 1,
) {
    val durationMillis: Long = samples.size * 1_000L / sampleRate

    companion object {
        const val TARGET_SAMPLE_RATE = 16_000
    }
}

/**
 * Phase-5 prototype decoder. It accepts Call Recorder's provider URI, decodes through Android's
 * platform codecs, downmixes to mono, and resamples to the 16 kHz signed PCM expected by local
 * speech models. The next phase should stream bounded chunks rather than retain a whole call.
 */
class AudioPcmDecoder(private val context: Context) {
    fun decodeTo16kMono(uri: Uri): DecodedPcm {
        val descriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
            ?: error("Unable to open recording")
        return descriptor.use {
            decode(it.fileDescriptor, it.startOffset, it.length)
        }
    }

    fun decode(
        fileDescriptor: java.io.FileDescriptor,
        startOffset: Long,
        length: Long,
    ): DecodedPcm {
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

            val interleaved = ArrayList<Short>()
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
                            when (pcmEncoding) {
                                AudioFormat.ENCODING_PCM_FLOAT -> while (buffer.remaining() >= Float.SIZE_BYTES) {
                                    val normalized = buffer.float.coerceIn(-1f, 1f)
                                    interleaved += (normalized * Short.MAX_VALUE).toInt().toShort()
                                }
                                else -> while (buffer.remaining() >= Short.SIZE_BYTES) {
                                    interleaved += buffer.short
                                }
                            }
                        }
                        outputEnded = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            val mono = downmix(interleaved, channelCount)
            return DecodedPcm(resample(mono, sampleRate, DecodedPcm.TARGET_SAMPLE_RATE))
        } finally {
            try {
                codec?.stop()
            } finally {
                codec?.release()
                extractor.release()
            }
        }
    }

    private fun downmix(samples: List<Short>, channels: Int): ShortArray {
        if (channels <= 1) return samples.toShortArray()
        val frames = samples.size / channels
        return ShortArray(frames) { frame ->
            var sum = 0L
            repeat(channels) { channel -> sum += samples[frame * channels + channel] }
            (sum / channels).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun resample(source: ShortArray, sourceRate: Int, targetRate: Int): ShortArray {
        if (source.isEmpty() || sourceRate == targetRate) return source
        val targetSize = (source.size.toLong() * targetRate / sourceRate).toInt().coerceAtLeast(1)
        val ratio = sourceRate.toDouble() / targetRate.toDouble()
        return ShortArray(targetSize) { outputIndex ->
            val sourcePosition = outputIndex * ratio
            val lower = floor(sourcePosition).toInt().coerceIn(0, source.lastIndex)
            val upper = (lower + 1).coerceAtMost(source.lastIndex)
            val fraction = sourcePosition - lower
            (source[lower] + (source[upper] - source[lower]) * fraction)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, default: Int): Int =
        if (containsKey(key)) getInteger(key) else default

    private companion object {
        const val TIMEOUT_US = 10_000L
    }
}
