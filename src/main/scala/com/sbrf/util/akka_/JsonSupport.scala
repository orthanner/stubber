package com.sbrf.util.akka_

import com.sbrf.util.AkkaHttpDataFormatSupport
import spray.json.JsValue

object JsonSupport extends AkkaHttpDataFormatSupport[JsValue, Either[JsValue, Nothing]] {
}
