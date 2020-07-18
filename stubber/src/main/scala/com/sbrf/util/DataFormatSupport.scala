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

  var transformers: Map[Uri.Path, DataTransformer] = Map[Uri.Path, DataTransformer]()

  private def transform(data: C, path: Uri.Path): Try[Option[R]] = Try {
    transformers.get(path) map( _ << data)
  }

  def register(transformer: DataTransformer): Unit =
    transformers = transformers + (Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()) -> transformer)

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
