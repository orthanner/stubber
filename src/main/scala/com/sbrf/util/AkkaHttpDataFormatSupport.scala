package com.sbrf.util

import akka.actor.ActorSystem
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}

import scala.concurrent.Future

trait AkkaHttpDataFormatSupport[C, R] extends DataFormatSupport[HttpRequest, C, R, HttpResponse, ActorRef] {
  private def rewrite(uri: Uri): Uri = uri.copy(authority = uri.authority.copy(port = 9999))

  override def getPathFromRequest(rq: HttpRequest): String = rq.uri.path.toString()

  def doRequest(rq: HttpRequest)(implicit sys: ActorSystem): Future[HttpResponse] =
    Http().singleRequest(rq.copy(uri = rewrite(rq.uri)))

  def proxy(dst: ActorRef[Response]): HttpResponse => Proxy = Proxy(_, dst)

  def apply(): Behavior[Command] =
    Behaviors.receive[Command] { (ctx: ActorContext[Command], command: Command) =>
      implicit val sys: ActorSystem = ctx.system.classicSystem
      command match {
        case Transform(nodes, rq, replyTo) =>
          val result = transform(rq, nodes) map {
            case Some(value) => Success(value)
            case None =>
              ctx.pipeToSelf(doRequest(rq))(_ map proxy(replyTo) recover {e => FailedRequest(replyTo).print(e)} get)
              NotFound
          }
          result recover { e => Failure(e) } filter NotFound.ne foreach replyTo.tell

        case Proxy(response, dst) =>
          dst ! Pass(response)

        case FailedRequest(dst) =>
          dst ! NotFound
      }
      Behaviors.same
    }

}
