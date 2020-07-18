package com.sbrf.util.json

import akka.http.scaladsl.model.Uri
import com.sbrf.util.xml.Transformer
import com.sbrf.util.{BindTo, DataFormatSupport}

import scala.collection.immutable.HashMap
import scala.util.Try
import cats.implicits._
import spray.json.{JsNumber, JsValue}

object JsonSupport extends DataFormatSupport[JsValue, JsValue] {
  type JsonTransformer = Transformer[JsValue, JsValue, Int, JsNumber, JsValue, JsValue]

  var transformers: Map[Uri.Path, JsonTransformer] =
    new HashMap[Uri.Path, JsonTransformer]()

  def register(transformer: JsonTransformer): Unit =
    transformers = transformers + (Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()) -> transformer)

  override protected def transform(data: JsValue, path: Uri.Path): Try[Option[JsValue]] = Try {
    transformers.get(path) map { _ << data }
  }
}
