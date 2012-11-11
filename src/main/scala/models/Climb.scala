package freeclimb.models

import freeclimb.validation._

import scalaz._
import Scalaz._

/**
 * The Climb model.
 *
 * Note that it does not have an Id field.  An Id is an implementation
 * detail, as a Climb is uniquely identified by its name and containing
 * Crag
 *
 * @param name identifies the Climb within the specified Crag.
 *
 * @param title is a human readable form of the name.
 *
 * @param description is a markdown formatted description of the Climb.
 *
 * @param crag is the Crag that this Climb belongs to.
 *
 * @param grade is the Grade of the Climb.  This includes information about
 *        the *type* of grade.
 *
 */
class Climb private (
    val name: String,
    val title: String,
    val description: String,
    val crag: Crag,
    val grade: Grade) {

  override val hashCode = {
    name.hashCode + 41 * (
    title.hashCode + 41 * (
    description.hashCode + 41 * (
    crag.hashCode + 41 * (
    grade.hashCode
    ))))
  }

  override def equals(that: Any) = that match {
    case c: Climb => {
      c.asInstanceOf[Climb].name        == this.name        &&
      c.asInstanceOf[Climb].title       == this.title       &&
      c.asInstanceOf[Climb].description == this.description &&
      c.asInstanceOf[Climb].crag        == this.crag        &&
      c.asInstanceOf[Climb].grade       == this.grade
    }
    case _        => false
  }

  override val toString = "Climb(" + name + ", " + crag + ")"
}

object Climb {

  /** Validated Climb constructor.
   *
   * Validates the construction of a new Climb instance.
   *
   * @return RichValidation[String, Climb] where in the error case, each key
   *         corresponds to a field on the Climb model.
   *
   * For example:
   *
   * <code>
   *      import models.{Crag, Climb}
   *      val crag = Crag("foo", "bar", "baz")
   *
   *      val validClimb = Climb("name", "title", "desc", crag, List(), None, Set())
   *      println(validClimb)
   *      Success(ClimbImpl(name,title,desc,Crag(foo,bar,baz),List(),None,Set()))
   *
   *      val invalidClimb = Climb("name", "", "desc", crag, List(), None, Set())
   *      println(invalidClimb)
   *      Failure(Map(title -> NonEmptyList(String is empty)))
   * </code>
   */
  def apply(name: String,
            title: String,
            description: String,
            crag: Crag,
            grade: Grade) = (

    validateName(name)               .enrichAs("name")        |@|
    validateTitle(title)             .enrichAs("title")       |@|
    validateDescription(description) .enrichAs("description")

  ) { case _ => new Climb ( name, title, description, crag, grade): Climb }.disjunction

  def makeUnsafe(
      name: String,
      title: String,
      description: String,
      crag: Crag,
      grade: Grade) = {
    Climb(name, title, description, crag, grade).fold (
      error => throw new RuntimeException("Invalid Climb: "+name),
      climb => climb
    )
  }

  /**
   * Validation functions for each field of Climb
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

  private def validateDescription(implicit desc: String): Validation[String, String] = nonEmpty

}

