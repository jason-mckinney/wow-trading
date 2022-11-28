package net.jmmckinney.wowtrading.model

import net.jmmckinney.wowtrading.api.json.response.CommoditySnapshot

case class ItemListing (
  time: java.time.Instant,
  region: String,
  auctionId: Long,
  itemId: Long,
  quantity: Long,
  unit_price: Long,
  time_left: String
)

object ItemListing {
  def fromCommoditySnapshot(snapshot: CommoditySnapshot, time: java.time.Instant, region: String): Seq[ItemListing] = {
    for {
      auctions <- snapshot.auctions
      itemListing = ItemListing(
        time = time,
        region = region,
        auctionId = auctions.id,
        itemId = auctions.item.id,
        quantity = auctions.quantity,
        unit_price = auctions.unit_price,
        time_left = auctions.time_left
      )
    } yield itemListing
  }

  def listingsFromEncoded(encodedListings: EncodedListings): Seq[ItemListing] = {
    encodedListings match {
      case EncodedListings(listings) => listings
    }
  }
}
