package freeclimb.api

case class SessionReader[A](g: ApiSession => A) {
  def apply(s: ApiSession) = g(s)

  def map[B](f: A => B): SessionReader[B] = { s: ApiSession => f(g(s)) }
  def flatMap[B](f: A => SessionReader[B]): SessionReader[B] = {
    s: ApiSession => f(g(s))(s)
  }

  private implicit def function2SessionReader[T](f: Function[ApiSession, T]): SessionReader[T] = SessionReader[T](f)

}
