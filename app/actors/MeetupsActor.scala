package actors

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global


case class MeetupEvent(id: String, time: Long, name: String)

case class RSVP(event: MeetupEvent, rsvpCount: Int = 0)


object MeetupsProtocol {
  implicit def meetupEventFormat = Json.format[MeetupEvent]

  implicit def rsvpFormat = Json.format[RSVP]
}


object MeetupsActor {

  case object Refresh

  case class RequestRSVP(event: MeetupEvent)

  case class PastMeetupEvent(event: MeetupEvent)

  val key = System.getenv("MEETUP_API")

}

class MeetupsRsvpActor extends Actor {
  override def receive: Actor.Receive = {
    case MeetupsActor.RequestRSVP(e) =>
      val broadcast = sender

      val f: Future[Response] = WS.url(s"http://api.meetup.com/2/rsvps.json?event_id=${e.id}" +
        "&rsvp=yes" +
        s"&key=${MeetupsActor.key}").get()

      f.foreach { (r: Response) =>
        import MeetupsProtocol._

        val count = (Json.parse(r.body)\"meta"\"count").toString().toInt
        val rsvp = RSVP(e, count)
        broadcast ! BroadcastActor.Publish(
          JsObject(List(("next-event", Json.toJson(rsvp))))
        )
      }
  }

}


class MeetupsActor extends Actor {

  val rsvpActor = context.system.actorOf(Props(classOf[MeetupsRsvpActor]))


  override def receive: Receive = {
    case MeetupsActor.Refresh => {

      val broadcast = sender

      val f: Future[Response] = WS.url("http://api.meetup.com/2/events.json?status=past,upcoming" +
        "&group_id=3505122" +
        "&time=-2m,2m" +
        s"&key=${MeetupsActor.key}").get()

      val tomorrow = System.currentTimeMillis() + (24L * 60L * 60L * 100L)

      f.foreach { (r: Response) =>
        import MeetupsProtocol._

        val events = Json.fromJson[List[MeetupEvent]](Json.parse(r.body) \ "results").get
        val (past, future) = events.sortWith {
          case (a, b) => a.time > b.time
        }.partition(_.time < tomorrow)

        broadcast ! BroadcastActor.Publish(
          JsObject(List(("last-event", Json.toJson(past.head))))
        )
        rsvpActor.tell(MeetupsActor.RequestRSVP(future.reverse.head), broadcast)
      }
    }

  }


}

