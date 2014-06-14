package actors

import play.api.libs.json._
import akka.actor.{Actor, ActorRef}
import consumers.TwitterConsumer
import play.api.libs.json.JsObject
import play.api.libs.json.JsString


class TwitterConsumerActor(broadcastActorRef: ActorRef) extends Actor with akka.actor.ActorLogging {
  private val topicObservable = TwitterConsumer.observable(System.getenv("TWITTER_TOPIC"), 50);
  topicObservable.subscribe(
    (response) => {
      val status = response.statusCode
      if(status == 200) {
    	  val (texts, images) = TwitterConsumer.toTextsAndImages(response.content)
    	  broadcastActorRef ! BroadcastActor.Publish("TwitterConsumerActor.tweets", texts)
          broadcastActorRef ! BroadcastActor.Publish("TwitterConsumerActor.images", images)
      } 
    },
    (error) => log.error(s"TwitterConsumerActor: $error"),
    () => log.error(s"TwitterConsumerActor: scala bcn observable completed")
  )

  override def receive: Receive = {
    case _ => log.error(s"TwitterConsumerActor: Oh, oh, what to do, what to do...")
  }
}