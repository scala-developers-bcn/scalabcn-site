package actors

import play.api.libs.iteratee.Concurrent
import akka.actor.{Props, Actor}
import play.api.libs.json._

/**
 * some case xxx to communicate with Broadcast Actor Family
 */
object BroadcastActor {

  case class Subscribe(endpoint: Concurrent.Channel[String])

  /** Publish JsValue transparently to all subscribed Channels */
  case class Publish(json: JsValue)

  /** Request refresh of data from APIs */
  case object Refresh

}

class BroadcastActor extends Actor {
  var endpoints: List[Concurrent.Channel[String]] = Nil;

  var eventsActor =
    context.system.actorOf(Props(classOf[MeetupsActor]))

  override def receive: Receive = {
    case BroadcastActor.Subscribe(endpoint) => endpoints = endpoint :: endpoints
    case BroadcastActor.Publish(v) => endpoints.foreach {
      _.push(Json.stringify(v))
    }
    case BroadcastActor.Refresh => {
      eventsActor ! MeetupsActor.Refresh
      // twitter API is not refreshed since that could cause twitter quota excess.
    }
  }
}
