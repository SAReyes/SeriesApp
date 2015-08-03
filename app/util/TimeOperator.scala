package util

object TimeOperator {

  def time_to_string(timestamp: Long): String = {
    val sec = (timestamp / 1000) % 60
    val min = (timestamp / (1000*60)) % 60
    val hour = timestamp / (1000*60*60)
    s"$hour:$min:$sec"
  }

}
