package com.sbrf.util

import scala.util.Try

trait Transformer[T, S, A, B, C, R] {
  val makeAttr: A => B
  val makeElement: B => C
  val valueExtractor: S => A

  def check: Try[C] => Boolean = _ => true

  final def apply(data: T): R =
    render(transformData(valueExtractor, makeAttr, makeElement)(extractValue(data)) filter check map {_.recover(recover(_)).get})

  final def <<(data: T): R = apply(data)

  def extractValue(root: T): List[S]

  def render(queue: List[C]): R

  def recover(e: Throwable): C
}
