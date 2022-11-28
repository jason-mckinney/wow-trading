package net.jmmckinney.wowtrading.datastore

import cats.effect.IO
import cats.effect.Resource
import com.typesafe.scalalogging.StrictLogging
import com.zaxxer.hikari.HikariDataSource
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.circe.jsonb.implicits._
import doobie.postgres.implicits._
import doobie.postgres.sqlstate
import doobie.util.ExecutionContexts
import doobie.util.transactor
import doobie.util.transactor.Transactor
import fs2.Stream
import io.circe.syntax._
import net.jmmckinney.wowtrading.api.json.response.CommoditySnapshot
import cats.effect.unsafe.IORuntime
import io.circe.Json
import net.jmmckinney.wowtrading.model.ItemListing
import io.circe.generic.auto._, io.circe.parser._
import doobie.Meta
import org.postgresql.util.PGobject
import doobie.util.update.Update
import net.jmmckinney.wowtrading.model.EncodedListings
import java.time.temporal.ChronoUnit


class TimescaleDatastoreResource(url: String, user: String, pass: String, threadPoolSize: Int = 16)
extends DatastoreResource
with StrictLogging {
  val postgres: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](threadPoolSize)
    transactor <- HikariTransactor.newHikariTransactor[IO](
      driverClassName = "org.postgresql.Driver",
      url = url,
      user = user,
      pass = pass,
      connectEC = ec
    )
  } yield transactor

  override def allocated: IO[(Datastore, IO[Unit])] = postgres.allocated
    .flatMap(allocated => {
      val datastore = TimescaleDatastore(allocated._1)
      datastore.createTablesIfMissing.map(_ => (datastore, allocated._2))
    })

  override def use(f: Datastore => IO[Any]): IO[Any] = {
    postgres.use(transactor => {
      logger.info("Creating TimescaleDatastore connection pool")
      for {
        datastore <- IO(TimescaleDatastore(transactor))
        _ <- datastore.createTablesIfMissing
      } yield datastore
    }.flatMap(datastore => f(datastore)))
  }
}

private class TimescaleDatastore(transactor: Transactor[IO]{type A = HikariDataSource}) 
extends Datastore 
with StrictLogging {
  private def createCommoditiesTableIfMissing: IO[Unit] = {
    sql"""
      CREATE TABLE IF NOT EXISTS commodities (
        time TIMESTAMPTZ NOT NULL,
        region VARCHAR (2) NOT NULL,
        itemid INT NOT NULL,
        listings JSONB NOT NULL
      );

      SELECT create_hypertable(
        'commodities',
        'time',
        if_not_exists => TRUE
      )
    """
    .query[Int]
    .stream
    .exceptSomeSqlState {
      case state if state.value == "02000" => {
        Stream.emits[doobie.ConnectionIO, Unit](Seq.empty)
      }
    }
    .transact(transactor)
    .compile
    .drain
  }
  
  def createTablesIfMissing: IO[Unit] = {
    for {
      tables <- getTables
      io <- tables match {
        case tables if (!tables.contains("commodities")) => {
          logger.info("Creating commodities table")
          createCommoditiesTableIfMissing
        }
        case _ => IO{}
      }
    } yield io
  }

  private def getTables: IO[List[String]] = {
    sql"SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'"
    .query[String]
    .to[List]
    .transact(transactor)
  }

  override def flushdb: IO[Unit] = {
    sql"DELETE FROM commodities"
    .update
    .run
    .transact(transactor)
    .map(_ => {})
  }

  override def insertCommoditySnapshot(
    snapshot: CommoditySnapshot,
    time: java.time.Instant,
    region: String
  ): IO[Int] = {
    logger.info("Inserting commodity snapshot")
    
    val listings = ItemListing.fromCommoditySnapshot(
      snapshot,
      time.truncatedTo(ChronoUnit.MILLIS),
      region
    )
    .groupBy(_.itemId)
    .map(group => EncodedListings(time, region, group._1, group._2)).toSeq

    Update[EncodedListings](
      """
      INSERT INTO commodities(time, region, itemid, listings)
      VALUES(?, ?, ?, ?)
      """
    ).updateMany(listings)
    .transact(transactor)
  }

  override def itemListings: fs2.Stream[IO, ItemListing] = {
    sql"SELECT * FROM commodities"
    .query[EncodedListings]
    .stream
    .transact(transactor)
    .flatMap(encoded => Stream.emits(ItemListing.listingsFromEncoded(encoded)))
  }
  override def itemListingsAt(time: java.time.Instant): fs2.Stream[IO, ItemListing] = {
    sql"SELECT * FROM commodities WHERE time=$time"
    .query[EncodedListings]
    .stream
    .transact(transactor)
    .flatMap(encoded => Stream.emits(ItemListing.listingsFromEncoded(encoded)))
  }
  
  override def itemListingsFrom(from: java.time.Instant, to: java.time.Instant): fs2.Stream[IO, ItemListing] = {
    sql"SELECT * FROM commodities WHERE time >= $from AND time <= $to"
    .query[EncodedListings]
    .stream
    .transact(transactor)
    .flatMap(encoded => Stream.emits(ItemListing.listingsFromEncoded(encoded)))
  }

  override def latestSnapshots(nLatest: Int): fs2.Stream[IO, ItemListing] = {
    sql"select * from commodities where time in (select distinct time from commodities order by time desc limit 1)"
    .query[EncodedListings]
    .stream
    .transact(transactor)
    .flatMap(encoded => Stream.emits(ItemListing.listingsFromEncoded(encoded)))
  }

  override def latestSnapshotTime: IO[Option[java.time.Instant]] = {
    sql"select distinct time from commodities order by time desc limit 1"
    .query[java.time.Instant]
    .stream
    .transact(transactor)
    .compile
    .last
  }
}
