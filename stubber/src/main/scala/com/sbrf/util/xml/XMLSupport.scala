package com.sbrf.util.xml

import akka.http.scaladsl.model.Uri
import com.sbrf.util.{BindTo, DataFormatSupport}

import scala.collection.immutable.HashMap
import scala.util.Try
import scala.xml.{Elem, Node, NodeSeq, UnprefixedAttribute}

object XMLSupport extends DataFormatSupport[Node, NodeSeq] {

  var transformers: Map[Uri.Path, Transformer[NodeSeq, Node, Int, UnprefixedAttribute, Elem]] =
    new HashMap[Uri.Path, Transformer[NodeSeq, Node, Int, UnprefixedAttribute, Elem]]()

  def register(transformer: Transformer[NodeSeq, Node, Int, UnprefixedAttribute, Elem]): Unit =
    transformers = transformers + ((Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()), transformer))

  override protected def transform(data: NodeSeq, path: Uri.Path): Try[Option[NodeSeq]] =
    Try {
      transformers.get(path) map {
        _ << data
      }
    }

}
