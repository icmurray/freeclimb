package freeclimb.search

import com.github.seratch.scalikesolr._

import freeclimb.models._

trait SearchIndex {

  def indexClimb(climb: Revisioned[Climb]) {
    index(climbToDoc(climb))
  }

  def indexCrag(crag: Revisioned[Crag]) {
    index(cragToDoc(crag))
  }

  private def index(doc: SolrDocument) {
    val client = createClient()
    val request = new UpdateRequest(documents = List(doc))
    client.doUpdateDocuments(request)
  }

  protected def climbToDoc(climb: Revisioned[Climb]) = SolrDocument(
    writerType = WriterType.JSON,
    map = Map(
      "key" -> ("climb:"+climb.model.crag.name+"/"+climb.model.name),
      "revision" -> climb.revision,
      "document_type" -> "climb",
      "name" -> climb.model.name,
      "title" -> climb.model.title,
      "description" -> climb.model.description,
      "crag__name" -> climb.model.crag.name,
      "grade__system" -> climb.model.grade.system.id,
      "grade__difficulty" -> climb.model.grade.difficulty
  ))

  protected def cragToDoc(crag: Revisioned[Crag]) = SolrDocument(
    writerType = WriterType.JSON,
    map = Map(
      "key" -> ("crag:"+crag.model.name),
      "revision" -> crag.revision,
      "document_type" -> "crag",
      "name" -> crag.model.name,
      "title" -> crag.model.title
  ))

  protected def createClient(): SolrClient

  implicit private def stringToSolrDocumentValue(s: String): SolrDocumentValue = {
    SolrDocumentValue(s)
  }

  implicit private def intToSolrDocumentValue(i: Int): SolrDocumentValue = {
    SolrDocumentValue(i.toString)
  }

  implicit private def longToSolrDocumentValue(l: Long): SolrDocumentValue = {
    SolrDocumentValue(l.toString)
  }
}
