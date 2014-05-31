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
import consumers.TwitterConsumer
import akka.actor.ActorRef

import play.api.libs.json._

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
}


 case class Status(text:String)
  case class Statuses(statuses:List[Status])
object TwitterProtocol{

  implicit def StatusFormat =Json.format[Status]
  implicit def StatusesFormat =Json.format[Statuses]

}

class TwitterConsumerActor(broadcastActorRef: ActorRef) extends Actor {
  private val scalaBcnObservable = TwitterConsumer.observable("scalabcn", 5);
  scalaBcnObservable.subscribe(
    (response) => {
      val status = response.statusCode
      println(s"TwitterConsumerActor: performed request ($status)")
      import TwitterProtocol._

      val jsResult:JsResult[Statuses] =  Json.fromJson[Statuses](
        Json.parse(response.content)
      )

      jsResult map { statuses =>
          val humanReadableMessages:String = statuses.statuses.map{_.text}.mkString("<br/>")
          broadcastActorRef ! RandomActor.Publish(humanReadableMessages)
      }

    },
    (error) => System.err.println(s"TwitterConsumerActor: $error"),
    () => println(s"TwitterConsumerActor: scala bcn observable completed")
  )

  override def receive: Receive = {
    case _ => println(s"TwitterConsumerActor: Oh, oh, what to do, what to do...")
  }
}

object Application extends Controller {
  def index = Action { implicit request => Ok(views.html.index()) }

  val broadcastActor = Akka.system.actorOf(Props(classOf[RandomActor]))
  val twitterConsumerActor = Akka.system.actorOf(Props(classOf[TwitterConsumerActor], broadcastActor))

  def wsEvents = WebSocket.using[String] { request =>

    val broadcast: (Enumerator[String], Concurrent.Channel[String]) = Concurrent.broadcast[String]
    broadcastActor ! RandomActor.Subscribe(broadcast._2)

    val out = broadcast._1
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }
    (in, out)
  }
  
  def wsNextEvents = wsEvents

}