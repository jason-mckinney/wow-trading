package net.jmmckinney.wowtrading.app

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
import fs2.io.file.Files
import fs2.io.file.Path
import net.jmmckinney.wowtrading.api.json.response._
import net.jmmckinney.wowtrading.api.json.response.CommoditySnapshot
import doobie.implicits._
import doobie.util.transactor
import com.typesafe.scalalogging.StrictLogging
import net.jmmckinney.wowtrading.datastore.TimescaleDatastoreResource
import net.jmmckinney.wowtrading.datastore.TimescaleDatastore
import reflect.Selectable.reflectiveSelectable
import net.jmmckinney.wowtrading.api.BlizzardApi
import eu.timepit.refined.types.string.NonEmptyString
import scala.concurrent.duration._
import scala.jdk.DurationConverters._
import java.time.temporal.ChronoUnit


object Launch extends IOApp with StrictLogging {
  def run(args: List[String]): IO[ExitCode] = {
    val config = Configuration()
    
    TimescaleDatastoreResource(
      url = s"jdbc:postgresql://${config.Timescale.serverName}:${config.Timescale.portNumber}/",
      user = s"${config.Timescale.user}",
      pass = s"${config.Timescale.password}",
      threadPoolSize = config.Timescale.numThreads
    ).use(datastore => {
      BlizzardApi.use(
        clientId = NonEmptyString.from(config.Blizzard.clientId).toOption.get,
        clientSecret = config.Blizzard.secret
      )(blizzardApi => {
        def pollUntilNewSnapshot: IO[Int] = IO.defer {
          logger.info("Checking if blizzard has a new commodity snapshot")
          datastore.latestSnapshotTime.flatMap(_ match {
            case Some(latestSnapshotTime) => {
              blizzardApi.getCommoditiesUntilValid(
                namespace = config.Blizzard.namespace,
                locale = config.Blizzard.locale
              )
              .flatMap(snapshot => {
                if (snapshot._2.isAfter(latestSnapshotTime)) {
                    logger.info("  New snapshot received")
                    datastore.insertCommoditySnapshot(snapshot._1, snapshot._2, config.Blizzard.locale.substring(config.Blizzard.locale.length-2))
                } else {
                  val timeToWait = Math.max(1, java.time.Instant.now.until(snapshot._2.plus(1.hours.toJava), ChronoUnit.MINUTES))
                  logger.info(s"No new snapshot - checking again in $timeToWait minute(s)")
                  IO.sleep(timeToWait.minutes).flatMap(_ => pollUntilNewSnapshot)
                }
              })
            }
            case None => {
              blizzardApi.getCommoditiesUntilValid(
                namespace = config.Blizzard.namespace,
                locale = config.Blizzard.locale
              )
              .flatMap(snapshot => {
                datastore.insertCommoditySnapshot(snapshot._1, snapshot._2, config.Blizzard.locale.substring(config.Blizzard.locale.length-2))
              })
            }
          })
        }

        pollUntilNewSnapshot
      }
      .map(rows => logger.info(s"Added snapshot with $rows rows to datastore. Waiting 1 hour for next snapshot..."))
      .andWait(58.minutes)
      .foreverM
    )
    }).map(_ => ExitCode.Success)
  }
}