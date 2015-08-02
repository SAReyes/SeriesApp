package concurrency.actors

import akka.actor.{Actor, ActorRef}
import akka.routing.{Deafen, Listen}
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.Play.current

/**
 * Created by Adrian on 23/07/2015.
 */
case class BroadCaster(client: ActorRef, listenerURL: String) extends Actor {
  val listener = Akka.system.actorSelection(listenerURL)


  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    Logger.info(s"$self Listening to $listenerURL")
    listener ! Listen(self)
  }

  override def receive: Receive = {
    case any => listener ! any
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    Logger.info(s"$self disconnecting from $listenerURL")
    listener ! Deafen(self)
  }
}
