package net.jmmckinney.wowtrading.model

import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.DecodingFailure
import com.typesafe.scalalogging.StrictLogging


given itemEncoder: Encoder[Item] = new Encoder[Item] {
  final def apply(a: Item): Json = Json.obj(
    ("auctionid", Json.fromLong(a.auctionId)),
    ("quantity", Json.fromLong(a.quantity)),
    ("unit_price", Json.fromLong(a.unit_price)),
    ("time_left", Json.fromString(a.time_left))
  )
}

given itemDecoder: Decoder[Item] = new Decoder[Item] {
  final def apply(c: HCursor): Decoder.Result[Item] = for {
    auctionId <- c.downField("auctionid").as[Long]
    quantity <- c.downField("quantity").as[Long]
    unit_price <- c.downField("unit_price").as[Long]
    time_left <- c.downField("time_left").as[String]
  } yield {
    Item(auctionId, quantity, unit_price, time_left)
  }
}

case class Item(
  auctionId: Long,
  quantity: Long,
  unit_price: Long,
  time_left: String
)

case class EncodedListings(
  time: java.time.Instant,
  region: String,
  itemId: Long,
  listings: Json
)

object EncodedListings extends StrictLogging {
  def apply(
    time: java.time.Instant,
    region: String,
    itemId: Long,
    listings: Seq[Item]
  ): EncodedListings = EncodedListings(
    time = time,
    region = region,
    itemId = itemId,
    listings = listings.asJson
  )

  def apply(time: java.time.Instant, region: String, itemid: Long, listings: Seq[ItemListing])(using d: DummyImplicit): EncodedListings = EncodedListings(
    time,
    region,
    itemid,
    listings.map(listing => Item(
      auctionId = listing.auctionId,
      quantity = listing.quantity,
      unit_price = listing.unit_price, 
      time_left = listing.time_left
    )).asJson
  )

  def unapply(encoded: EncodedListings): Some[Seq[ItemListing]] = {
    encoded.listings.as[Seq[Item]].fold(
      e => {
        logger.error(s"${e.message}")
        Some(Seq.empty)
      },
      v => Some(
        for {
          item <- v
        } yield ItemListing(
          time = encoded.time,
          region = encoded.region,
          auctionId = item.auctionId,
          itemId = encoded.itemId,
          quantity = item.quantity,
          unit_price = item.unit_price, 
          time_left = item.time_left
        )
      )
    )
  }
}
