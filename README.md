# Dependencies
* JDK 8: https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
* sbt: https://www.scala-sbt.org/1.x/docs/Setup.html
* Suitable IDE (i.e. Eclipse or Intellij)

# Technologies Used
* Scala
* Akka

# Description
* Audio files are streamed as byte arrays and processed concurrently in 2 ways
  1. Bytes are written to an audio mixer and played back
  2. The fourier transform of the bytes is calculated and plotted in real-time
* The fourier transform shows which frequencies make up the sound wave at each point in time

# Youtube Video
[![Whistling](https://img.youtube.com/vi/8S3herpSUmk/0.jpg)](https://www.youtube.com/watch?v=8S3herpSUmk)
