package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{ Concurrent, Enumerator, Iteratee }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import scala.util.Random
import akka.actor.{ Props, Actor }
import play.libs.Akka
import scala.concurrent.{ Channel, Promise }

object RandomActor {

  case class Subscribe(endpoint: Concurrent.Channel[String])

  case class Publish(msg: String)

}

class RandomActor extends Actor {
  var endpoints: List[Concurrent.Channel[String]] = Nil;

  override def receive: Receive = {
    case RandomActor.Subscribe(endpoint) => endpoints = endpoint :: endpoints
    case RandomActor.Publish(v) => endpoints.foreach { _.push(v) }
  }

  context.system.scheduler.schedule(1 second, 1 second) {
    self ! RandomActor.Publish("" + Random.nextInt())
  }
}

object Application extends Controller {
  def index = Action { implicit request => Ok(views.html.index()) }

  val actor = Akka.system.actorOf(Props(classOf[RandomActor]))

  def ws = WebSocket.using[String] { request =>

    val broadcast: (Enumerator[String], Concurrent.Channel[String]) = Concurrent.broadcast[String]
    actor ! RandomActor.Subscribe(broadcast._2)

    val out = broadcast._1
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }
    (in, out)
  }

}