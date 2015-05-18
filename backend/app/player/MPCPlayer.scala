package player

import java.io.{FileNotFoundException, File}
import java.net.ConnectException

import controllers.PlayerController
import models.DBFiles
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.xml.sax.InputSource
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import play.api.libs.ws.WS
import util.WSResponse

import scala.concurrent._
import scala.concurrent.duration._
import scala.xml.parsing.NoBindingFactoryAdapter

import scala.slick.driver.H2Driver.simple._

case class MPCPlayer(exec: String, web_port: Int) extends Player {
  val CMD_PLAY = 887
  val CMD_PAUSE = 888
  val CMD_STOP = 890

  val CMD_SCENE_FORWARD = 922
  val CMD_SCENE_BACK = 921

  val CMD_FRAME_FORWARD = 891
  val CMD_FRAME_BACK = 892

  val CMD_NEXT_FILE = 920
  val CMD_PREVIOUS_FILE = 919

  override val url: String = s"http://localhost:$web_port"
  val url_cmd = s"$url/command.html"

  private def exec_cmd(cmd: Int) = {
    val dummy = WS.url(url_cmd).withQueryString("wm_command" -> cmd.toString).get()
    try {
      val response = Await.result(dummy, 500.milli).status
      Logger.info(s"response to cmd($cmd): $response")
      response
    }
    catch {
      case e: TimeoutException =>
        Logger.info(s"time out, no web interface found")
        404
    }
  }

  override def play() = exec_cmd(CMD_PLAY)

  override def stop() = exec_cmd(CMD_STOP)

  override def pause() = exec_cmd(CMD_PAUSE)

  override def scene_back(): Int = exec_cmd(CMD_SCENE_BACK)

  override def scene_forward(): Int = exec_cmd(CMD_SCENE_FORWARD)

  override def frame_back(): Int = exec_cmd(CMD_FRAME_BACK)

  override def frame_forward(): Int = exec_cmd(CMD_FRAME_FORWARD)

  override def previous_file(): Int = exec_cmd(CMD_PREVIOUS_FILE)

  override def next_file(): Int = {
    exec_cmd(CMD_NEXT_FILE)
  }

  var current_file = ""
  var current_position = 0L
  var current_file_duration = Long.MaxValue
  var current_state = 0
  var current_volume = 100

  val parseFactory = new SAXFactoryImpl
  val parser = parseFactory.newSAXParser()
  val source = new InputSource(url + "/variables.html")
  val adapter = new NoBindingFactoryAdapter

  override def get_variables(): Option[WSResponse] = {
    try {
      Await.result(WS.url(url + "/variables.html").get(), 300.milli)
      val feed = adapter.loadXML(source, parser)

      if(feed == null){
        // something goes wrong as the web interface didn't load properly
        Logger.info("The webinterface is returning null when parsing?")
        Some(WSResponse(current_position, current_file, current_volume, current_state))
      } else {
        val position = (feed \\ "body" \\ "p")
          .filter(e => (e \\ "@id").text == "position")
          .text.toLong
        val duration = (feed \\ "body" \\ "p")
          .filter(e => (e \\ "@id").text == "duration")
          .text.toLong
        val file_path = (feed \\ "body" \\ "p")
          .filter(e => (e \\ "@id").text == "filepath")
          .text

        val volume = (feed \\ "body" \\ "p")
          .filter(e => (e \\ "@id").text == "volumelevel")
          .text.toInt

        val state = (feed \\ "body" \\ "p")
          .filter(e => (e \\ "@id").text == "state")
          .text.toInt

        //      if (position == 0) {
        //        None
        //      }
        //      else {
        //        if(current_file != file_path){
        //          update_file_info()
        //        }

        if(state != -1){
          current_file = file_path
          current_position = position
          current_file_duration = if(duration == 0) Long.MaxValue else duration
          current_state = state
          current_volume = volume
          Some(WSResponse(position, file_path, volume, state))
        }
        else {
          Some(WSResponse(current_position, current_file, current_volume, current_state))
        }
      }
//      }

    }
    catch {
      case e: TimeoutException =>
//        if (!current_file.isEmpty) {
//          // this means the web interface was closed
//          update_file_info()
//        }
        None
      case e: ConnectException =>
        None
      case e: FileNotFoundException =>
        // Srsly? couldn't find the endpoint? TimeoutException should've gone off
        None
    }
  }

  def update_file_info() = {
    val database = Database.forDataSource(DB.getDataSource())
    database withSession { implicit rs =>
      val f = new File(current_file)
      if (current_file_duration - current_position <= PlayerController.tolerance) {
        Logger.info("Updated by tolerance: " + (current_file_duration,current_position,PlayerController.tolerance))
        DBFiles.update(f, DBFiles.FINISHED, 0)
      }
      else if (!current_file.isEmpty){
        if (current_position != 0) {
          Logger.info("Updated to pause" + (current_file_duration,current_position,PlayerController.tolerance))
          DBFiles.update(f, DBFiles.PAUSED, current_position)
        }
        else if (current_position == 0 && current_state != 2 && current_state != -1) {
          DBFiles.update(f, DBFiles.NEW, 0)
        }
      }
    }

    current_file = ""
    current_file_duration = 0L
    current_position = Long.MaxValue
  }
}
