package com.sbrf.util.xml

import com.sbrf.util.DataFormatSupport
import spray.json.JsValue

import scala.xml.NodeSeq

object XMLSupport extends DataFormatSupport[NodeSeq, Either[NodeSeq, JsValue]] {
}
