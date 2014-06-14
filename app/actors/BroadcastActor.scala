package actors

import play.api.libs.iteratee.Concurrent
import akka.actor.{Props, Actor}
import play.api.libs.json._

import rx.lang.scala._
import rx.lang.scala.schedulers._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.language.implicitConversions

/**
 * some case xxx to communicate with Broadcast Actor Family
 */
object BroadcastActor {

  case class Subscribe(endpoint: Concurrent.Channel[String])

  /** Publish JsValue transparently to all subscribed Channels */
  case class Publish(key: String, json: JsValue)

  /** Request refresh of data from APIs */
  case object ClientConnection

}

class BroadcastActor extends Actor with akka.actor.ActorLogging{
  var endpoints: List[Concurrent.Channel[String]] = Nil;
  val heartBeat = Observable.timer(0 seconds, 5 minutes)
  var cachedPublications: Map[String, String] = Map()

  var eventsActor =
    context.system.actorOf(Props(classOf[MeetupsActor]))

  heartBeat.subscribe(_ => triggerPublicationsRefresh)

  override def receive: Receive = {
    case BroadcastActor.Subscribe(endpoint) => endpoints = endpoint :: endpoints
    case BroadcastActor.Publish(key, json) => updatePublication(key, json, pushContentToEndpoints)
    case BroadcastActor.ClientConnection => pushPublicationsToEndpoints
  }

  def updatePublication(key: String, json: JsValue, onUpdate: (String) => Unit) = {
    val jsonContent = Json.stringify(json)
    cachedPublications = cachedPublications + (key -> jsonContent)
    onUpdate(jsonContent)
  }

  def pushContentToEndpoints(jsonContent: String) = {
    endpoints.foreach (_.push(jsonContent))
  }

  def pushPublicationsToEndpoints() = {
    cachedPublications.values.foreach(pushContentToEndpoints)
  }

  def triggerPublicationsRefresh() = {
    eventsActor ! MeetupsActor.Refresh
  }
}
