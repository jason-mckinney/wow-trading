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
    // Files[IO].readAll(Path("/Users/jmckinney/Downloads/response.json"))
    //   .compile
    //   .toVector
    //   .flatMap(response => CommodityResponse(response.map(_.toChar).mkString))
    //   .flatMap(res => res match {
    //     case Some(commodities) => CommoditySnapshot(java.time.Instant.now(), "US" ,commodities).map(Some(_))
    //     case None => IO(Option.empty)
    //   })
    //   .flatMap(snapshot => {
    //     TimescaleDatastoreResource(
    //       url = "jdbc:postgresql://127.0.0.1/",
    //       user = "postgres",
    //       pass = "Kloud!1235"
    //     ).use(datastore => {
    //       snapshot match {
    //         case Some(value) => datastore.insertCommoditySnapshot(value)
    //         case None => IO{}
    //       }
    //         // set up and run gRPC stuff and other logic in here
            
    //         // one the scope of the function passed into `use` is exited,
    //         // the SQL transactor from hikari is cleaned up automatically

    //         // unlike Futures, Resources can be recycled,
    //         // and `use` can be called again to create another transactor/connection pool
    //     })
    //   })
    IO(ExitCode.Success)
  }