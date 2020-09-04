package com.sbrf.util.akka_

import com.sbrf.util.AkkaHttpDataFormatSupport
import spray.json.JsValue

import scala.xml.NodeSeq

object XMLSupport extends AkkaHttpDataFormatSupport[NodeSeq, Either[NodeSeq, JsValue]] {
}
