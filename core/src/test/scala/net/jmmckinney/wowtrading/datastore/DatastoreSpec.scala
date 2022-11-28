package net.jmmckinney.wowtrading.datastore

import org.specs2.mutable.Specification
import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import org.specs2.specification.AfterAll
import cats.effect.unsafe.IORuntime
import fs2.io.file.Files
import fs2.io.file.Path
import net.jmmckinney.wowtrading.api.json.response.CommoditySnapshot
import scala.concurrent.duration._
import net.jmmckinney.wowtrading.util.IO.repeat
import java.time.temporal.ChronoUnit


abstract class DatastoreSpec extends Specification
with CatsEffect
with AfterAll {
  sequential

  override val Timeout = 30.seconds
  given runtime: IORuntime = cats.effect.unsafe.IORuntime.global

  def datastoreResource: DatastoreResource
  var (datastore, shutdownHook): (Datastore, IO[Unit]) = datastoreResource.allocated.unsafeRunSync()


  def afterAll(): Unit = {
    datastore.flushdb.flatMap(_ => shutdownHook).unsafeRunSync()
  }

  "datastore is empty after flush" >> {
    for {
      _ <- datastore.flushdb
      snapshots <- datastore.itemListings.compile.toVector
      result <- IO(snapshots must beEmpty)
    } yield result
  }

  "can add commodity snapshot to datastore" >> {
    for {
      _ <- datastore.flushdb
      response <- Files[IO].readAll(Path("core/src/test/resources/commodities.json")).compile.toVector
      snapshot <- CommoditySnapshot(response.map(_.toChar).mkString)
      now = java.time.Instant.now.truncatedTo(ChronoUnit.MILLIS)
      _ <- datastore.insertCommoditySnapshot(snapshot.get, now, "US")
      rows <- datastore.itemListings.compile.count
      result = rows must be_==(snapshot.get.auctions.length)
    } yield result
  }

  "can retrieve item listings from a specific time from the datastore" >> {
   for {
      _ <- datastore.flushdb
      response <- Files[IO].readAll(Path("core/src/test/resources/commodities.json")).compile.toVector
      snapshot <- CommoditySnapshot(response.map(_.toChar).mkString)
      now = java.time.Instant.now.truncatedTo(ChronoUnit.MILLIS)
      later = now.plusSeconds(3600)
      _ <- datastore.insertCommoditySnapshot(snapshot.get, now, "US")
      _ <- datastore.insertCommoditySnapshot(snapshot.get, later, "US")
      rows <- datastore.itemListingsAt(now).compile.toVector
      result = {
        rows.head.time must be_==(now)
        rows.length must be_==(snapshot.get.auctions.length)
      }
    } yield result
  }

  "can retrieve listings from a time range from the datastore" >> {
    for {
      _ <- datastore.flushdb
      response <- Files[IO].readAll(Path("core/src/test/resources/commodities.json")).compile.toVector
      snapshot <- CommoditySnapshot(response.map(_.toChar).mkString)
      now = java.time.Instant.now.truncatedTo(ChronoUnit.MILLIS)
      later = now.plusSeconds(3600)
      _ <- datastore.insertCommoditySnapshot(snapshot.get, now, "US")
      _ <- datastore.insertCommoditySnapshot(snapshot.get, later, "US")
      rows <- datastore.itemListingsFrom(now, later).compile.count
      result = {
        rows must be_==(2 * snapshot.get.auctions.length)
      }
    } yield result
  }

  "can retrieve listings with the most recent timestamp from the datastore" >> {
    for {
      _ <- datastore.flushdb
      response <- Files[IO].readAll(Path("core/src/test/resources/commodities.json")).compile.toVector
      snapshot <- CommoditySnapshot(response.map(_.toChar).mkString)
      now = java.time.Instant.now.truncatedTo(ChronoUnit.MILLIS)
      later = now.plusSeconds(3600)
      _ <- datastore.insertCommoditySnapshot(snapshot.get, now, "US")
      _ <- datastore.insertCommoditySnapshot(snapshot.get, later, "US")
      rows <- datastore.latestSnapshot.compile.toVector
      result = rows.filter(_.time == later).size must be_==(rows.size)
    } yield result
  }

  "can retrieve listings with the 3 most recent timestamps from the datastore" >> {
    for {
      _ <- datastore.flushdb
      response <- Files[IO].readAll(Path("core/src/test/resources/commodities.json")).compile.toVector
      snapshot <- CommoditySnapshot(response.map(_.toChar).mkString)
      earlier = java.time.Instant.now.truncatedTo(ChronoUnit.MILLIS)
      last2 = earlier.plusSeconds(3600)
      last1 = last2.plusSeconds(3600)
      latest = last1.plusSeconds(3600)
      _ <- datastore.insertCommoditySnapshot(snapshot.get, earlier, "US")
      _ <- datastore.insertCommoditySnapshot(snapshot.get, last2, "US")
      _ <- datastore.insertCommoditySnapshot(snapshot.get, last1, "US")
      _ <- datastore.insertCommoditySnapshot(snapshot.get, latest, "US")
      rows <- datastore.latestSnapshots(3).compile.toVector
      result = rows.filter(row => {
        row.time == latest || row.time == last1 || row.time == last2
      }).size must be_==(rows.size)
    } yield result
  }

  "interacting with empty datastore yields no errors" >> {
    for {
      _ <- datastore.flushdb
      allListings <- datastore.itemListings.compile.count
      latestSnapshot <- datastore.latestSnapshot.compile.count
      latestSnapshots <- datastore.latestSnapshots(3).compile.count
      result = {
        allListings must be_==(0)
        latestSnapshot must be_==(0)
        latestSnapshots must be_==(0)
      }
    } yield result
  }
}
