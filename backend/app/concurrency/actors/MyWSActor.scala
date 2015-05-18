package concurrency.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{Deafen, Listen}
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka

object MyWSActor {
  def props(client: ActorRef) = Props(new MyWSActor(client))
}

class MyWSActor(client: ActorRef) extends Actor {
  val listener = Akka.system.actorSelection("/user/playing_ws")

  override def preStart(): Unit = {
    listener ! Listen(self)
  }

  def receive = {
    case any =>
      client ! any
  }

  override def postStop(): Unit = {
    Logger.info("Closing this websocket " + this)
    listener ! Deafen(self)
  }
}
