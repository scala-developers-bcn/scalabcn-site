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
      broadcastActor ! BroadcastActor.Refresh
      Ok(views.html.index())
  }

  val broadcastActor = Akka.system.actorOf(Props(classOf[BroadcastActor]))
  val twitterConsumerActor = Akka.system.actorOf(Props(classOf[TwitterConsumerActor], broadcastActor))

  def wsEvents = WebSocket.using[String] { request =>

    val broadcast: (Enumerator[String], Concurrent.Channel[String]) = Concurrent.broadcast[String]
    broadcastActor ! BroadcastActor.Subscribe(broadcast._2)

    val out = broadcast._1
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }
    (in, out)
  }
  
  def wsNextEvents = wsEvents

}