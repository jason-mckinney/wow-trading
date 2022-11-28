package net.jmmckinney.wowtrading.datastore

import net.jmmckinney.wowtrading.api.json.response.CommoditySnapshot
import cats.effect.IO
import net.jmmckinney.wowtrading.model.ItemListing

trait DatastoreResource {
  def allocated: IO[(Datastore, IO[Unit])]
  def use(f: Datastore => IO[Any]): IO[Any]
}

abstract class Datastore {
  def insertCommoditySnapshot(snapshot: CommoditySnapshot, time: java.time.Instant, region: String): IO[Int]

  def flushdb: IO[Unit]
  def itemListings: fs2.Stream[IO, ItemListing]
  def itemListingsAt(time: java.time.Instant): fs2.Stream[IO, ItemListing]
  def itemListingsFrom(from: java.time.Instant, to: java.time.Instant): fs2.Stream[IO, ItemListing]
  def latestSnapshot: fs2.Stream[IO, ItemListing] = latestSnapshots(1)
  def latestSnapshots(nLatest: Int): fs2.Stream[IO, ItemListing]
  def latestSnapshotTime: IO[Option[java.time.Instant]]
}
