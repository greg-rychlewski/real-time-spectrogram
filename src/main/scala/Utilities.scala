import java.io.File
import javax.sound.sampled.AudioFormat

object Utilities {
  // Get file extension from file name string
  def getFileExtension(fileName: String): String = {
    fileName.split("\\.").last
  }

  // Get list of wav files in a specific folder
  def listWavFiles(dir: File): Array[String] = {
    dir.listFiles
      .map(_.getName.toLowerCase)
      .filter(getFileExtension(_) == "wav")
  }

  // Take the logarithm (base2) of a number
  def log2(n: Double): Double = {
    Math.log(n) / Math.log(2)
  }

  // Find the lowest power of two greater than or equal to a given number
  def supremumPowTwo(n: Double): Int = {
    Math.pow(2, Math.ceil(log2(n))).toInt
  }

  // Take a buffer of bytes in little endian form and convert it to an array of ints
  // between -1 and 1
  def bufferProcessBytes(byteBuffer: Array[Byte], format: AudioFormat): Array[Double] = {
    val bytesPerSample: Int = format.getSampleSizeInBits / 8

    byteBuffer
      .map(java.lang.Byte.toUnsignedInt)
      .grouped(bytesPerSample)
      .map(bytes => {
        var sum: Double = 0
        var max: Double = 0

        for (i <- 0 until bytesPerSample) {
          sum += Math.pow(256, i) * bytes(i)
          max += Math.pow(256, i) * 255
        }

        2 * (sum / max) - 1
      }).toArray
  }

  // Take an array of bytes with interleaved information from two audio channels
  // and separate them into different arrays
  def separateChannels(buffer: Array[Byte], format: AudioFormat): (Array[Byte], Array[Byte]) = {
    val frameSize: Int = format.getFrameSize
    val channelArray = buffer
      .zipWithIndex.groupBy(_._2 % frameSize >= frameSize / 2)
      .map(t => t._2.unzip._1)
      .toArray

    (channelArray(0), channelArray(1))
  }

  // Find the number of bytes in a specific time interval (milliseconds)
  // Given a certain audio format
  def numBytesInTimeInterval(milliseconds: Int, format: AudioFormat): Int = {
    val sampleRate: Int = format.getSampleRate.toInt
    val bytesPerSample: Int = format.getFrameSize
    val seconds: Double = milliseconds / 1000f

    supremumPowTwo(sampleRate * seconds * bytesPerSample)
  }
}
