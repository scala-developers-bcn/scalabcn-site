package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.util.Random
import akka.actor.{Props, Actor}
import play.libs.Akka
import scala.concurrent.{Channel, Promise}


object RandomActor {
  case class Subscribe(endpoint: Concurrent.Channel[String])
  case object Publish
}

class RandomActor extends Actor {
  var endpoint: Concurrent.Channel[String] = null;

  override def receive: Receive = {
    case RandomActor.Subscribe(endpoint) => this.endpoint = endpoint
    case RandomActor.Publish => {
      endpoint.push("" + Random.nextInt())
    }
  }

  context.system.scheduler.schedule(1 second, 1 second) {
    self ! RandomActor.Publish
  }
}

object Application extends Controller {
  def index = Action { implicit request => Ok(views.html.index()) }

  val actor = Akka.system.actorOf(Props(classOf[RandomActor]))


  def ws = WebSocket.using[String] { request =>

    // Log events to the console
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }


    val broadcast: (Enumerator[String], Concurrent.Channel[String]) = Concurrent.broadcast[String]
    actor ! RandomActor.Subscribe(broadcast._2)

    val out = broadcast._1


    (in, out)
  }

}