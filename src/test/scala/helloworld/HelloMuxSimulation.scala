
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.util.Random


class HelloMuxSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://stream.mux.com")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.9")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.61 Safari/537.36")

  val scn = scenario("VideoSimulation")
    .exec(http("Parent M3U")
            .get("/27pNhwcfT5hCL3VSSvMmrM9RyskpXPzc.m3u8")
            .check(bodyString.saveAs("m3u8_parent"))
            .check(status is 200)
            .check(regex("RESOLUTION=480x270.*\n([^\n]*)").saveAs("480x720_uri"))
    )
    .exec(http("Child M3U")
            .get("${480x720_uri}")
            .check(bodyString.saveAs("m3u8_child"))
            .check(status is 200)
            .check(regex("#EXTINF:[^\n]*\n([^\n]*)").findAll.saveAs("mp4Segments"))
    )
  .foreach("${mp4Segments}", "mp4Segment") {
    exec(http("MP4 Segment")
            .get("${mp4Segment}")
           .check(status is 200)
    )
  }

  setUp(scn.inject(reampUsers(10000) during (1 minutes))
    .protocols(httpProtocol)
}
