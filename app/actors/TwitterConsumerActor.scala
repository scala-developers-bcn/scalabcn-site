package actors

import play.api.libs.json.{JsObject, JsString, JsResult, Json}
import akka.actor.{Actor, ActorRef}
import consumers.TwitterConsumer

case class Status(text: String)

case class Statuses(statuses: List[Status])

object TwitterProtocol {

  implicit def StatusFormat = Json.format[Status]

  implicit def StatusesFormat = Json.format[Statuses]

}

class TwitterConsumerActor(broadcastActorRef: ActorRef) extends Actor {
  private val scalaBcnObservable = TwitterConsumer.observable("scalabcn", 5);
  scalaBcnObservable.subscribe(
    (response) => {
      val status = response.statusCode
      println(s"TwitterConsumerActor: performed request ($status)")
      import TwitterProtocol._

      val jsResult: JsResult[Statuses] = Json.fromJson[Statuses](
        Json.parse(response.content)
      )

      jsResult map { statuses =>
        val humanReadableMessages: String = statuses.statuses.map {
          _.text
        }.mkString("<br/>")
        broadcastActorRef ! BroadcastActor.Publish(
          JsObject(List(("msgs", JsString(humanReadableMessages))))
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