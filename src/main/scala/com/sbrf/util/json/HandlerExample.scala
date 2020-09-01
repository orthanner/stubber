package com.sbrf.util.json

import akka.http.scaladsl.model.HttpRequest
import com.sbrf.util.{BindTo, Transformer}
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue}

import cats.implicits._

@BindTo("/demo")
object HandlerExample extends Transformer[HttpRequest, JsValue, JsValue, Int, JsNumber, JsValue, Either[_ <: JsValue, Nothing]] {
  override val makeAttr: (HttpRequest, Int) => JsNumber = (_, value) => JsNumber(value)
  override val makeElement: JsNumber => JsValue = identity
  override val valueExtractor: JsValue => Int = x => x.asInstanceOf[JsNumber].value.intValue + 5

  override def unwrapRequest(rq: HttpRequest, root: JsValue): List[JsValue] = root.asInstanceOf[JsArray].elements.toList

  override def render(rq: HttpRequest, queue: List[JsValue]): Result =
    JsObject("data" -> JsArray(queue.toVector)).asLeft

  override def recover(e: Throwable): JsValue =
    JsObject("error" -> JsString(e.getClass.getName), "message" -> JsString(e.getMessage))
}
