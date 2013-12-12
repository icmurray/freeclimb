package org.freeclimbers
package core

/**
 * For functions which merge two items, these keep it clear which instance is
 * being kept and which is being removed.
 */
case class Keep[T](v: T) extends AnyVal
case class Remove[T](v: T) extends AnyVal

