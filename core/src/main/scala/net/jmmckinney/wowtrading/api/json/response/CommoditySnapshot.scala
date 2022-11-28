package net.jmmckinney.wowtrading.api.json.response

import cats.effect.IO
import io.circe.generic.auto._, io.circe.parser._

case class Self (
  href: String
)

case class Links (
  self: Self
)

case class Item (
  id: Long
)

case class Auctions (
  id: Long,
  item: Item,
  quantity: Long,
  unit_price: Long,
  time_left: String
)

case class CommoditySnapshot private(
  _links: Links,
  auctions: Seq[Auctions]
)

object CommoditySnapshot {
  def apply(input: String): Option[CommoditySnapshot] = decode[CommoditySnapshot](input).toOption
}
