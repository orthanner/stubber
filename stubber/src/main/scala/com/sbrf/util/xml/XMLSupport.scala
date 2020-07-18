package com.sbrf.util.xml

import akka.http.scaladsl.model.Uri
import com.sbrf.util.{BindTo, DataFormatSupport}

import scala.collection.immutable.HashMap
import scala.util.Try
import scala.xml.{Node, NodeSeq}

object XMLSupport extends DataFormatSupport[Node, NodeSeq, NodeSeq] {

  var transformers: Map[Uri.Path, DataTransformer] =
    new HashMap[Uri.Path, DataTransformer]()

  def register(transformer: DataTransformer): Unit =
    transformers = transformers + ((Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()), transformer))

  override protected def transform(data: NodeSeq, path: Uri.Path): Try[Option[NodeSeq]] =
    Try {
      transformers.get(path) map {
        _ << data
      }
    }

}
