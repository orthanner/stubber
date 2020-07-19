package com.sbrf.util.json

import com.sbrf.util.DataFormatSupport

import spray.json.JsValue

object JsonSupport extends DataFormatSupport[JsValue, Either[JsValue, _]] {
}
