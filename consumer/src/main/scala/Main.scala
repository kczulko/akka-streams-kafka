import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.Future

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem.apply("akka-stream-kafka")
    implicit val materializer = ActorMaterializer()

    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers("127.0.0.1:9092")
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1"))
      .mapAsync(1)(msg => {
        msg.committableOffset.commitScaladsl
        Future.successful(msg)
      })
      .runForeach(msg => println(s"partition: ${msg.record.partition}; value: ${msg.record.value}"))
  }
}
