package com.sbrf.util.json

import com.sbrf.util.BindTo
import com.sbrf.util.xml.Transformer
import spray.json.{JsArray, JsNumber, JsObject, JsString, JsValue}

@BindTo("/demo")
object HandlerExample extends Transformer[JsValue, JsValue, Int, JsNumber, JsValue] {
  override val makeAttr: Int => JsNumber = JsNumber _
  override val makeElement: JsNumber => JsValue = identity
  override val valueExtractor: JsValue => Int = x => x.asInstanceOf[JsNumber].value.intValue + 5

  override def extractValue(root: JsValue): List[JsValue] = root.asInstanceOf[JsArray].elements.toList

  override def render(queue: List[JsValue]): JsValue = JsObject("data" -> JsArray(queue.toVector))

  override def recover(e: Throwable): JsValue =
    JsObject("error" -> JsString(e.getClass.getName), "message" -> JsString(e.getMessage))
}
