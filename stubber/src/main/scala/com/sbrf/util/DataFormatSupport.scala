package com.sbrf.util

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}

import scala.util.Try
import cats.implicits._

import scala.concurrent.Future

/**
 * Базовый класс для акторов, осуществляющих обработку запросов. Конкретный обработчик выбирается исходя из переданного пути.
 * Представляет собой функцию `(Uri.Path, C) => Response[R]`. Для обработки запросов на определённый путь сначала
 * необходимо зарегистрировать функцию обработки запросов к этому пути.
 *
 * @tparam C тип исходных данных
 * @tparam R тип выходных данных
 * @see com.sbrf.util.BindTo
 */
trait DataFormatSupport[C, R] {
  /**
   * Функция преобразования `C => R`
   */
  type DataTransformer = Transformer[C, _, _, _, _, R]
  /**
   * справочник обработчиков для различных URI
   */
  var transformers: Map[Uri.Path, DataTransformer] = Map[Uri.Path, DataTransformer]()

  /**
   * регистрирует обработчик
   *
   * @param transformer обработчик
   */
  def register(transformer: DataTransformer): Unit =
    transformers = transformers + (Uri.Path(transformer.getClass.getAnnotation(classOf[BindTo]).value()) -> transformer)

  def apply(): Behavior[Command] =
    Behaviors.receive[Command] { (ctx: ActorContext[Command], command: Command) =>
      implicit val sys: ActorSystem = ctx.system.classicSystem
      command match {
        case Transform(nodes, rq, replyTo) =>
          val path = rq.uri.path
          val result = transform(rq, nodes, path) map {
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

  private def transform(rq: HttpRequest, data: C, path: Uri.Path): Try[Option[R]] = Try {
    transformers.get(path) fmap (_ << rq) map (_ (data))
  }

  private def proxy(dst: ActorRef[Response]): HttpResponse => Proxy = Proxy(_, dst)

  def rewrite(uri: Uri): Uri = uri.copy(authority = uri.authority.copy(port = 9999))

  private def doRequest(rq: HttpRequest)(implicit sys: ActorSystem): Future[HttpResponse] =
    Http().singleRequest(rq.copy(uri = rewrite(rq.uri)))

  /**
   * Ответ актора
   */
  sealed trait Response

  /**
   * Запрос к актору
   */
  sealed trait Command

  /**
   * Результат работы (если обработка выполнены успешно)
   *
   * @param nodes выходные данные
   */
  final case class Success(nodes: R) extends Response

  /**
   * Результат обработки, завершившейся сбоем
   *
   * @param error информация о возникшей исключительной ситуации
   */
  final case class Failure(error: Throwable) extends Response

  /**
   * Ответ внешнего сервиса для передачи клиенту
   * @param response ответ
   */
  final case class Pass(response: HttpResponse) extends Response

  /**
   * Преобразовать запрос
   *
   * @param data    десериализованное тело запроса
   * @param rq      запрос
   * @param replyTo адрес обработчика ответного сообщения
   */
  final case class Transform(data: C, rq: HttpRequest, replyTo: ActorRef[Response]) extends Command

  final case class Proxy(response: HttpResponse, dst: ActorRef[Response]) extends Command

  /**
   * Сообщение о том, что запрос выполнить не удалось
   * @param dst получатель сообщения
   */
  case class FailedRequest(dst: ActorRef[Response]) extends Command {
    def print(e: Throwable): this.type = {
      e.printStackTrace()
      this
    }
  }

  /**
   * Признак того, что обработчик не был найден
   */
  case object NotFound extends Response
}
