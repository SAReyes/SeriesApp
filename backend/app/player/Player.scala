package player

import java.io.File

import models.DBFile
import play.api.Logger
import util.{TimeOperator, WSResponse}

/**
 * This trait contains all the methods to be used with a player
 */
trait Player {
  def setVolume(i: Int): Unit

  val exec: String
  val url: String

  def play(): Int
  def stop(): Int
  def pause(): Int

  def scene_forward(): Int
  def scene_back(): Int

  def frame_forward(): Int
  def frame_back(): Int

  def next_file(): Int
  def previous_file(): Int

  def get_variables(): Option[WSResponse]

  def update_file_info(data: WSResponse): Unit

  def execute(file: DBFile): Unit = {
    execute(new File(file.parent,file.name).getAbsolutePath, file.position)
  }
  def execute(file: String, position: Long): Unit = {
    new ProcessBuilder(exec, file,
      "/startpos", TimeOperator.time_to_string(position))
      .start()
  }
}

/**
 * A factory
 */
object Player{
  def build_mpc_player(): Player = {
    Logger.info(s"Building an mpc player")
    MPCPlayer(play.Play.application().configuration().getString("pi.players.mpc.exec"),
      play.Play.application().configuration().getInt("pi.players.mpc.port"))
  }
}