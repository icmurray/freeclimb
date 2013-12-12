package org.freeclimbers
package core

import org.mindrot.jbcrypt.BCrypt

object Password {

  def hash(password: PlainText): Digest = {
    Digest(BCrypt.hashpw(password.s, BCrypt.gensalt()))
  }

  def check(candidate: PlainText, digest: Digest): Boolean = {
    BCrypt.checkpw(candidate.s, digest.s)
  }

}

