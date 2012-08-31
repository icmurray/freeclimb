package freeclimb.api

import freeclimb.models._

/**
 * Defines search-related api actions
 */
trait SearchApi {

  def findClimb(params: SearchParams): ApiAction[SearchResult[Revisioned[Climb]]]

}

/**
 * There's a fixed set of possible search parameters.
 *
 * TODO: ordering
 * TODO: faceting
 */
case class SearchParams(
    q: Option[String],
    lowerGrade: Option[Grade],
    upperGrade: Option[Grade],
    geo: Option[GeoSearchParams])

/**
 * The search results returns a subset of all the found items.
 *
 * TODO: faceting
 */
trait SearchResult[A] {

  /** The total number of hits. */
  val totalFound: Int

  /** The page (0-indexed) of the returned results. */
  val page: Int

  /** The results on this page */
  val results: Seq[A]

}

/**
 * Possible geo-enabled search parameters.
 */
sealed trait GeoSearchParams

/** Within a given circle */
case class GeoDistance(
    val center: LatLon,
    val distance: Double)
  extends GeoSearchParams

/** Within a bounding box */
case class BoundingBox(
    val topLeft: LatLon,
    val bottomRight: LatLon)
  extends GeoSearchParams

final case class LatLon(
    val lat: Double,
    val lon: Double)
