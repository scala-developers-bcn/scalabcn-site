import org.specs2.mutable.Specification
import play.api.libs.json.Json
import scala.io.Source

class TwitterParserSpec extends Specification {

  "A TwitterParser" should {
    "extract the picture from a json-tweet" in {
      val in = getClass.getResourceAsStream("/samples/twitter/tweet_with_photo.json")
      in should not equalTo(null)

      val twitAsString = Source.fromInputStream(in).getLines().toList.mkString

      TwitterParser.photosOf(twitAsString) should equalTo(List("http://pbs.twimg.com/media/Bo-diCyCQAME2Ic.jpg"))
    }
  }
}

object TwitterParser {
  def photosOf(s: String) = {
    Json.parse(s) \ "entities" \ "media" \ "media_url"
  }

}
