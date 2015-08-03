package concurrency.actors

import akka.actor.ActorRef
import controllers.PlayerController
import play.api.Logger
import util.WSResponse

class WSRunnable(listener: ActorRef) extends Runnable {

  var running = true

  def stop() = running = false

  var prev_r: WSResponse = WSResponse("", -1, -1, -1, -1)

  // TODO: Verify which player is been used
  override def run() = {
    while (running) {
      // Verify the player current state
      PlayerController.player.get_variables() match {
        case Some(ws) =>
          // The player was found, now verify if the file's been changed along with its state
          if (!prev_r.file_path.isEmpty && prev_r.file_path != ws.file_path && ws.state != -1) {
            Logger.info(s"Something changed within the player PREV($prev_r) CURRENT($ws)")
            PlayerController.player.update_file_info(prev_r)
          }
          if(ws.state != -1){
            listener ! ws
            prev_r = ws
          }
          Thread.sleep(300) // There's no need to check continuously the player
        case None =>
          // In case the player can't be found verify if it was closed by reading
          // the previous state
          if (!prev_r.file_path.isEmpty) {
            Logger.info(s"Player closed - updating last info $prev_r")
            PlayerController.player.update_file_info(prev_r)
          }
          prev_r = WSResponse("", -1, -1, -1, -1)
          listener ! prev_r //Send this msg as a way to tell there's no player process
      }
    }
  }
}
