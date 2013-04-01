package freeclimb.models

import scalaz._
import Scalaz._

import freeclimb.validation._

/**
 * Grade trait.
 *
 * For purposes of representing Grades in the database, each Grade must
 * define the GradingSystem within which it is a member; and the difficulty,
 * or rank of this Grade within that GradingSystem.
 */
sealed trait Grade {

  import Grade._
  import GradingSystem._

  def system: GradingSystem
  def difficulty: Int
}

case class EuSport (val grade: Grade.EuSport.EuSport) extends Grade {
  override val toString = grade.toString
  override val system = Grade.GradingSystem.EuSport
  override val difficulty = grade.id
}

object EuSport extends FromDifficultyString {
  val enum = Grade.EuSport
}

case class UkTechnical (val grade: Grade.UkTechnical.UkTechnical) extends Grade {
  override val toString = grade.toString
  override val system = Grade.GradingSystem.UkTechnical
  override val difficulty = grade.id
}

object UkTechnical extends FromDifficultyString {
  val enum = Grade.UkTechnical
}

case class UkAdjective (val grade: Grade.UkAdjective.UkAdjective) extends Grade {
  override val toString = grade.toString
  override val system = Grade.GradingSystem.UkAdjective
  override val difficulty = grade.id
}

object UkAdjective extends FromDifficultyString {
  val enum = Grade.UkAdjective
}

case class UkTrad (
    val adjGrade: Grade.UkAdjective.UkAdjective,
    val techGrade: Grade.UkTechnical.UkTechnical) extends Grade {

  override val toString = adjGrade.toString + " " + techGrade.toString
  override val system = Grade.GradingSystem.UkTrad
  override val difficulty = (adjGrade.id - 1) * UkTrad.MAX_TECH_GRADE + techGrade.id

}

object UkTrad {
  
  val MAX_TECH_GRADE = Grade.UkTechnical.MAX_DIFFICULTY

  def apply(difficulty: Int): UkTrad = {
    val adjGrade = Grade.UkAdjective(difficulty / MAX_TECH_GRADE + 1)
    val techGrade = Grade.UkTechnical(difficulty % MAX_TECH_GRADE)
    new UkTrad(adjGrade, techGrade)
  }

  def apply(difficulty: String): \/[String,UkTrad] = {
    difficulty.split(" ").toList match {
      case List(adjS, techS) =>
        (UkAdjective(adjS).validation  |@|
         UkTechnical(techS).validation
        ) { case (adj, tech) => UkTrad(adj,tech): UkTrad }.disjunction
      case _                 => ("Unknown UkTrad grade: " + difficulty).left

    }
  }

}

trait FromDifficultyString {
  val enum: Grade.GradeEnumeration

  def apply(difficulty: String): \/[String,Grade] = {
    enum.values.find { _.toString == difficulty }.
                toSuccess("Unknown " + enum.toString + " grade: " + difficulty).
                disjunction
  }
}

object Grade {

  trait GradeEnumeration extends Enumeration

  def apply(system: String, difficulty: String): Disj[Grade] = {
    GradingSystem.values.find { _.toString == system }.
                         toSuccess("Unknown grading system: " + system).
                         enrichAs("system").
                         disjunction >>= { Grade(_, difficulty) }
  }

  //def apply(system: String, difficulty: Int): Disj[Grade] = {
  //  Grade(GradingSystem.withName(system), difficulty)
  //}

  def apply(system: GradingSystem.GradingSystem, difficulty: Int): Grade = system match {
    case GradingSystem.EuSport => new EuSport(Grade.EuSport(difficulty))
    case GradingSystem.UkTechnical => new UkTechnical(Grade.UkTechnical(difficulty))
    case GradingSystem.UkAdjective => new UkAdjective(Grade.UkAdjective(difficulty))
    case GradingSystem.UkTrad => UkTrad(difficulty)
  }

  def apply(system: GradingSystem.GradingSystem, difficulty: String): Disj[Grade] = system match {
    case GradingSystem.EuSport     => EuSport(difficulty)
    case GradingSystem.UkTechnical => UkTechnical(difficulty)
    case GradingSystem.UkAdjective => UkAdjective(difficulty)
    case GradingSystem.UkTrad      => UkTrad(difficulty).validation.enrichAs("grade").disjunction
  }

