package net.jmmckinney.wowtrading.app

import org.specs2.mutable.Specification
import cats.effect.testing.specs2.CatsEffect
import reflect.Selectable.reflectiveSelectable


class ConfigurationSpec extends Specification with CatsEffect {
  "configuration loaded with defaults" >> {
    val config = Configuration()

    config.Blizzard.clientId must not be_==("")
    config.Blizzard.secret.value must not be_==("")
    config.Blizzard.namespace must be_==("dynamic-us")
    config.Blizzard.locale must be_==("en_US")

    config.Timescale.serverName must be_==("timescale")
    config.Timescale.numThreads must be_==(16)
    config.Timescale.portNumber must be_==(5432)
    config.Timescale.databaseName must be_==("postgres")
    config.Timescale.user must be_==("postgres")
    config.Timescale.password must be_==("password")
  }
}
