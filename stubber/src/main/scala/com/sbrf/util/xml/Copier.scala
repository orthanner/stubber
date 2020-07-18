package com.sbrf.util.xml

import com.sbrf.util.BindTo

import scala.xml._

@BindTo("/")
object Copier extends Transformer[NodeSeq, Node, Int, UnprefixedAttribute, Elem, NodeSeq] {

  val makeAttr: Int => UnprefixedAttribute = v => new UnprefixedAttribute("content", v.toString, Null)

  val makeElement: UnprefixedAttribute => Elem = attr => Elem(null, "copy", attr, TopScope, minimizeEmpty = true)

  val valueExtractor: Node => Int = x => (x \@ "value").toInt + 2

  override def render(queue: List[Elem]): NodeSeq =
    <response>
      {queue}
    </response>

  override def recover(e: Throwable): Elem =
    new Elem(null, "failure", new UnprefixedAttribute("message", e.getMessage, Null), TopScope, minimizeEmpty = true)

  override def extractValue(root: NodeSeq): List[Node] = (root \ "element").theSeq.toList
}
