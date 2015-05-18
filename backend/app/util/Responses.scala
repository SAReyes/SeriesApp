package util

case class PlayRequestResponse(ws_uri: String)

case class WSResponse(position: Long, file_path: String, volume: Int, state: Int)