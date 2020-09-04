package com.sbrf.util

import cats.implicits._

import scala.util.Try

/**
 * Базовый класс для акторов, осуществляющих обработку запросов. Конкретный обработчик выбирается исходя из переданного пути.
 * Представляет собой функцию `(String, C) => Response[R]`. Для обработки запросов на определённый путь сначала
 * необходимо зарегистрировать функцию обработки запросов к этому пути.
 *
 * @tparam Q тип запроса
 * @tparam C тип исходных данных
 * @tparam R тип выходных данных
 * @tparam P тип ответа прокси
 * @tparam T тип получателя ответов
 * @see com.sbrf.util.BindTo
 */
trait DataFormatSupport[Q, C, R, P, T[_]] {
  /**
   * Функция преобразования `C => R`
   */
  type DataTransformer = Transformer[Q, C, _, _, _, _, R]
  /**
   * справочник обработчиков для различных URI
   */
  var transformers: Map[String, DataTransformer] = Map[String, DataTransformer]()

  /**
   * регистрирует обработчик
   *
   * @param transformer обработчик
   */
  def register(transformer: DataTransformer): Unit =
    transformers = transformers + (transformer.getClass.getAnnotation(classOf[BindTo]).value() -> transformer)

  def transform(rq: Q, data: C): Try[Option[R]] = Try {
    transformers.get(getPathFromRequest(rq)) fmap (_ << rq) map (_ (data))
  }

  def getPathFromRequest(rq: Q): String

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
  final case class Pass(response: P) extends Response

  /**
   * Преобразовать запрос
   *
   * @param data    десериализованное тело запроса
   * @param rq      запрос
   * @param replyTo адрес обработчика ответного сообщения
   */
  final case class Transform(data: C, rq: Q, replyTo: T[Response]) extends Command

  final case class Proxy(response: P, dst: T[Response]) extends Command

  /**
   * Сообщение о том, что запрос выполнить не удалось
   * @param dst получатель сообщения
   */
  case class FailedRequest(dst: T[Response]) extends Command {
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
