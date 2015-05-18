package concurrency.actors

import akka.actor.ActorRef
import controllers.PlayerController
import util.WSResponse

class WSRunnable(listener: ActorRef) extends Runnable {

  var prev_r: WSResponse = WSResponse(-1, null, -1, -1)

  // TODO: Verify which player is been used
  override def run() = {
    while (true) {
      // Verify the player current state
      PlayerController.player.get_variables() match {
        case Some(ws) =>
          // The player was found, now verify if it was changed along with the state
          if (prev_r.file_path != null && prev_r.file_path != ws.file_path && ws.state != -1) {
            PlayerController.player.update_file_info()
          }
          if(ws.state != -1){
            listener ! ws
            prev_r = ws
          }
          Thread.sleep(300) // There's no need to check continuously the player
        case None =>
          // In case the player can't be found verify if it was closed by reading
          // the previous state
          if (prev_r.file_path != null && !prev_r.file_path.isEmpty) {
            PlayerController.player.update_file_info()
          }
          prev_r = WSResponse(-1, null, -1, -1)
          listener ! prev_r //Send this msg as a way to tell there's no player process
      }
    }
  }
}
