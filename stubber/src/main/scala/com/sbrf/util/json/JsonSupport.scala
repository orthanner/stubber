package com.sbrf.util.json

import akka.http.scaladsl.model.Uri
import com.sbrf.util.xml.Transformer
import com.sbrf.util.{BindTo, DataFormatSupport}

import scala.collection.immutable.HashMap
import scala.util.Try
import cats.implicits._
import spray.json.{JsArray, JsNumber, JsValue}

object JsonSupport extends DataFormatSupport[JsValue, JsValue] {
  var transformers: Map[Uri.Path, Transformer[JsValue, JsValue, Int, JsNumber, JsValue]] =
    new HashMap[Uri.Path, Transformer[JsValue, JsValue, Int, JsNumber, JsValue]]()

  def register(transformer: Transformer[JsValue, JsValue, Int, JsNumber, JsValue]): Unit =
    transformers = transformers + (Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()) -> transformer)

  override protected def transform(data: JsValue, path: Uri.Path): Try[Option[JsValue]] = Try {
    transformers.get(path) map { _ << data }
  }
}
