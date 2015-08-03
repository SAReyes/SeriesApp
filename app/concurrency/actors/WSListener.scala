package concurrency.actors

import akka.actor.Actor
import akka.routing.Listeners

class WSListener extends Actor with Listeners {
  override def receive = listenerManagement orElse {
    case any => gossip(any)
  }
}
