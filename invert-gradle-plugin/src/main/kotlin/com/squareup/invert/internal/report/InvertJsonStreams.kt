package com.squareup.invert.internal.report

import com.squareup.invert.models.InvertSerialization.InvertJson
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.OutputStream

internal object InvertJsonStreams {

  @OptIn(ExperimentalSerializationApi::class)
  fun <T> write(
    file: File,
    serializer: SerializationStrategy<T>,
    value: T,
  ) {
    file.sink().buffer().use { sink ->
      write(sink.outputStream(), serializer, value)
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  fun <T> write(
    stream: OutputStream,
    serializer: SerializationStrategy<T>,
    value: T,
  ) {
    InvertJson.encodeToStream(
      serializer = serializer,
      value = value,
      stream = stream,
    )
  }

  @OptIn(ExperimentalSerializationApi::class)
  fun <T> read(
    file: File,
    deserializer: DeserializationStrategy<T>,
  ): T = file.source().buffer().use { source ->
    InvertJson.decodeFromStream(
      deserializer = deserializer,
      stream = source.inputStream(),
    )
  }

  @OptIn(ExperimentalSerializationApi::class)
  fun <T> encodedSizeInBytes(
    serializer: SerializationStrategy<T>,
    value: T,
  ): Long {
    val output = CountingOutputStream()
    write(output, serializer, value)
    return output.bytesWritten
  }

  private class CountingOutputStream : OutputStream() {
    var bytesWritten = 0L
      private set

    override fun write(value: Int) {
      bytesWritten++
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
      bytesWritten += length
    }
  }
}
