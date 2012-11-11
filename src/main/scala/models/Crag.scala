package freeclimb.models

import freeclimb.validation._

import scalaz._
import Scalaz._

/**
 * The Crag model.
 *
 * Note that it does not have an Id field.  An Id is an implementation
 * detail, as a Crag is uniquely identified by its name and containing
 * Crag
 *
 * @param name identifies the Crag within the system.
 *
 * @param title is a human readable form of the name.
 *
 */
class Crag private (
    val name: String,
    val title: String) {

  override val hashCode = name.hashCode + 41 * (title.hashCode + 41)

  override def equals(that: Any) = that match {
    case c: Crag => {
      c.asInstanceOf[Crag].name        == this.name        &&
      c.asInstanceOf[Crag].title       == this.title
    }
    case _        => false
  }

  override val toString = "Crag(" + name + ")"
}

object Crag {

  /** Validated Crag constructor.
   *
   * Validates the construction of a new Crag instance.
   *
   * @return RichValidation[String, Crag] where in the error case, each key
   *         corresponds to a field on the Crag model.
   *
   * For example:
   *
   * <code>
   *      import models.{Crag, Crag}
   *      val crag = Crag("foo", "bar", "baz")
   *
   *      val validCrag = Crag("name", "title", "desc", crag, List(), None, Set())
   *      println(validCrag)
   *      Success(CragImpl(name,title,desc,Crag(foo,bar,baz),List(),None,Set()))
   *
   *      val invalidCrag = Crag("name", "", "desc", crag, List(), None, Set())
   *      println(invalidCrag)
   *      Failure(Map(title -> NonEmptyList(String is empty)))
   * </code>
   */
  def apply(name: String,
            title: String) = (

    validateName(name)               .enrichAs("name")        |@|
    validateTitle(title)             .enrichAs("title")

  ) { case _ => new Crag (name, title): Crag }.disjunction

  def makeUnsafe(
      name: String,
      title: String) = {
    Crag(name, title).fold (
      error => throw new RuntimeException("Invalid Crag: "+name),
      climb => climb
    )
  }

  /**
   * Validation functions for each field of Crag
   */
  
  private def validateName(implicit name: String) = {
    nonEmpty                .toValidationNEL <*
    maxLength(20)           .toValidationNEL <*
    onlyContains(slugChars) .toValidationNEL
  }

  private def validateTitle(implicit title: String) = {
    nonEmpty      .toValidationNEL <*
    maxLength(30) .toValidationNEL
  }

}
