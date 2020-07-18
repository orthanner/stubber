package com.sbrf.util

import akka.http.scaladsl.model.HttpRequest

import scala.util.Try

trait Transformer[T, S, A, B, C, R] {
  case class Apply(rq: HttpRequest)
  {
    def << : T => R = data =>
      render(rq, transformData(valueExtractor, makeAttr, makeElement)(extractValue(data)) filter check map {_.recover(recover(_)).get})
  }
  val makeAttr: A => B
  val makeElement: B => C
  val valueExtractor: S => A

  def check: Try[C] => Boolean = _ => true

  final def apply: HttpRequest => Apply = rq => Apply(rq)

  final def << : HttpRequest => Apply = apply

  def extractValue(root: T): List[S]

  def render(rq: HttpRequest, queue: List[C]): R

  def recover(e: Throwable): C
}
