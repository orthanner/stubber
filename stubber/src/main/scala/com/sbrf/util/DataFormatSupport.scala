package com.sbrf.util
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.{HttpRequest, Uri}

import scala.util.Try

trait DataFormatSupport[V, C, R] {
  type DataTransformer = Transformer[C, V, _, _, _, R]

  sealed trait Response
  final case class Success(nodes: R) extends Response
  final case class Failure(error: Throwable) extends Response
  case object NotFound extends Response

  sealed trait Command
  final case class Transform(data: C, rq: HttpRequest, replyTo: ActorRef[Response]) extends Command

  var transformers: Map[Uri.Path, DataTransformer] = Map[Uri.Path, DataTransformer]()

  private def transform(rq: HttpRequest, data: C, path: Uri.Path): Try[Option[R]] = Try {
    transformers.get(path) map( _ << rq << data)
  }

  def register(transformer: DataTransformer): Unit =
    transformers = transformers + (Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()) -> transformer)

  def apply(): Behavior[Command] =
    Behaviors.receiveMessage {
      case Transform(nodes, rq, replyTo) =>
        val path = rq.uri.path
        val result = transform(rq, nodes, path) map {
          case Some(value) => Success(value)
          case None => NotFound
        }
        result recover { e => Failure(e) } foreach { replyTo ! _ }
        Behaviors.same
    }
}
