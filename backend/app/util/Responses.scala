package util

case class PlayRequestResponse(ws_uri: String)

case class WSResponse(file_path: String, position: Long, length: Long, volume: Int, state: Int)