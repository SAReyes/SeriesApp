package controllers

import java.io.File

import concurrency.actors.{UpdateMsg, FilesUpdater, MyWSActor}
import messages.FileUpdate
import models.{DBFile, DBFiles}
import play.api.Logger
import play.api.Play.current
import play.api.db.slick._
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc._

object FilesController extends Controller {

  implicit val fileUpdateFormat = Json.format[FileUpdate]
  implicit val fileFormat = Json.format[DBFile]
  implicit val fileWSFormat = Json.format[UpdateMsg]
  implicit val fileWSFrame = FrameFormatter.jsonFrame[UpdateMsg]

  // ##############    COMMON USE VARIABLES    ##############
  // TODO: read from an external .conf (i.e: using typesafe's lib) and implement some kind of strategy to change players

  /**
   * Specifies the file filter used by the app
   */
  val filter = play.Play.application().configuration().getString("pi.filter").split(",")

  /**
   * The root directory that will be scanned
   */
  val root_dir = play.Play.application().configuration().getString("pi.root")

  /**
   * The .exe run by the current player
   */
  var player_exec = play.Play.application().configuration().getString("pi.players.mpc.exec")

  /**
   * The port where the player's web interface is running
   */
  var player_port = play.Play.application().configuration().getString("pi.players.mpc.port")

  /**
   * References websocket broadcaster's actor
   */
  val broadcaster = Akka.system.actorSelection(FilesUpdater.listener)

  def index(any: String) = Action {
    Logger.info(s"Index: $any")
    Ok(views.html.index())
  }

  def ws(id: String) = WebSocket.acceptWithActor[String, String] {
    request =>
      client =>
        MyWSActor.props(client)
  }

  /**
   * Used by:
   * <br><i>
   * GET @ /service/files/:file
   * </i><br><br>
   * List the files from the database, using the parameter <b>file</b> as an exact matcher
   * @param file the filter to match
   * @return http[200] - content: a list of <i>models.DBFile</i> as json
   */
  def list(file: String) = DBAction { implicit rs =>
    Logger.info(s"request: $file")
    val dummy = play.utils.UriEncoding.decodePath(file, "UTF-8")
    val f = new File(FilesController.root_dir, dummy)
    Logger.info(s"fetching: ${f.getAbsolutePath} dir:${f.isDirectory} exist:${f.exists()}")
    if (f.isDirectory) {
      // If asked for a directory return all the directory content
      Ok(Json.toJson(DBFiles.list_dir(f.getAbsolutePath)))
    }
    else if (DBFiles.exist(f)) {
      // If asked for a single file, execute the player strategy
      DBFiles.findByFile(f) match {
        case Some(dbfile) =>
          PlayerController.player.execute(dbfile)
          Ok(Json.toJson(DBFiles.list(f.getPath)))
        case None =>
          BadRequest("This shouldn't happen")
      }
    }
    else {
      // Couldn't find any match
      NotFound
    }
  }

  /**
   * Used by:
   * <br><i>
   * GET @ /service/updateFiles
   * </i><br><br>
   * Scans the root directory in case there's a new file to be added to the database
   * @return http[200]
   */
  def updateFiles() = DBAction { implicit rs =>
    DBFiles.dumpFiles(new File(FilesController.root_dir))
    Ok
  }

  /**
   * Used by:
   * <br><i>
   * GET @ /service/files/
   * </i><br><br>
   * List the files from the database at the root directory
   * @return http[200] - content: a list of <i>models.DBFile</i> as json
   */
  def listRoot() = DBAction { implicit rs =>
    Ok(Json.toJson(DBFiles.list_dir(FilesController.root_dir)))
  }

  /**
   * Used by:
   * <br><i>
   * GET @ /service/files
   * </i><br><br>
   * Sets the DBFile, which id is <b>id</b> in the data base, to the new <b>status</b>, the position
   * of the video will be overwritten by 0.<br> The position is meant to be automatically updated by
   * <i>concurrency.actors.WSRunnable</i>
   * @return http[200]
   */
  def updateFile() = DBAction (parse.json) { implicit rs =>
    rs.request.body.validate[FileUpdate].map {
      file =>
        Logger.info(s"Update request - $file")
        DBFiles.updateByID(file.id, file.state) match {
          case Some(newFile) =>
            Ok("Updated")
          case None =>
            BadRequest("Couldn't update the file")
        }
    }.getOrElse(BadRequest("Update request - invalid request"))
  }

  def say_hello() = Action {
    Ok("Hello World!")
  }

  var cosa = 0

  def test() = Action {
    cosa += 1
    System.setProperty("derpy.derp", cosa.toString)
    Ok(s"here you go: ${play.Play.application().configuration().getString("derpy.derp")} | $cosa")
  }

  def get_log() = Action {
    Ok.sendFile(new File("logs", "application.log")).as("text/plain")
  }

  /**
   * This endpoint will broadcast a message whenever a file status
   * was updated, will not listen to messages, and the message returned
   * is meaningless
   */
  def filesEvents() = WebSocket.acceptWithActor[String, UpdateMsg]{
    request =>
      client =>
        Logger.info(s"Connecting a file_WS ${request.remoteAddress}")
        FilesUpdater.props(client)
  }
}