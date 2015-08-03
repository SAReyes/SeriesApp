package player

import java.io.{FileNotFoundException, File}
import java.net.ConnectException

import concurrency.actors.UpdateMsg
import controllers.{FilesController, PlayerController}
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

  private def execCmd(cmd: Int, name: String, params: (String, String)*) = {

    var request = WS.url(url_cmd).withQueryString(("wm_command", cmd.toString))

    // Set extra parameters
    for (param <- params) {
      request = request.withQueryString(param)
    }

    // Make the http call
    val dummy = request.get()
    try {
      val response = Await.result(dummy, 500.milli).status
      Logger.info(s"[PLAYER} Request to $url_cmd | action: $name " +
        s"| params: ${request.queryString} | response: $response")
      response
    }
    catch {
      case e: TimeoutException =>
        Logger.info(s"time out, no web interface found")
        404
    }
  }


  override def setVolume(i: Int): Unit = execCmd(-2, "volume", ("volume", i.toString))

  override def play() = execCmd(CMD_PLAY, "play")

  override def stop() = execCmd(CMD_STOP, "stop")

  override def pause() = execCmd(CMD_PAUSE, "pause")

  override def scene_back(): Int = execCmd(CMD_SCENE_BACK, "scene back")

  override def scene_forward(): Int = execCmd(CMD_SCENE_FORWARD, "scene forward")

  override def frame_back(): Int = execCmd(CMD_FRAME_BACK, "frame back")

  override def frame_forward(): Int = execCmd(CMD_FRAME_FORWARD, "frame forward")

  override def previous_file(): Int = execCmd(CMD_PREVIOUS_FILE, "previous file")

  override def next_file(): Int = execCmd(CMD_NEXT_FILE, "next file")

  // This works as it is supposed just to be one file played at a given time
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
      Await.result(WS.url(url + "/variables.html").get(), 750.milli)
      val feed = adapter.loadXML(source, parser)

      if (feed == null) {
        // something goes wrong as the web interface didn't load properly
        Logger.info("The webinterface is returning null when parsing?")
        Some(WSResponse(current_file, current_position, current_file_duration, current_volume, current_state))
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

        // -1 -> N/A, triggered by no file
        // 0 -> stopped
        // 1 -> paused, when paused, goes into this when the file finishes as well
        // 2 -> playing
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

        if (state != -1) {
          current_file = file_path
          current_position = position
          current_file_duration = if (duration == 0) Long.MaxValue else duration
          current_state = state
          current_volume = volume
          Some(WSResponse(file_path, position, duration, volume, state))
        }
        else {
          Some(WSResponse(current_file, current_position, current_file_duration, current_volume, current_state))
        }
      }
      //      }

    }
    catch {
      case e @ (_ : TimeoutException | _ : ConnectException | _ : FileNotFoundException ) =>
        None
    }
  }

  def update_file_info(data: WSResponse) = this.synchronized {
    val database = Database.forDataSource(DB.getDataSource())
    database withSession { implicit rs =>
      DBFiles.findByFile(new File(data.file_path)) match {
        case Some(dbData) =>
          Logger.info(s"Update: Found this $dbData")
          if (data.length - data.position <= PlayerController.tolerance) {
            Logger.info(s"Updating $data to FINISHED by tolerance")
            DBFiles.updateByID(dbData.id.get, DBFiles.FINISHED, 0)
          } else {
            if (data.position != 0) {
              Logger.info(s"Updating $data to PAUSED)")
              DBFiles.updateByID(dbData.id.get, DBFiles.PAUSED, current_position)
            }
            else if (data.state != 2 && data.state != -1) {
              Logger.info(s"Updating $data to NEW")
              DBFiles.updateByID(dbData.id.get, DBFiles.NEW, 0)
            }
          }
        case None =>
          Logger.info("No database record")
      }
    }

    current_file = ""
    current_file_duration = 0L
    current_position = Long.MaxValue
    FilesController.broadcaster ! UpdateMsg("Current data updated")
  }
}
