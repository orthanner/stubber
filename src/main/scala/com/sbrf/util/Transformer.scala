package com.sbrf.util

import akka.http.scaladsl.model.HttpRequest

import cats.implicits._

/**
 * Реализация функции `T => R`. Вспомогательные типы нужны для разбивки процесса на шаги.
 * <ol>
 * <li>Сначала вызывается метод `unwrapRequest`, преобразующий запрос `T` в список элементов `List[S]`</li>
 * <li>Для каждого элемента (1) вызывается функция `S => A`. Вызов дополнительно оборачивается в монаду [[UnsafeValue]]</li>
 * <li>Для каждого элемента полученного списка вызывается функция `F`, являющаяся **композицией** функций `A => B` и `B => C`</li>
 * <li>Наконец, `List[UnsafeValue[C]]` преобразуется в `R`</li>
 * </ol>
 * @tparam T тип запроса
 * @tparam S тип <strong>элемента</strong> запроса
 * @tparam A тип обрабатываемых значений
 * @tparam B тип промежуточных значений
 * @tparam C тип выходных значений
 * @tparam R тип возвращаемого результата
 */
trait Transformer[Q, T, S, A, B, C, R] {

  type Result = R

  val makeAttr: (Q, A) => B
  val makeElement: B => C
  val valueExtractor: S => A

  /**
   * Проверяет элемент выходного списка на пригодность к добавлению в ответ
   * @return булевскую функцию, возвращающуб `true`, если элемент надо включить в ответ, и `false` в противном случае
   */
  def check: UnsafeValue[C] => Boolean = _ => true

  final def apply: Q => T => R = rq => data =>
    render(rq, transformData(valueExtractor, makeAttr, makeElement)(rq)(unwrapRequest(rq, data)) filter check map { _ valueOr recover })

  final def << : Q => T => R = apply

  /**
   * &laquo;Распаковывает&raquo; тело запроса в список (возможно, из одного элемента)
   * @param body тело запроса
   * @return список содержащихся в запросе объектов
   */
  def unwrapRequest(rq: Q, body: T): List[S]

  /**
   * конвертирует результат в ответ
   * @param rq запрос
   * @param queue список объектов, полученных в результате обработки запроса
   * @return ответ
   */
  def render(rq: Q, queue: List[C]): Result

  /**
   * Обрабатывает ошибку
   * @param e ошибка
   * @return ошибка, сконвертированная в объект
   */
  def recover(e: Throwable): C
}
