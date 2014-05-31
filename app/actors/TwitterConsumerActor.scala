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
  private val scalaBcnObservable = TwitterConsumer.observable("scalabcn", 50);
  scalaBcnObservable.subscribe(
    (response) => {
      val status = response.statusCode
      println(s"TwitterConsumerActor: performed request ($status)")
      import TwitterProtocol._

      (Json.parse(response.content) \ "statuses") match {
        case arr: JsArray =>
          val x = arr.value.flatMap { s =>
            (s \ "entities" \ "media") match {
              case m: JsArray => m.value.map { e =>
                Some((e \ "media_url"))
              }
              case _ => None
            }
          }.flatten

          broadcastActorRef ! BroadcastActor.Publish(
            JsObject(List(("images", JsArray.apply(x))))
          )
          val msgs: Seq[JsValue] = arr.value.take(5).map {
            _ \ "text"
          }
          broadcastActorRef ! BroadcastActor.Publish(
            JsObject(List(("twits", JsArray.apply(msgs))))
          )
      }


    },
    (error) => System.err.println(s"TwitterConsumerActor: $error"),
    () => println(s"TwitterConsumerActor: scala bcn observable completed")
  )

  override def receive: Receive = {
    case _ => println(s"TwitterConsumerActor: Oh, oh, what to do, what to do...")
  }
}