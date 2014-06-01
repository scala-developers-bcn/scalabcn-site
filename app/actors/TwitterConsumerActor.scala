package actors

import play.api.libs.json._
import akka.actor.{Actor, ActorRef}
import consumers.TwitterConsumer
import play.api.libs.json.JsObject
import play.api.libs.json.JsString


class TwitterConsumerActor(broadcastActorRef: ActorRef) extends Actor {
  //  TODO 'scalabcn' should be configurable.
  private val scalaBcnObservable = TwitterConsumer.observable("scalabcn", 50);
  scalaBcnObservable.subscribe(
    (response) => {
      val status = response.statusCode
      println(s"TwitterConsumerActor: performed request ($status)")
      if(status == 200) {
    	  val (texts, images) = TwitterConsumer.toTextsAndImages(response.content)
    	  broadcastActorRef ! BroadcastActor.Publish(texts)
          broadcastActorRef ! BroadcastActor.Publish(images)
      } 
    },
    (error) => System.err.println(s"TwitterConsumerActor: $error"),
    () => println(s"TwitterConsumerActor: scala bcn observable completed")
  )

  override def receive: Receive = {
    case _ => println(s"TwitterConsumerActor: Oh, oh, what to do, what to do...")
  }
}