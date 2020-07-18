package com.sbrf.util
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.Uri

import scala.util.Try

trait DataFormatSupport[V, C] {
  sealed trait Response
  final case class Success(nodes: C) extends Response
  final case class Failure(error: Throwable) extends Response

  sealed trait Command
  final case class Transform(xml: List[V], path: Uri.Path, replyTo: ActorRef[Response]) extends Command

  protected def transform(xml: List[V], path: Uri.Path): Try[Option[C]]

  def apply(): Behavior[Command] =
    Behaviors.receiveMessage {
      case Transform(nodes, path, replyTo) =>
        val result = transform(nodes, path) map {
          case Some(value) => Success(value)
          case None => Failure(new NoSuchElementException("No handler found for: " + path.toString()))
        }
        result recover { e => Failure(e) } foreach { replyTo ! _ }
        Behaviors.same
    }
}
