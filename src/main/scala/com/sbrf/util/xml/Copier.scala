package com.sbrf.util.xml

import akka.http.scaladsl.model.HttpRequest
import com.sbrf.util.{BindTo, Transformer}
import spray.json.JsValue

import scala.xml._
import cats.implicits._

@BindTo("/")
object Copier extends Transformer[HttpRequest, NodeSeq, Node, Int, UnprefixedAttribute, Elem, Either[NodeSeq, _ <: JsValue]] {

  val makeAttr: (HttpRequest, Int) => Option[UnprefixedAttribute] = (_, v) => new UnprefixedAttribute("content", v.toString, Null).some

  val makeElement: UnprefixedAttribute => Elem = attr => Elem(null, "copy", attr, TopScope, minimizeEmpty = true)

  val valueExtractor: Node => Int = x => (x \@ "value").toInt + 2

  override def render(rq: HttpRequest, queue: List[Elem]): Result =
    <response>
      {queue}
    </response>.asLeft

  override def recover(e: Throwable): Elem =
    new Elem(null, "failure", new UnprefixedAttribute("message", e.getMessage, Null), TopScope, minimizeEmpty = true)

  override def unwrapRequest(rq: HttpRequest, root: NodeSeq): List[Node] = (root \ "element").theSeq.toList
}
