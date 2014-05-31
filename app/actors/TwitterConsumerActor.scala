package actors

import play.api.libs.json._
import akka.actor.{Actor, ActorRef}
import consumers.TwitterConsumer
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

case class Statuses(statuses: List[String])

object TwitterProtocol {
  implicit def StatusesFormat = Json.format[Statuses]

}

class TwitterConsumerActor(broadcastActorRef: ActorRef) extends Actor {
  private val scalaBcnObservable = TwitterConsumer.observable("scalabcn", 5);
  scalaBcnObservable.subscribe(
    (response) => {
      val status = response.statusCode
      println(s"TwitterConsumerActor: performed request ($status)")
      import TwitterProtocol._

      val msgs:Seq[JsValue] = (Json.parse(response.content) \ "statuses" \\ "text")

      broadcastActorRef ! BroadcastActor.Publish(
        JsObject(List(("twits", JsArray.apply(msgs))))
      )


    },
    (error) => System.err.println(s"TwitterConsumerActor: $error"),
    () => println(s"TwitterConsumerActor: scala bcn observable completed")
  )

  override def receive: Receive = {
    case _ => println(s"TwitterConsumerActor: Oh, oh, what to do, what to do...")
  }
}