package net.jmmckinney.wowtrading.api

import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import sttp.model.Uri
import com.ocadotechnology.sttp.oauth2.Secret
import eu.timepit.refined.types.string.NonEmptyString
import scala.concurrent.duration._
import net.jmmckinney.wowtrading.app.Configuration
import reflect.Selectable.reflectiveSelectable

class BlizzardApiSpec extends Specification with CatsEffect:

  override val Timeout = 90.seconds

  val config = Configuration()

  "Able to obtain token" >> {
    for {
      token <- BlizzardApi.use(
        tokenUrl = Uri(new java.net.URI("https://oauth.battle.net/token")),
        clientId = NonEmptyString.from(config.Blizzard.clientId).toOption.get,
        clientSecret = config.Blizzard.secret
      )(blizzardApi => blizzardApi.getToken)

      result = token.value must not be_==("")
    } yield result
  }

  "Able to obtain commodity data" >> {
    for {
      response <- BlizzardApi.use(
        tokenUrl = Uri(new java.net.URI("https://oauth.battle.net/token")),
        clientId = NonEmptyString.from(config.Blizzard.clientId).toOption.get,
        clientSecret = config.Blizzard.secret
      )(blizzardApi => blizzardApi.getCommodities(namespace = "dynamic-us", locale = "en_US"))
      
      result = {
        response must beSome
        println(s"${response.get.auctions.length}")
        response.get.auctions.length must beGreaterThan(0)
      }
    } yield result
  }
