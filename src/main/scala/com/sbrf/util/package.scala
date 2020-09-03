package com.sbrf

import cats._
import cats.implicits._

package object util {

  type UnsafeValue[A] = Either[Throwable, A]

  /**
   * Преобразует предварительно разобранный запрос в ответ.
   * @param valueExtractor функция извлечения значений из элементов списка–запроса
   * @param makeAttr функция преобразования извлечённого значения в буферную величину
   * @param makeElement функция преобразования промежуточного значения в выходное
   * @tparam R тип запроса
   * @tparam X тип содержащихся в запросе элементов
   * @tparam A тип значений, извлекаемых из элементов запроса
   * @tparam B тип промежуточных значений
   * @tparam C тип выходных значений
   * @return функцию, преобразующую запрос и его тело в список выходных элементов
   */
  def transformData[R, X, A, B, C](valueExtractor: X => A, makeAttr: (R, A) => Option[B], makeElement: B => C): R => List[X] => List[UnsafeValue[C]] = {
    import cats.instances.either._

    val safeExtractor: X => UnsafeValue[A] = x => MonadError[UnsafeValue, Throwable].catchNonFatal {valueExtractor(x)}

    val transformElement: R => A => Option[B] = rq => makeAttr(rq, _)

    val filter: List[UnsafeValue[Option[C]]] => List[UnsafeValue[C]] =
      _ filter {v => v.isLeft || v.exists(_.isDefined)} map {_ map {_ get}}

    rq: R => transformList(safeExtractor, makeElement, transformElement) (rq) andThen filter
  }

  private def transformList[R, X, A, B, C](safeExtractor: X => UnsafeValue[A], makeElement: B => C, transformElement: R => A => Option[B]): R => List[X] => List[UnsafeValue[Option[C]]] =
    rq => Functor[List].lift(safeExtractor fmap Functor[UnsafeValue].lift(transformElement(rq) fmap Functor[Option].lift(makeElement)))

}
