package com.uoft.audio

import java.awt._
import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.{JComboBox, JPanel}
import akka.actor.ActorRef
import com.uoft.audio.AudioReader.InitStream

class Graph(
             binWidth: Int,
             binHeight: Int,
             numHorizontalBins: Int,
             numVerticalBins: Int,
             verticalSpacing: Int,
             songList: Array[String],
             readerActor: ActorRef
           ) extends  JPanel {
  // Set the graph's display properties
  val graphWidth: Int = binWidth * numHorizontalBins
  val graphHeight: Int = (binHeight + verticalSpacing) * numVerticalBins
  setBackground(Color.BLACK)

  // Create drop down menu of all songs
  // When the user selects a new song, send a message to the reader actor to stop the current song
  // and start reading the new one
  val dropdown: JComboBox[String] = new JComboBox(songList)
  var currentSong: String = songList(0)

  dropdown.addActionListener(new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = {
      val newSong = dropdown.getSelectedItem.asInstanceOf[String]

      if (newSong != currentSong) {
        currentSong = newSong
        readerActor ! InitStream(newSong, first=false)
      }
    }
  })

  add(dropdown)

  // Initialize empty plot data
  var data: Array[Array[Double]] = Array.ofDim[Double](numHorizontalBins, numVerticalBins)

  // Shift fourier transform plot to the left when new data comes in
  // Redraw entire graph
  def updatePlot(newData: Array[Double]): Unit = {
    for (j <- (numHorizontalBins - 1) to 1 by -1) {
      data(j) = data(j - 1)
    }
    data(0) = newData

    repaint()
  }

  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)
    val g2d: Graphics2D = g.asInstanceOf[Graphics2D]

    for {
      i <- 0 until numHorizontalBins
      j <- data(i).indices
    } {
      val scale: Float = 1.2f
      val currData: Float = data(i)(j).toFloat
      val amp: Float = Math.min(scale * currData, 1)

      g2d.setColor(new Color(amp, amp, amp))
      g2d.fillRect(
        graphWidth - binWidth * (i + 1),
        graphHeight - (binHeight + verticalSpacing) * j,
        binWidth,
        binHeight
      )
    }
  }
}
