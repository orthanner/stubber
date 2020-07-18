package com.sbrf.util.json

import akka.http.scaladsl.model.HttpRequest
import com.sbrf.util.{BindTo, Transformer}
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue}

import cats.implicits._

@BindTo("/demo")
object HandlerExample extends Transformer[JsValue, JsValue, Int, JsNumber, JsValue, Either[_ <: JsValue, _]] {
  override val makeAttr: Int => JsNumber = JsNumber(_)
  override val makeElement: JsNumber => JsValue = identity
  override val valueExtractor: JsValue => Int = x => x.asInstanceOf[JsNumber].value.intValue + 5

  override def extractValue(root: JsValue): List[JsValue] = root.asInstanceOf[JsArray].elements.toList

  override def render(rq: HttpRequest, queue: List[JsValue]): Either[_ <: JsValue, _] =
    JsObject("data" -> JsArray(queue.toVector)).asLeft

  override def recover(e: Throwable): JsValue =
    JsObject("error" -> JsString(e.getClass.getName), "message" -> JsString(e.getMessage))
}
