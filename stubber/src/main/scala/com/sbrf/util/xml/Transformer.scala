package com.sbrf.util.xml

import com.sbrf.util.transformData

import scala.util.Try

trait Transformer[T, S, A, B, C] {
  val makeAttr: A => B
  val makeElement: B => C
  val valueExtractor: S => A
  private def function: List[S] => List[Try[C]] = transformData(valueExtractor, makeAttr, makeElement)

  def check: Try[C] => Boolean = _ => true

  final def apply(data: T): T = {
    val queue: List[Try[C]] = function(extractValue(data))
    render(queue filter check map {_.recover( e => recover(e)).get})
  }

  final def <<(data: T): T = apply(data)

  def extractValue(root: T): List[S]

  def render(queue: List[C]): T

  def recover(e: Throwable): C
}
