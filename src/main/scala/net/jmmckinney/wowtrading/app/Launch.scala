package net.jmmckinney.wowtrading.app

import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import fs2.io.file.Files

import net.jmmckinney.wowtrading.api.json.response.CommodityResponse
import fs2.io.file.Path

object Launch extends IOApp {
  def measureAndPrint[A](a: IO[A]): IO[A] = for {
    startTime <- IO(System.currentTimeMillis)
    result <- a
    _ <- IO(println(s"expression took ${System.currentTimeMillis - startTime}ms"))
  } yield result

  def run(args: List[String]): IO[ExitCode] = 
    measureAndPrint(Files[IO].readAll(Path("/Users/jmckinney/Downloads/Response.json"))
      .map(_.toChar)
      .compile
      .toVector
      .flatMap(response => CommodityResponse(response.mkString))
      ).map(commodities => {
        ExitCode.Success
      })
}