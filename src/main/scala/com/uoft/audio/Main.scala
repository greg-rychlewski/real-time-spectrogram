package com.uoft.audio

import java.io._
import javax.sound.sampled._
import javax.swing.JFrame
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import com.uoft.audio.FFT.{fastFourierTransform, identityWindow}
import com.uoft.audio.Utilities.{bufferProcessBytes, listWavFiles, numBytesInTimeInterval, separateChannels}

// Actor reads audio stream into a buffer and then passes it along
object AudioReader {
  def props(
             sampleTime: Int,
             musicFolder: String,
             playerActor: ActorRef,
             plotterActor: ActorRef
           ): Props = {
    Props(new AudioReader(sampleTime, musicFolder, playerActor, plotterActor))
  }

  // Messages: read from current stream or initialize a new stream
  case object Read
  case class InitStream(song: String, first: Boolean)
}

class AudioReader(
                   sampleTime: Int,
                   musicFolder: String,
                   playerActor: ActorRef,
                   plotterActor: ActorRef
                 ) extends Actor {
  import AudioPlayer._
  import AudioPlotter._
  import AudioReader._

  // Variables originally set to null. They will be set once the actor receives a message
  var stream: AudioInputStream = _
  var buffer: Array[Byte] = _
  var bufferSize: Int = _

  def receive = {
    case InitStream(newSong: String, first) =>
      // Create stream from new song's file name
      val songPath = musicFolder + newSong
      stream = AudioSystem.getAudioInputStream(new File(songPath))
      bufferSize = numBytesInTimeInterval(sampleTime, stream.getFormat)
      buffer = new Array[Byte](bufferSize)

      // Send message to audio player to update its information
      // Send message to self to start reading new song
      if (!first) {
        playerActor ! CloseSourceLine
      }
      playerActor ! InitSourceLine(stream)
      self ! Read

    case Read =>
      // Get next values from stream
      val next = stream.read(buffer)

      // Shut down actor system if there is no more data to read
      if (next == -1) {
        context.system.terminate()
      }

      // Only pass first channel
      // Transform the bytes so that they are between -1 and 1 to make a nicer plot
      val channelOne: Array[Byte] = separateChannels(buffer, stream.getFormat)._1
      val channelOneTransform: Array[Double] = bufferProcessBytes(channelOne, stream.getFormat)

      // Send messages to play and plot the portion of the song that was just read
      plotterActor ! Plot(channelOneTransform.toList)
      playerActor ! Play(buffer.toList)
  }
}

// Actor receives audio buffer from reader actor and then plays the song
object AudioPlayer {
  def props: Props = Props[AudioPlayer]

  // Messages: initialize, write to, or close an audio source line
  case object CloseSourceLine
  case class Play(buffer: List[Byte])
  case class InitSourceLine(stream: AudioInputStream)
}

class AudioPlayer extends Actor{
  import AudioPlayer._
  import AudioReader._

  // Source line is set to null until actor receives a message initializing it
  var sourceLine: SourceDataLine = _

  def receive = {
    case InitSourceLine(newStream) =>
      // Initialize new source line
      sourceLine = AudioSystem.getSourceDataLine(newStream.getFormat)
      sourceLine.open()
      sourceLine.start()

    case Play(buffer) =>
      // Send message back to reader to get next batch of data
      sender ! Read

      // Write current audio buffer to source line
      sourceLine.write(buffer.toArray, 0, buffer.length)

    case CloseSourceLine =>
      // Disable current source line
        sourceLine.stop()
        sourceLine.close()

  }
}

// Actor receives audio buffer from reader actor and plots the Fourier Transform in real-time
object AudioPlotter {
  def props(songList: Array[String]): Props = {
    Props(new AudioPlotter(songList))
  }

  // Messages: plot current buffer, initialize reference to reader actor, initialize empty graph
  case class Plot(buffer: List[Double])
  case class InitReader(readerActor: ActorRef)
  case object InitGraph
}

class AudioPlotter(songList: Array[String]) extends Actor  {
  import AudioPlotter._

  // Config used to set the properties of the graph
  val conf: Config = ConfigFactory.load()

  // The graph and the reference to the reader actor are set to null
  // They will be initialized through messages
  var readerActor: ActorRef = _
  var fftGraph: Graph = _

  // Empty Java GUI window that will hold the graph
  val window: JFrame = new JFrame()
  window.setSize(conf.getInt("audio.window-width"), conf.getInt("audio.window-height"))

  def receive = {
    case InitReader(newReaderActor) =>
      // Get reference to current reader actor
      readerActor = newReaderActor

    case InitGraph =>
      // Initialize fourier transform graph
      // Contains a drop down menu so the user can switch between songs
      // Holds a reference to the reader actor so that a message can be sent to it
      // when the user selects a new song
      fftGraph = new Graph(
        conf.getInt("audio.fft-bin-width"),
        conf.getInt("audio.fft-bin-height"),
        conf.getInt("audio.graph-num-horizontal-bins"),
        conf.getInt("audio.fft-num-freq"),
        conf.getInt("audio.graph-vertical-spacing"),
        songList,
        readerActor
      )

      window.add(fftGraph)
      window.setVisible(true)

    case Plot(buffer) =>
      // Calculate fourier transform from audio buffer using fast fourier transform algorithm
      // Update current plot with new results
      val fourierTransform = fastFourierTransform(buffer.toArray, identityWindow)
      fftGraph.updatePlot(fourierTransform)
  }
}

object Main extends App {
  import AudioPlotter._
  import AudioReader._

  val conf: Config = ConfigFactory.load()

  // Get list of all wav files in music folder
  // The first song played will be determined by alphabetical order
  val musicFolder: String = conf.getString("audio.music-folder")
  val songList: Array[String] = listWavFiles(new File(musicFolder))
  val firstSong: String = songList(0)

  // Initialize actor system
  val system: ActorSystem = ActorSystem("audio")
  val plotter: ActorRef = system.actorOf(AudioPlotter.props(songList), "plotterActor")
  val player: ActorRef = system.actorOf(AudioPlayer.props, "playerActor")
  val reader: ActorRef = system.actorOf(
    AudioReader.props(
      conf.getInt("audio.sample-time"),
      musicFolder,
      player,
      plotter
    ),
    "readerActor"
  )

  // Send message to plotter actor to initialize its reference to the reader and to initialize an empty graph
  plotter ! InitReader(reader)
  plotter ! InitGraph

  // Send message to reader to start reading the first song
  reader ! InitStream(firstSong, first=true)
}