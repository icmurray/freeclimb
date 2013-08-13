package org.freeclimbers.core.util

import scala.language.implicitConversions

/**
 * Provides additional methods to the standard scala Map
 */
trait RichMap[K,V] {

  /**
   * Haskell's `adjust` function.
   *
   * Modification of element by application of `f`
   * to original element.
   */
  def adjust(k: K)(f: V => V): Map[K,V]
}

object RichMap {
  implicit def map2RichMap[K,V](m: Map[K,V]) = new RichMap[K,V] {
    def adjust(k: K)(f: V => V) = {
      m.updated(k, f(m(k)))
    }
  }
}
