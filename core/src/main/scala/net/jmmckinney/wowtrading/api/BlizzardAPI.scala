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

  def getCommodities(namespace: String, locale: String): IO[Option[CommoditySnapshot]] = getToken.flatMap(token => {
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
          v => v.through(fs2.text.utf8.decode).compile.string.flatMap(CommoditySnapshot(_))
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
    tokenUrl: Uri,
    clientId: NonEmptyString,
    clientSecret: Secret[String]
  )(f: BlizzardApi => IO[A]): IO[A] = {
    HttpClientFs2Backend.resource[IO]()
    .use(backend => {
      f(BlizzardApi(tokenUrl, clientId, clientSecret)(using backend))
    })
  }
}
