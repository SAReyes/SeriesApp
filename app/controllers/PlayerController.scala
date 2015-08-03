package controllers

import java.io.File

import concurrency.actors.{PingSocket, MyWSActor}
import models.DBFiles
import play.api.Logger
import play.api.db.slick._
import play.api.libs.json.{Format, Json}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc.{Result, WebSocket, Action, Controller}
import player.Player
import util.WSResponse
import play.api.Play.current

object PlayerController extends Controller {

  // Json implicits
  implicit val ws_response_format = Json.format[WSResponse]
  implicit val ws_response_frame = FrameFormatter.jsonFrame[WSResponse]

  /**
   * This var sets window's time, this windows indicates the time in which the video
   * can be closed or skipped to be considered as FINISHED, the tolerance is written in
   * seconds in the .conf file
   */
  val tolerance = play.Play.application().configuration().getString("pi.tolerance").toInt * 1000

  /**
   * The player currently been used
   * TODO: let WSRunnable to identify the player that's been used,
   * TODO: so as to be completely abstarct to the user
   */
  var player = Player.build_mpc_player()

  /**
   * Executes the function f checking it's returning value
   * @param f the function to execute, must be some function from the player trait
   * @return if f's return value is 200 return http[200] else [400]
   */
  private def execWrapper(f: () => Int) = {
    if (f() == 200) {
      Ok
    } else {
      BadRequest("No file's being played")
    }
  }

  /**
   * executes f while changing the curren't file state to <b>new_state</b>
   * @param f the function to execute, must be some function from the player trait
   * @param new_state the new state
   * @param s the data base session should be implicit
   * @return the exec_wrapper result
   */
  private def file_changed_update_state(f: () => Int, new_state: Int = 0)
                                       (implicit s: Session) = {
    val prev = player.get_variables()
    val r = execWrapper(f)
    prev match {
      case Some(prev_vars) =>
        player.get_variables() match {
          case Some(new_vars) =>
            if (new_vars.file_path != prev_vars.file_path)
              Logger.info(s"File updated saving ${prev_vars.file_path} as $new_state")
            DBFiles.updateByFile(new File(prev_vars.file_path), new_state, 0)
          case None =>
            Logger.info("None received while reading the variables post-update")
        }
      case None =>
        Logger.info("None received while reading the variables pre-update")
    }
    r // Returning exec_wrapper result
  }

  // ###############################################################################
  // ###                          THE PLAYER SERVICE                             ###
  // ###############################################################################

  // GET @ /now_playing/play
  def playerPlay() = Action {
    execWrapper(player.play)
  }

  // GET @ /now_playing/pause
  def playerPause() = Action {
    execWrapper(player.pause)
  }

  def playerStop() = Action {
    execWrapper(player.stop)
  }

  def playerSceneBack() = DBAction { implicit rs =>
    file_changed_update_state(player.scene_back, new_state = DBFiles.NEW)
  }

  // TODO when the file changes because of this, save the state as FINISHED
  def playerSceneForward() = DBAction { implicit rs =>
    file_changed_update_state(player.scene_forward, new_state = DBFiles.FINISHED)
  }

  def playerFrameBack() = Action {
    execWrapper(player.frame_back)
  }

  def playerFrameForward() = Action {
    execWrapper(player.frame_forward)
  }

  // TODO atm this just plays the files in the dir, verify with the database
  def playerPreviousFile() = DBAction { implicit rs =>
    file_changed_update_state(player.previous_file, new_state = DBFiles.NEW)
  }

  def playerNextFile() = DBAction { implicit rs =>
    file_changed_update_state(player.next_file, new_state = DBFiles.FINISHED)
  }

  def playingWebsocket() = WebSocket.acceptWithActor[String, WSResponse] {
    request =>
      client =>
        Logger.info("Websocket connection request")
        MyWSActor.props(client)
  }

  private def ReadJsonField[T](field: String)
                              (f: (T) => Unit)
                              (validResponse: Result, errorResponse: Result)
                              (implicit formatter: Format[T]) =
    Action(parse.json) { request =>
      (request.body \ field).asOpt[T].map { resultData =>
        f.apply(resultData)
        validResponse
      }.getOrElse {
        errorResponse
      }
    }

  def setVolume() = ReadJsonField[Int]("volume") { (volume) =>
    player.setVolume(volume)
  }(Ok, BadRequest)

  def pingPoing() = WebSocket.acceptWithActor[String, String] {
    request =>
      client =>
        Logger.info("Ping connection")
        PingSocket.props(client)
  }
}
