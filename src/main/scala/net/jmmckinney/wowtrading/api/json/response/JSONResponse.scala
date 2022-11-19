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
  id: Int
)

case class Auctions (
  id: Int,
  item: Item,
  quantity: Int,
  unit_price: Int,
  time_left: String
)

case class CommodityResponse (
  _links: Links,
  auctions: Seq[Auctions]
)

object CommodityResponse {
  def apply(input: String): IO[Option[CommodityResponse]] = IO(decode[CommodityResponse](input).toOption)
}
