package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Props, Actor}
import play.libs.Akka
import actors.{TwitterConsumerActor, BroadcastActor}


object Application extends Controller {
  def index = Action {
    implicit request =>
      // causing a refresh on every 'index' hit is not a very good idea. Needs review.
      broadcastActor ! BroadcastActor.Refresh
      Ok(views.html.index())
  }

  val broadcastActor = Akka.system.actorOf(Props(classOf[BroadcastActor]))

  // should twitter consumer actor be moved inside broadcast? That way AppController
  // would only know of the broadcast and data/APIs should all be hidden behind Broadcast Facade.
  val twitterConsumerActor = Akka.system.actorOf(Props(classOf[TwitterConsumerActor], broadcastActor))


  def ws = WebSocket.using[String] { request =>

    val broadcast: (Enumerator[String], Concurrent.Channel[String]) = Concurrent.broadcast[String]
    broadcastActor ! BroadcastActor.Subscribe(broadcast._2)

    val out = broadcast._1
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }
    (in, out)
  }
}