package net.jmmckinney.wowtrading.util.IO

import cats.effect.IO

extension[A](io: IO[A]) {
  def repeat(n: Int): IO[A] = IO.defer {
    if (n > 1)
      io.repeat(n-1).flatMap(_ => io)
    else 
      io
  }
}
