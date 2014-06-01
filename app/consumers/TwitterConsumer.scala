package consumers

import rx.lang.scala._
import play.api.libs.json._
import rx.lang.scala.schedulers._
import scala.concurrent.duration.Duration
import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.DurationLong
import scala.language.postfixOps
import scala.language.implicitConversions
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.commons.io.IOUtils
import org.apache.http.params.HttpParams
import scala.concurrent.{Future, ExecutionContext}
import rx.lang.scala.subjects.ReplaySubject
import scala.util.Success
import scala.util.Failure
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

case class QueryTopicResponse(topic: String, statusCode: Int, content: String)

class TwitterConsumer {
  private val consumerKey = System.getenv("TWITTER_CONSUMER_KEY")
  private val consumerSecret = System.getenv("TWITTER_CONSUMER_SECRET")
  private val accessToken = System.getenv("TWITTER_ACCESS_TOKEN")
  private val accessTokenSecret = System.getenv("TWITTER_ACCESS_TOKEN_SECRET")

  private val consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
  consumer.setTokenWithSecret(accessToken, accessTokenSecret);
  private val httpClient = new DefaultHttpClient();

  def queryTopic(topic: String, amount: Int) = {
    println(s"queryTopic $topic $amount")
    val request = new HttpGet(s"https://api.twitter.com/1.1/search/tweets.json?q=$topic&count=$amount");
    consumer.sign(request);
    val response = httpClient.execute(request);
    QueryTopicResponse(topic,
      response.getStatusLine().getStatusCode(),
      IOUtils.toString(response.getEntity().getContent()))
  }
}

case class Statuses(statuses: List[String])

object TwitterProtocol {
  implicit def StatusesFormat = Json.format[Statuses]
}

object TwitterConsumer {

  def observable(topic: String, amount: Int):Observable[QueryTopicResponse] = {
    val twitterConsumer = new TwitterConsumer
    Observable.timer(0 seconds, 5 seconds).map(t => twitterConsumer.queryTopic(topic, amount))
  }
  
  def toTextsAndImages(content: String, maxTweets: Int = 10, maxImages: Int = 5) = {
	  (Json.parse(content) \ "statuses") match {
        case arr: JsArray =>
          val x = arr.value.flatMap { s =>
            (s \ "entities" \ "media") match {
              case m: JsArray => m.value.map { e =>
                Some((e \ "media_url"))
              }
              case _ => None
            }
          }.flatten.distinct.take(maxImages)
    
         
          val msgs: Seq[JsValue] = arr.value.map {
            _ \ "text"
          }.take(maxTweets)
 
          (JsObject(List(("twits", JsArray.apply(msgs)))), JsObject(List(("images", JsArray.apply(x)))))
          
        case _ => (JsObject(Nil), JsObject(Nil))
      }
  }

  def main(args: Array[String]): Unit = {
    val consumer = new TwitterConsumer();
    println(consumer.queryTopic("scalabcn", 50).content);
  }
}
