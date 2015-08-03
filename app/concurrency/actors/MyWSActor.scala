package concurrency.actors

import akka.actor.{ActorRef, Props}

object MyWSActor {
  def props(client: ActorRef) = Props(BroadCaster(client, "/user/playing_ws"))
}
