package freeclimb

import scalaz._
import Scalaz._

package object validation {

  type Disj[A] = \/[Map[String,NonEmptyList[String]], A]

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
    override def enrichAs(key: String) = v.swap map { es => Map(key -> es) } swap
  }

  /**
   * Implicit conversion of Validation to RichValidation
   */
  implicit def validation2Rich[E, A](v: Validation[E, A]): RichValidation[E, A] = new RichValidation[E, A] {
    override def enrichAs(key: String) = v.swap map { e => Map(key -> NonEmptyList(e)) } swap
  }

  /**
   * Some common validation functions
   */
  def nonEmpty(implicit s: String) = s match {
    case ""                => Failure("String is empty")
    case s if s.trim == "" => Failure("String is empty")
    case _                 => Success(s)
  }

  def maxLength(limit: Int)(implicit s: String) = s match {
    case s if s.length > limit => Failure("String cannot exceed %d in length"
                                          .format(limit))
    case _                     => Success(s)
  }

  val lowerAlpha    = Set('a' to 'z': _*)
  val upperAlpha    = Set('A' to 'Z': _*)
  val numerAlpha    = Set('0' to '9': _*)
  val nonAlphaChars = Set('-', '_')
  val slugChars     = nonAlphaChars ++ lowerAlpha ++ upperAlpha ++ numerAlpha

  def onlyContains(chars: Set[Char])(implicit s: String) = s.toSet match {
    case set if set subsetOf chars => Success(s)
    case _                         => Failure("String must contain only characters in [%s]"
                                              .format(chars.toList.sorted))
  }

}
