package concurrency.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.Listeners
import play.api.Logger

object FilesUpdater {
  val listener = "/user/files_ws"

  def props(client: ActorRef) = Props(new FilesUpdater(client))
}

class FilesUpdater(client: ActorRef) extends BroadCaster(client, FilesUpdater.listener){
  override def receive: Receive = {
    case data: UpdateMsg =>
      client ! data
    case _ =>
  }
}

case class UpdateMsg(data: String)

class FilesListener extends Actor with Listeners {
  override def receive = listenerManagement orElse {
    case data: UpdateMsg =>
      Logger.info("Broadcasting an update signal")
      gossip(data)
    case _ =>
  }
}
