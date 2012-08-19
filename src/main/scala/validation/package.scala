package freeclimb

import scalaz._
import Scalaz._

package object validation {

  /** RichValidation trait
    *
    * Provides richer error information, in the form of a map from Strings to
    * NonEmptyLists of errors.
    *
    * Call `enrichAs("key")` on an ordinary `Validation` to lift it to a
    * RichValidation.  The two implicit conversions below take care of
    * transforming from the Valdiation forms defined in scalaz, to a
    * RichValidation.
    */
  trait RichValidation[E, A] {
    def enrichAs(key: String): Validation[Map[String, NonEmptyList[E]], A]
  }

  /**
   * Implicit conversion of ValidationNEL to RichValidation
   */
  implicit def validationNel2Rich[E, A](v: ValidationNEL[E, A]): RichValidation[E, A] = new RichValidation[E, A] {
    override def enrichAs(key: String) = v.fail map { es => Map(key -> es) } validation
  }

  /**
   * Implicit conversion of Validation to RichValidation
   */
  implicit def validation2Rich[E, A](v: Validation[E, A]): RichValidation[E, A] = new RichValidation[E, A] {
    override def enrichAs(key: String) = v.fail map { e => Map(key -> NonEmptyList(e)) } validation
  }

  /**
   * Some common validation functions
   */
  def nonEmpty(implicit s: String) = s match {
    case ""                => "String is empty".fail
    case s if s.trim == "" => "String is empty".fail
    case _                 => s.success
  }

  def maxLength(limit: Int)(implicit s: String) = s match {
    case s if s.length > limit => "String cannot exceed %d in length"
                                    .format(limit).fail
    case _                     => s.success
  }

  val lowerAlpha = Set('a' to 'z': _*)
  val upperAlpha = Set('A' to 'Z': _*)
  val numerAlpha = Set('0' to '9': _*)

  def onlyContains(chars: Set[Char])(implicit s: String) = s.toSet match {
    case set if set subsetOf chars => s.success
    case _                         => "String must contain only characters in [%s]"
                                        .format(chars.toList.sorted)
                                        .fail
  }

}
