package net.jmmckinney.wowtrading.api

import cats.effect.IO
import cats.effect.kernel.Resource
import com.ocadotechnology.sttp.oauth2.AccessTokenProvider
import com.ocadotechnology.sttp.oauth2.Secret
import com.ocadotechnology.sttp.oauth2.cache.cats.CachingAccessTokenProvider
import eu.timepit.refined.types.string.NonEmptyString
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.client3.httpclient.fs2.HttpClientFs2Backend
import sttp.model.Uri
import io.circe.Json
import java.io.IOException
import net.jmmckinney.wowtrading.api.json.response.CommoditySnapshot
import com.typesafe.scalalogging.StrictLogging
import sttp.model.StatusCode
import doobie.enumerated.JdbcType.Timestamp
import sttp.model.HeaderNames
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZoneId
import scala.concurrent.duration._


private class BlizzardApi(
  tokenUrl: Uri,
  clientId: NonEmptyString,
  clientSecret: Secret[String]
)(using backend: SttpBackend[IO, Fs2Streams[IO] & WebSockets]) extends StrictLogging {
  val delegate = AccessTokenProvider[IO](tokenUrl, clientId, clientSecret)(backend)
  val tokenProvider = CachingAccessTokenProvider.refCacheInstance[IO](delegate)

  def getToken: IO[Secret[String]] = {
    for {
      provider <- tokenProvider
      token <- provider.requestToken(Option.empty)
    } yield token.accessToken
  }

  def getCommoditiesUntilValid(
    namespace: String,
    locale: String
  ): IO[(CommoditySnapshot, java.time.Instant)] = IO.defer{
    getCommodities(namespace, locale).flatMap(_ match {
      case Some(snapshot) => IO(snapshot)
      case None => IO.sleep(1.minute).flatMap(_ => getCommoditiesUntilValid(namespace, locale))
    })
  }

  def getCommodities(
    namespace: String,
    locale: String
  ): IO[Option[(CommoditySnapshot, java.time.Instant)]] = getToken.flatMap(token => {
    logger.info(s"Getting commodities for $namespace/$locale")
    val uri = Uri(
      java.net.URI(s"https://us.api.blizzard.com/data/wow/auctions/commodities?" +
        s"namespace=$namespace&" +
        s"locale=$locale&" +
        s"access_token=${token.value}"
      )
    )

    basicRequest.get(uri)
    .response(asStreamUnsafe(Fs2Streams[IO]))
    .send(backend).flatMap(response =>
      response.code match {
        case StatusCode.Ok => response.body.fold(
          e => {
            logger.error(s"Error retrieving commodities snapshot from blizzard: $e")
            IO(Option.empty)
          },
          v => v.through(fs2.text.utf8.decode).compile.string.map(CommoditySnapshot(_) match {
            case Some(snapshot) => {
              response.headers.collectFirst{
                case header if(header.name == "last-modified") => {
                  LocalDateTime.parse(
                    header.value,
                    DateTimeFormatter.ofPattern("EEE, d MMM uuuu HH:mm:ss zzz", Locale.US)
                  )
                  .atZone(ZoneId.of("Etc/GMT"))
                  .toInstant
                }
              } match {
                case Some(instant) => Some((snapshot, instant))
                case None => {
                  logger.error("Error getting commodity snapshot from blizzard: could not parse last-modified time from headers")
                  Option.empty
                }
              }
            }
            
            case None => {
              logger.error("Error getting commodity snapshot from blizzard: could not build snapshot from response body")
              Option.empty
            }
          })
        )
        case _ => {
          logger.error(s"Error retrieving commodities snapshot from blizzard: ${response.code}")
          IO(Option.empty)
        }
      }
    )
  })
}

object BlizzardApi {
  def use[A](
    clientId: NonEmptyString,
    clientSecret: Secret[String]
  )(f: BlizzardApi => IO[A]): IO[A] = {
    HttpClientFs2Backend.resource[IO]()
    .use(backend => {
      f(BlizzardApi(tokenUrl = Uri(new java.net.URI("https://oauth.battle.net/token")), clientId, clientSecret)(using backend))
    })
  }
}
