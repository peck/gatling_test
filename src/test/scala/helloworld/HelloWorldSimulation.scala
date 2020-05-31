
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.util.Random


class HelloWorldSimulation extends Simulation {

  val feeder = Iterator.continually(
    Map("email" -> (Random.alphanumeric.take(20).mkString + "@foo.com"),
        "user_name" -> (Random.alphanumeric.take(20).mkString),
        "sender" -> (Random.nextInt((50 - 0) + 1).toString)
    )
  )


  val httpProtocol = http
    .baseUrl("https://dev.sugarcaneatl.com")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.9")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36")
    .wsBaseUrl("wss://dev.sugarcaneatl.com")

  val scn = scenario("BasicSimulation")
    .feed(feeder)
    .exec(http("Root")
            .get("/")
            .check(css("meta[name=csrf-token]", "content").saveAs("csrf_token"))
    )
    .exec(http("Age Verification")
            .post("/age_verification")
            .formParam("_csrf_token", "${csrf_token}")
            .formParam("age_verification[birth_date][month]", "5")
            .formParam("age_verification[birth_date][day]", "28")
            .formParam("age_verification[birth_date][year]", "1999")
    )
    .exec(http("User Registration Edit")
            .get("/users/register")
            .check(css("meta[name=csrf-token]", "content").saveAs("csrf_token"))
    )
    .exec(http("User Registration Create")
          .post("/users/register")
          .formParam("_csrf_token", "${csrf_token}")
          .formParam("user[user_name]", "${user_name}")
            .formParam("user[first_name]", "Test")
            .formParam("user[last_name]", "Test")
            .formParam("user[email]", "${email}")
            .formParam("user[password]", "password")
    )
    .exec(http("Live Stream")
            .get("/zCzLPJwyD6rm42mhsZVcEZCXmN00vB9xR/live")
            .check(css("meta[name=csrf-token]", "content").saveAs("csrf_token"))
            .check(css("div[data-phx-main=true]", "data-phx-session").saveAs("phx_session"))
            .check(css("div[data-phx-main=true]", "data-phx-static").saveAs("phx_static"))
            .check(css("div[data-phx-main=true]", "id").saveAs("phx_id"))
    )
    .exec(ws("Connect WS").connect("/live/websocket?_csrf_token=${csrf_token}&vsn=2.0.0"))
    .exec(ws("Join LiveView Channel")
            .sendText("""
["4", "4", "lv:${phx_id}", "phx_join", {"url": "http://dev.sugarcaneatl.com/cA2gVqg02MArWAiQvyfwsT1cP4mWBM00hb/live", "static": "${phx_static}", "session": "${phx_session}", "params": {"_csrf_token": "${csrf_token}"}}]
""")
    )
  .doIfEquals("${sender}", "1") {
    exec(ws("Send Here Message")
    .sendText("""
 ["4", "5", "lv:${phx_id}", "event", {"type": "form", "event": "new_message", "value": "_csrf_token=${csrf_token}&message[body]=still%20here"}]
 """)
  )
  }
    .repeat(120, "count") {
      exec(ws("Websocket Heartbeat")
             .sendText("""[null, ${count}, "phoenix", "heartbeat", {}]""")
      )
      .pause(30)
    }

   setUp(scn.inject(rampUsers(20000) during (10 minutes)))
    .maxDuration(15 minutes)
    .protocols(httpProtocol)

}
