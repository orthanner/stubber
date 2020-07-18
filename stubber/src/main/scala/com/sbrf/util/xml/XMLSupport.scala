package com.sbrf.util.xml

import com.sbrf.util.DataFormatSupport
import spray.json.JsValue

import scala.xml.{Node, NodeSeq}

object XMLSupport extends DataFormatSupport[Node, NodeSeq, Either[NodeSeq, JsValue]] {
}
