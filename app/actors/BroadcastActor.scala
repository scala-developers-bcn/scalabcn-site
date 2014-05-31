package actors

import play.api.libs.iteratee.Concurrent
import akka.actor.{Props, Actor}


object BroadcastActor {

  case class Subscribe(endpoint: Concurrent.Channel[String])

  case class Publish(msg: String)

  case object Refresh

}

class BroadcastActor extends Actor {
  var endpoints: List[Concurrent.Channel[String]] = Nil;

  var eventsActor =
    context.system.actorOf(Props(classOf[MeetupsActor]))

  override def receive: Receive = {
    case BroadcastActor.Subscribe(endpoint) => endpoints = endpoint :: endpoints
    case BroadcastActor.Publish(v) => endpoints.foreach {
      _.push(v)
    }
    case BroadcastActor.Refresh => {
      eventsActor ! MeetupsActor.Refresh
    }
  }
}
