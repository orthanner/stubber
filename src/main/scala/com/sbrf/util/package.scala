package com.sbrf

import cats._
import cats.implicits._

package object util {

  type UnsafeValue[A] = Either[Throwable, A]

  def transformData[R, X, A, B, C](valueExtractor: X => A, makeAttr: (R, A) => B, makeElement: B => C): R => List[X] => List[UnsafeValue[C]] = {
    import cats.instances.either._

    val safeExtractor: X => UnsafeValue[A] = x => MonadError[UnsafeValue, Throwable].catchNonFatal {valueExtractor(x)}

    val transformElement: R => A => B = rq => makeAttr(rq, _)

    rq: R => Functor[List].lift(safeExtractor fmap Functor[UnsafeValue].lift(transformElement(rq) fmap makeElement))
  }
}
