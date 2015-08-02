package concurrency.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{Deafen, Listen}
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka

object PingSocket {
  def props(client: ActorRef) = Props(new PingSocket(client))
}

class PingSocket(client: ActorRef) extends Actor {

  override def receive: Receive = {
    case "ping" =>
      client ! "pong"
  }
}
