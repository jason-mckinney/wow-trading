package net.jmmckinney.wowtrading.app

import com.typesafe.scalalogging.StrictLogging
import com.typesafe.config.ConfigFactory
import com.ocadotechnology.sttp.oauth2.Secret

class Configuration extends StrictLogging {
  val properties = ConfigFactory.load

  val Blizzard: AnyRef{
    val clientId: String
    val secret: Secret[String]
    val namespace: String
    val locale: String
  } = new {
    val clientId = properties.getString("blizzard.clientId")
    val secret = Secret[String](properties.getString("blizzard.secret"))
    val namespace = properties.getString("blizzard.namespace")
    val locale = properties.getString("blizzard.locale")
  }

  val Timescale: AnyRef{
    val numThreads: Int
    val serverName: String
    val portNumber: Int
    val databaseName: String
    val user: String
    val password: String
  } = new {
    val numThreads = properties.getInt("timescale.numThreads")
    val serverName = properties.getString("timescale.serverName")
    val portNumber = properties.getInt("timescale.portNumber")
    val databaseName = properties.getString("timescale.databaseName")
    val user = properties.getString("timescale.user")
    val password = properties.getString("timescale.password")
  }
}
