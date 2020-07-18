package com.sbrf.util
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.Uri

import scala.util.Try

trait DataFormatSupport[V, C, R] {
  type DataTransformer = Transformer[C, V, _, _, _, R]

  sealed trait Response
  final case class Success(nodes: R) extends Response
  final case class Failure(error: Throwable) extends Response

  sealed trait Command
  final case class Transform(data: C, path: Uri.Path, replyTo: ActorRef[Response]) extends Command

  protected def transform(data: C, path: Uri.Path): Try[Option[R]]

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
