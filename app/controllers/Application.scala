package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

object Application extends Controller {


  def index = Action { implicit request =>
    Ok(views.html.index())
  }


  def ws = WebSocket.using[String] { request =>

    // Log events to the console
    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }

    // Send a single 'Hello!' message
    val out = Enumerator(""  + Random.nextInt())

    (in, out)
  }

}