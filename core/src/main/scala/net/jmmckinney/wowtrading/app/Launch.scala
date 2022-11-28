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


object Launch extends IOApp with StrictLogging {
  def run(args: List[String]): IO[ExitCode] = 
    IO(ExitCode.Success)
  }