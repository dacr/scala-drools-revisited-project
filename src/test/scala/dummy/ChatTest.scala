package dummy

import org.scalatest._
import Matchers._
import OptionValues._
import EitherValues._
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp._
import com.softwaremill.sttp.json4s._
import org.json4s.native.JsonMethods._

import org.slf4j.LoggerFactory

class ChatTest extends FunSuite {
  val logger = LoggerFactory.getLogger("ChatTest")

  implicit val formats = org.json4s.DefaultFormats


  test("sttp get usage") {
    implicit val backend = HttpURLConnectionBackend()
    val response = sttp.get(uri"http://httpbin.org/ip").send()
    val json = parse(response.body.right.value)
    val ip = (json\"origin").extract[String]
    ip should fullyMatch regex("""\d+[.]\d+[.]\d+[.]\d+""".r)
  }



  test("sttp post usage") {
    val signup = Some("yes")
    val request =
      sttp
        .body(Map("name" -> "John", "surname" -> "doe"))
        .post(uri"https://httpbin.org/post?signup=$signup")

    implicit val backend = HttpURLConnectionBackend()
    val response = request.send()

    val json = parse(response.body.right.value)
    (json\"args"\"signup").extract[String] should be("yes")
  }

  //implicit val sttpBackend = AkkaHttpBackend()

}
