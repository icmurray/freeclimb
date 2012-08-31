package freeclimb.sql

import java.sql.Connection

import scalaz._
import Scalaz._

/**
 * A DB is an action that when handed a database `Connection` runs the
 * action, return a value of type `A`.
 *
 * It defines map and flatMap; and hence can be used in for expressions to
 * compose DB objects together before running the composed DB.
 *
 * It's really the Reader monad with the first type-parameter fixed to
 * `Connection`.
 */
case class DB[+A](g: Connection => A) {

  /**
   * Run the DB action with the given Connection.
   *
   * The DB does **not** manage the Connection.  If you need to have the
   * Connection managed then try `runWithinTransaction`.
   */
  def run(connection: Connection): A = g(connection)

  /**
   * Run the DB action within a transaction.
   */
  def runWithinTransaction(connection: Connection): A = {
    try{
      connection.setAutoCommit(false)
      val result = run(connection)
      connection.commit()
      result
    } catch {
      case e => connection.rollback() ; throw e
    } finally {
      connection.close()
    }
  }

  def map[B](f: A => B): DB[B] = {
    DB(c => f(g(c)))
  }

  def flatMap[B](f: A => DB[B]): DB[B] = {
    DB(c => f(g(c)) run c )
  }

}

/**
 * This trait defines some implicit conversions for inter-operating with
 * scalaz's type classes.
 */
trait DBInstances {

  /**
   * Ensure that DB is seen as a Monad and a Functor when using scalaz.
   * For example, this is necessary when using it with monad transformers.
   */
  implicit val dbInstance = new Monad[DB] with Functor[DB] {
    override def bind[A, B](fa: DB[A])(f: A => DB[B]): DB[B] = fa flatMap f
    override def point[A](a: => A): DB[A] = DB(_ => a )
  }
}

/**
 * Companion object that captures all the implicits defined above
 */
object DB extends DBInstances
