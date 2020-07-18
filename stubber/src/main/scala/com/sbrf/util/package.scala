package com.sbrf

import cats._
import cats.implicits._

import scala.util.Try

package object util {
  def transformData[X, A, B, C](valueExtractor: X => A, makeAttr: A => B, makeElement: B => C): List[X] => List[Try[C]] = {
    val safeExtractor: X => Try[A] = x => Try {valueExtractor(x)}
    Functor[List].lift(safeExtractor fmap Functor[Try].lift(makeAttr fmap makeElement))
  }
}
