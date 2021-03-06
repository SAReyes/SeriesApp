package models

import java.io.{File, FilenameFilter}

import controllers.FilesController
import org.h2.jdbc.JdbcSQLException
import play.api.Logger
import play.api.db.slick.Config.driver.simple._

case class DBFile(id: Option[Long] = None,
                  name: String,
                  var state: Int,
                  dir: Boolean,
                  parent: String = null,
                  var position: Long = 0)

class DBFiles(tag: Tag) extends Table[DBFile](tag, "FILE") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name", O.NotNull)

  def state = column[Int]("state", O.NotNull)

  def dir = column[Boolean]("dir", O.NotNull)

  def parent = column[String]("parent", O.NotNull)

  def position = column[Long]("position", O.NotNull)

  def * = (id.?, name, state, dir, parent, position) <>(DBFile.tupled, DBFile.unapply)

  //  def unique_name = index("u_name", name, unique = true)
}

object DBFiles {

  val PAUSED = 0
  val NEW = 1
  val FINISHED = 2

  val files = TableQuery[DBFiles]

  def count(implicit s: Session) = Query(files.length).first

  def findById(id: Long)(implicit s: Session) = {
    files.filter(_.id === id).firstOption
  }

  def findByFile(file: File)(implicit s: Session) = {
    files.filter(_.name === file.getName).filter(_.parent === file.getParent).firstOption
  }

  /**
   * Finds an exact match by the name
   */
  def findByName(name: String)(implicit s: Session) = {
    files.filter(_.name === name).firstOption
  }

  def insert(file: DBFile)(implicit s: Session) = files insert file

  /**
   * Calls files.list without parameters (just fetch all the files)
   */
  def findAll(implicit s: Session) = files.list

  /**
   * If the parameter <strong>filter</strong> is specified this method will
   * fetch all the files that match <strong>"%filter%"</strong>, otherwise
   * this will return all the files
   */
  def list(filter: String = "%")(implicit s: Session) = {
    files.filter(_.name.toLowerCase like s"%${filter.toLowerCase}%").list
  }

  def minDirStatus(dir: DBFiles#TableElementType)(implicit s: Session) = {
    Logger.info(s"minDirStatus(${dir.name})")
    val dummy = new File(dir.parent, dir.name)
    files.filter(_.parent === dummy.getAbsolutePath)
      .sortBy(_.state.asc).firstOption.get.state
  }

  /**
   * Updates the <strong>status</strong> of a file identified by
   * <strong>id</strong> propagating upwards the status
   */
  def updateByID(id: Long, status: Int, position: Long = 0)
            (implicit s: Session): Option[DBFile] = {
    immersiveUpdate(findById(id), status, position)
  }

  /**
   * Updates the <strong>status</strong> of a file identified by
   * <strong>id</strong> propagating upwards the status
   */
  def updateByFile(file: File, status: Int, position: Long = 0)
            (implicit s: Session): Option[DBFile] = {
    immersiveUpdate(findByFile(file), status, position)
  }

  private def immersiveUpdate(file: Option[DBFile], status: Int, position: Long)
                             (implicit s: Session): Option[DBFile] = {
    file match {
      case Some(f) =>
        Logger.info(s"$f found as ${f.name} id=${f.id}} update to $status @ $position")
        Some(privateUpdate(f, status, position))
      case None =>
        Logger.info(s"$file can't be found in the database")
        None
    }
  }


  /**
   * Updates the file with a new position
   * PRE: The file exists
   * @return The file with its new data
   */
  private def privateUpdate(file: DBFiles#TableElementType, status: Int, position: Long)
                    (implicit s: Session): DBFile = {
    Logger.info(s"Updating ${file.name} to $status")
    file.state = status
    if (file.position != position)
      file.position = position

    // Update the file first
    files.filter(_.id === file.id).update(file)

    // Retrieve the parent, update it if it's in a different state
    // (Control the root directory as well)
    val parent_name = new File(file.parent)
    if (parent_name.getAbsolutePath != FilesController.root_dir) {
      findByFile(parent_name) match {
        case Some(parent) =>
          Logger.info(s"${file.id}'s parent: ${parent.id}:${parent.name}")
          if(parent.state != file.state)
            privateUpdate(parent, minDirStatus(parent), position)
        case _ =>
      }
    }

    file //based on the precondition
  }

  /**
   * Verifies if a file exist in the database AND system
   */
  def exist(file: File)(implicit s: Session) = {
    files.filter(_.name === file.getName)
      .filter(_.parent === file.getParent)
      .list.size == 1 && file.exists
  }

  /**
   * Removes a file from the database
   */
  def remove(file: DBFile)(implicit s: Session) = {
    files.filter(_.id === file.id).delete
  }
  /**
   * Dumps all the files from dir into the data base
   * @param dir the dir to dump
   */
  def dumpFiles(dir: File)(implicit s: Session): Unit = {
    if (dir.isDirectory) {
      val files = dir.listFiles(new Filter)
      // In case the dir exist in the database, first clean the removed files
      if (exist(dir)){
        for(f <- DBFiles.list_dir(dir.getAbsolutePath)){
          val db_f = new File(f.parent, f.name)
          if(!files.contains(db_f))
            DBFiles.remove(f)
        }
      }

      // Now add the new files, just if they don't exist
      for (f <- files) {
        try {
          //          if(dir.getAbsolutePath == Application.root_dir)
          if (!exist(f)) {
            DBFiles.insert(DBFile(
              name = f.getName,
              state = DBFiles.NEW,
              dir = f.isDirectory,
              parent = dir.getAbsolutePath))
            Logger.info(s"insert: ${f.getPath}")
          }
        }
        catch {
          case e: JdbcSQLException =>
            Logger.error(e.getStackTrace.toString)
        }
        if (f.isDirectory) dumpFiles(f)
      }
    }
  }

  /**
   * list all the files from the database given the current dir
   * IDEA: verify if there's a file not mapped when asked to list a dir
   */
  def list_dir(dir: String)(implicit s: Session) = {
    files.filter(_.parent === dir).list
  }

  /**
   * Filters the the files using <b><i>Application.filter</i></b> which refers
   * to the conf <b><i>pi.filter</i></b>
   */
  private class Filter extends FilenameFilter {

    override def accept(dir: File, name: String): Boolean = {
      var r = false
      for (s <- FilesController.filter) {
        r |= name.endsWith(s)
      }
      r || new File(dir, name).isDirectory
    }
  }

}