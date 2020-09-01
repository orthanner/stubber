package com.sbrf

import cats._
import cats.implicits._

package object util {

  type UnsafeValue[A] = Either[Throwable, A]

  def transformData[X, A, B, C](valueExtractor: X => A, makeAttr: A => B, makeElement: B => C): List[X] => List[UnsafeValue[C]] = {
    import cats.instances.either._

    val safeExtractor: X => UnsafeValue[A] = x => MonadError[UnsafeValue, Throwable].catchNonFatal {valueExtractor(x)}

    Functor[List].lift(safeExtractor fmap Functor[UnsafeValue].lift(makeAttr fmap makeElement))
  }
}
