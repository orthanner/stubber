package com.sbrf.util.json

import akka.http.scaladsl.model.Uri
import com.sbrf.util.{BindTo, DataFormatSupport}

import scala.collection.immutable.HashMap
import scala.util.Try
import spray.json.JsValue

object JsonSupport extends DataFormatSupport[JsValue, JsValue, JsValue] {

  var transformers: Map[Uri.Path, DataTransformer] =
    new HashMap[Uri.Path, DataTransformer]()

  def register(transformer: DataTransformer): Unit =
    transformers = transformers + (Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()) -> transformer)

  override protected def transform(data: JsValue, path: Uri.Path): Try[Option[JsValue]] = Try {
    transformers.get(path) map { _ << data }
  }
}
