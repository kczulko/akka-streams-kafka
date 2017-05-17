import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ThrottleMode}
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.duration._
import scala.language.postfixOps

object Main {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem.apply("akka-stream-kafka")
    implicit val materializer = ActorMaterializer()

    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers("127.0.0.1:9092")

    Kamon.start(ConfigFactory.load())
    val totalMessagesIn = Kamon.metrics.counter("TotalMessagesIn")

    Source.repeat(0)
      .scan(0)((next, _) => next + 1)
      .throttle(1, 1 second, 1, ThrottleMode.Shaping)
      .map(nextInt => {
        totalMessagesIn.increment()
        val topicName = "topic1"
        val partitionCount = 2
        val partition = nextInt % partitionCount
//        new ProducerRecord[Array[Byte], String](topicName, nextInt.toString.getBytes, nextInt.toString)
        new ProducerRecord[Array[Byte], String](topicName, partition, null, nextInt.toString)
      })
      .runWith(Producer.plainSink(producerSettings))
  }
}
