package util

import play.api.libs.json.{Json => _, _}

object JsonConversion {

  
  implicit def value2JsonConverter[V : Writes](value: V) = new JsonSerializer(value)

  
  class JsonSerializer[V : Writes](value: V) {

    def toJson = play.api.libs.json.Json.toJson(value)
  }
}