package com.sbrf.util.xml

import akka.http.scaladsl.model.Uri
import com.sbrf.util.{BindTo, DataFormatSupport}

import scala.collection.immutable.HashMap
import scala.util.Try
import scala.xml.{Elem, Node, NodeSeq, UnprefixedAttribute}

object XMLSupport extends DataFormatSupport[Node, NodeSeq] {
  type XmlTransformer = Transformer[NodeSeq, Node, Int, UnprefixedAttribute, Elem, NodeSeq]

  var transformers: Map[Uri.Path, XmlTransformer] =
    new HashMap[Uri.Path, XmlTransformer]()

  def register(transformer: XmlTransformer): Unit =
    transformers = transformers + ((Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()), transformer))

  override protected def transform(data: NodeSeq, path: Uri.Path): Try[Option[NodeSeq]] =
    Try {
      transformers.get(path) map {
        _ << data
      }
    }

}
