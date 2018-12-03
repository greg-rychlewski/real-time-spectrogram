import Utilities.log2
import breeze.math.Complex
import breeze.numerics.abs
import com.typesafe.config.{ConfigFactory, Config}
import scala.io.Source

object FFT {
  val conf: Config = ConfigFactory.load()

  // Lookup table to find the bit-reversed value for all integers within a given range
  val bitReverseLookup: Array[Int] = Source
    .fromResource(conf.getString("audio.bit-reverse-lookup"))
    .getLines.next.split(",")
    .map(_.toInt)

  // Return the bit-reversed order of an array and cast all values to complex numbers
  // These values will be used to calculate the fourier transform of an audio signal
  def bitReverseComplexCopy(array: Array[Double]): Array[Complex] = {
    array
      .zipWithIndex
      .map(t => Complex(array(bitReverseLookup(t._2)), 0))
  }

  // Different windows to be used in the fourier transform algorithm
  def hammingWindow(i: Int, n: Int): Double = {
    0.53836 - (0.46164 * Math.cos(2 * Math.PI  * i  / (n - 1)))
  }

  def hannWindow(i: Int, n: Int): Double = {
    0.5 - (0.5 * Math.cos(2 * Math.PI  * i  / (n - 1)))
  }

  def identityWindow(i: Int, n: Int): Double = {
    1
  }

  // Fast fourier transform algorithm based on bit-reversing the array of audio data
  def fastFourierTransform(signal: Array[Double], window: (Int, Int) => Double): Array[Double] = {
    val n: Int = signal.length
    val windowedSignal: Array[Double] = signal
      .zipWithIndex
      .map(t => t._1 * window(t._2, n))
    val fourierTransform: Array[Complex] = bitReverseComplexCopy(windowedSignal)

    for (s <- 1 to log2(n.toDouble).toInt) {
      val m: Int = Math.pow(2, s).toInt
      val omegaM: Complex = Complex(Math.cos(2 * Math.PI / m), -Math.sin(2 * Math.PI / m))

      for (k <- 0 until n by m) {
        var omega: Complex = Complex(1, 0)

        for (j <- 0 until m / 2) {
          val t: Complex = omega * fourierTransform(j + k +  (m / 2))
          val u = fourierTransform(k + j)

          fourierTransform(k + j) = u + t
          fourierTransform(k + j + (m / 2)) = u - t

          omega = omega * omegaM
        }
      }
    }

    fourierTransform
      .zipWithIndex.filter(_._2 < (n / 4))
      .map(x => Math.log(2 * abs(x._1) / n + 1))
  }
}
