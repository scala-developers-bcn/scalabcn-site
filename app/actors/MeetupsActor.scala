package actors

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global


object MeetupsActor {

  case object Refresh

  case class RequestRSVP(eventId: Int)

  case class RSVP(eventId: Int, count: Int = 0)

}

class MeetupsRsvpActor extends Actor {
  override def receive: Actor.Receive = {
    case _ =>
  }

}

case class MeetupEvent(id: String, time: Long, name: String)

object MeetupsProtocol {
  implicit def meetupEventFormat = Json.format[MeetupEvent]
}


class MeetupsActor extends Actor {

  val rsvpActor = context.system.actorOf(Props(classOf[MeetupsRsvpActor]))

  val key = System.getenv("MEETUP_API")

  override def receive: Receive = {
    case MeetupsActor.Refresh => {

      val broadcast = sender

      val f: Future[Response] = WS.url("http://api.meetup.com/2/events.json?status=past,upcoming" +
        "&group_id=3505122" +
        "&time=-2m,2m" +
        s"&key=${key}").get()


      f.map[MeetupEvent] { (r: Response) =>
        import MeetupsProtocol._

        val events = Json.fromJson[List[MeetupEvent]](Json.parse(r.body) \ "results").get
        val lastEvent: MeetupEvent = events.filter(_.time < System.currentTimeMillis()).sortWith {
          case (a, b) => a.time > b.time
        }.head
        lastEvent
      }.foreach { (e: MeetupEvent) =>
        println("Last event id is " + e.id.toInt)
        
        rsvpActor ! (MeetupsActor.RequestRSVP(e.id.toInt), broadcast)
      }
    }

  }


}