  /**
   * The possible grading systems.
   *
   * These correspond to the `GradingSystem` enumeration defined on the
   * database.
   */
  object GradingSystem extends Enumeration {
    type GradingSystem = Value
    val EuSport = Value("EuSport")
    val UkAdjective = Value("UkAdjective")
    val UkTechnical = Value("UkTechnical")
    val UkTrad = Value("UkTrad")
    val Font = Value("Font")
  }

  object EuSport extends GradeEnumeration {
    type EuSport = Value
    val Eu1   = Value(1,  "F1")
    val Eu2   = Value(2,  "F2")
    val Eu3   = Value(3,  "F3")
    val Eu4a  = Value(4,  "F4a")
    val Eu4b  = Value(5,  "F4b")
    val Eu4c  = Value(6,  "F4c")
    val Eu5a  = Value(7,  "F5a")
    val Eu5b  = Value(8,  "F5b")
    val Eu5c  = Value(9,  "F5c")
    val Eu6a  = Value(10, "F6a")
    val Eu6aP = Value(11, "F6a+")
    val Eu6b  = Value(12, "F6b")
    val Eu6bP = Value(13, "F6b+")
    val Eu6c  = Value(14, "F6c")
    val Eu6cP = Value(15, "F6c+")
    val Eu7a  = Value(16, "F7a")
    val Eu7aP = Value(17, "F7a+")
    val Eu7b  = Value(18, "F7b")
    val Eu7bP = Value(19, "F7b+")
    val Eu7c  = Value(20, "F7c")
    val Eu7cP = Value(21, "F7c+")
    val Eu8a  = Value(22, "F8a")
    val Eu8aP = Value(23, "F8a+")
    val Eu8b  = Value(24, "F8b")
    val Eu8bP = Value(25, "F8b+")
    val Eu8c  = Value(26, "F8c")
    val Eu8cP = Value(27, "F8c+")
    val Eu9a  = Value(28, "F9a")
    val Eu9aP = Value(29, "F9a+")
    val Eu9b  = Value(30, "F9b")
    val Eu9bP = Value(31, "F9b+")
    val Eu9c  = Value(32, "F9c")
    val Eu9cP = Value(33, "F9c+")
  }

  object UkTechnical extends GradeEnumeration {
    type UkTechnical = Value
    val T1a = Value(1,  "1")
    val T2a = Value(2,  "2")
    val T3a = Value(3,  "3")
    val T4a = Value(4,  "4a")
    val T4b = Value(5,  "4b")
    val T4c = Value(6,  "4c")
    val T5a = Value(7,  "5a")
    val T5b = Value(8,  "5b")
    val T5c = Value(9,  "5c")
    val T6a = Value(10, "6a")
    val T6b = Value(11, "6b")
    val T6c = Value(12, "6c")
    val T7a = Value(13, "7a")
    val T7b = Value(14, "7b")
    val T7c = Value(15, "7c")
    val T8a = Value(16, "8a")
    val T8b = Value(17, "8b")
    val T8c = Value(18, "8c")

    val MAX_DIFFICULTY = 18
  }
  object UkAdjective extends GradeEnumeration {
    type UkAdjective = Value
    val Easy = Value(1,  "Easy")
    val Mod  = Value(2,  "Mod")
    val Diff = Value(3,  "Diff")
    val HD   = Value(4,  "HD")
    val VD   = Value(5,  "VD")
    val HVD  = Value(6,  "HVD")
    val MS   = Value(7,  "MS")
    val S    = Value(8,  "S")
    val HS   = Value(9,  "HS")
    val MVS  = Value(10, "MVS")
    val VS   = Value(11, "VS")
    val HVS  = Value(12, "HVS")
    val E1   = Value(13, "E1")
    val E2   = Value(14, "E2")
    val E3   = Value(15, "E3")
    val E4   = Value(16, "E4")
    val E5   = Value(17, "E5")
    val E6   = Value(18, "E6")
    val E7   = Value(19, "E7")
    val E8   = Value(20, "E8")
    val E9   = Value(21, "E9")
    val E10  = Value(22, "E10")
    val E11  = Value(23, "E11")
    val E12  = Value(24, "E12")
    val E13  = Value(25, "E13")
    val E14  = Value(26, "E14")
    val E15  = Value(27, "E15")
  }
}
