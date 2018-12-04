package com.uoft.audio

import breeze.math.Complex
import breeze.numerics.abs
import com.uoft.audio.Utilities.log2
import scala.io.Source

object FFT {
  // Return the bit-reversed order of an array
  // These values will be used to calculate the fourier transform of an audio signal
  def bitReverseCopy(array: Array[Double], n: Int): Array[Double] = {
    val bitReverseLookup = Source
      .fromResource("lookups/bit_reverse_lookup_" + n)
      .getLines.next.split(",")
      .map(_.toInt)

    array
      .zipWithIndex
      .map(t => array(bitReverseLookup(t._2)))
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
    val fourierTransform: Array[Complex] = bitReverseCopy(windowedSignal, n).map(Complex(_, 0))

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
