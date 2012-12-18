package freeclimb.restApi

import freeclimb.models.Grade

/**
 * Although this may look like a duplication of code, I think it will be
 * quite useful to have a separation of rest resources from domain models.
 */

/**
 * Note how a CragResource has no name.  This is because the name is provided
 * in the URL.
 */
case class CragResource(title: String)

/**
 * Note how a RevisionedCragResource has no name.  This is because the name is
 * provided in the URL.
 */
case class RevisionedCragResource(
    title: String,
    revision: Long)

case class ClimbResource(
    title: String,
    description: String,
    grade: Grade)
