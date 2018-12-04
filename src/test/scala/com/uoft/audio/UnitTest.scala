package com.uoft.audio

import java.io.File
import javax.sound.sampled.{AudioInputStream, AudioSystem}
import com.typesafe.config.{Config, ConfigFactory}
import com.uoft.audio.FFT.bitReverseCopy
import com.uoft.audio.Utilities.{listWavFiles, numBytesInTimeInterval, separateChannels, bufferProcessBytes}
import org.scalatest.FunSuite

class UnitTest extends FunSuite {
  val conf: Config = ConfigFactory.load()

  // Create audio stream to use for testing
  val musicFolder: String = conf.getString("audio.music-folder")
  val songList: Array[String] = listWavFiles(new File(musicFolder))
  val stream: AudioInputStream = AudioSystem.getAudioInputStream(new File(musicFolder + songList(0)))

  // Perform tests
  test("Number of bytes in time interval is rounded up to nearest power of 2") {
    assert(numBytesInTimeInterval(10, stream.getFormat) == 2048)
    assert(numBytesInTimeInterval(20, stream.getFormat) == 4096)
    assert(numBytesInTimeInterval(90, stream.getFormat) == 16384)
  }

  test("Stereo audio buffer is separated into two mono buffers") {
    val buffer = Array[Byte](1,2,3,4,5,6,7,8)

    assert(separateChannels(buffer, stream.getFormat)._1 sameElements Array[Byte](1,2,5,6))
    assert(separateChannels(buffer, stream.getFormat)._2 sameElements Array[Byte](3,4,7,8))
  }

  test("Byte buffer is converted into buffer of doubles with values between -1 and 1") {
    val buffer = Array[Byte](1,2,3,4)
    val bufferTransform = Array[Double](2 * (513 / 65535d) - 1, 2 * (1027 / 65535d) - 1)

    assert(bufferProcessBytes(buffer, stream.getFormat) sameElements bufferTransform)
  }

  test("Rearranging an array by bit-reversing the indices is successful") {
    val array = Array[Double](0.0, 1.0, 2.0, 3.0, 4.0 ,5.0, 6.0, 7.0)
    val n: Int = array.length

    assert(bitReverseCopy(array, n) sameElements Array[Double](0.0, 4.0, 2.0, 6.0, 1.0, 5.0, 3.0, 7.0))
  }
}
