package com.sbrf.util
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.{HttpRequest, Uri}

import scala.util.Try
import cats.implicits._

/**
 * Базовый класс для акторов, осуществляющих обработку запросов. Конкретный обработчик выбирается исходя из переданного пути.
 * Представляет собой функцию `(Uri.Path, C) => Response[R]`. Для обработки запросов на определённый путь сначала
 * необходимо зарегистрировать функцию обработки запросов к этому пути.
 *
 * @tparam C тип исходных данных
 * @tparam R тип выходных данных
 *
 * @see com.sbrf.util.BindTo
 */
trait DataFormatSupport[C, R] {
  /**
   * Функция преобразования `C => R`
   */
  type DataTransformer = Transformer[C, _, _, _, _, R]

  /**
   * Ответ актора
   */
  sealed trait Response

  /**
   * Результат работы (если обработка выполнены успешно)
   * @param nodes выходные данные
   */
  final case class Success(nodes: R) extends Response

  /**
   * Результат обработки, завершившейся сбоем
   * @param error информация о возникшей исключительной ситуации
   */
  final case class Failure(error: Throwable) extends Response

  /**
   * Признак того, что обработчик не был найден
   */
  case object NotFound extends Response

  /**
   * Запрос к актору
   */
  sealed trait Command

  /**
   * Преобразовать запрос
   * @param data десериализованное тело запроса
   * @param rq запрос
   * @param replyTo адрес обработчика ответного сообщения
   */
  final case class Transform(data: C, rq: HttpRequest, replyTo: ActorRef[Response]) extends Command

  /**
   * справочник обработчиков для различных URI
   */
  var transformers: Map[Uri.Path, DataTransformer] = Map[Uri.Path, DataTransformer]()

  private def transform(rq: HttpRequest, data: C, path: Uri.Path): Try[Option[R]] = Try {
    transformers.get(path) fmap (_ << rq) map (_(data))
  }

  /**
   * регистрирует обработчик
   * @param transformer обработчик
   */
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
