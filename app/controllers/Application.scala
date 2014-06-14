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
      Ok(views.html.index())
  }

  val broadcastActor = Akka.system.actorOf(Props(classOf[BroadcastActor]))

  // should twitter consumer actor be moved inside broadcast? That way AppController
  // would only know of the broadcast and data/APIs should all be hidden behind Broadcast Facade.
  val twitterConsumerActor = Akka.system.actorOf(Props(classOf[TwitterConsumerActor], broadcastActor))

  val broadcast: (Enumerator[String], Concurrent.Channel[String]) = Concurrent.broadcast[String]
  broadcastActor ! BroadcastActor.Subscribe(broadcast._2)
  val logger = Iteratee.foreach[String](m => println(s"broadcast: ${m.substring(0,10)}..."))
  broadcast._1 |>>> logger

  def ws = WebSocket.using[String] { request =>
    val out = broadcast._1
    val in = Iteratee.foreach[String](println).map { _ =>
      println("WebSocket client has disconnected")
    }
    broadcastActor ! BroadcastActor.ClientConnection
    (in, out)
  }
}