import java.io.File

import akka.actor.Props
import concurrency.actors.{FilesListener, WSListener, WSRunnable}
import controllers.FilesController
import models.DBFiles
import play.api.Play.current
import play.api.db.DB
import play.api.libs.concurrent.Akka
import play.api.{Application, GlobalSettings, Logger}

import scala.slick.driver.H2Driver.simple._

object Global extends GlobalSettings {

  var myRunnable: WSRunnable = null

  override def onStart(app: Application) = {
    Logger.info("init")
    val database = Database.forDataSource(DB.getDataSource())

    database withSession { implicit rs =>
      if (DBFiles.count == 0) {
        DBFiles.dumpFiles(new File(FilesController.root_dir))
      }
    }
    val listener = Akka.system.actorOf(Props[WSListener], name = "playing_ws")
    Akka.system.actorOf(Props[FilesListener], name = "files_ws")
    myRunnable = new WSRunnable(listener)
    new Thread(myRunnable).start()
  }

  override def onStop(app: Application): Unit = {
    myRunnable.stop()
  }
}
