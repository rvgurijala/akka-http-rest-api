package akkahttp.services

import akka.actor.{Actor, ActorLogging}
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

trait BookShopActor extends Actor with ActorLogging {
  implicit val mat: Materializer                  = Materializer(context)
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.global
}
