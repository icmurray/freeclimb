package org.freeclimbers.api

import org.scalatest.FunSuite

class PageTests extends FunSuite {

  test("linksFor does not construct previous page if viewing first page") {
    val links = firstPageLinks
    assert(links.get("prev") === None)
  }

  test("linksFor links to first page from second page") {
    val links = secondPageLinks
    assert(links.get("prev") === Some(simpleLinker(firstPage)))
  }

  test("linksFor links to penultimate page from last page") {
    val links = lastPageLinks
    assert(links.get("prev") === Some(simpleLinker(penultimatePage)))
  }

  test("linksFor links first page to second page") {
    val links = firstPageLinks
    assert(links.get("next") === Some(simpleLinker(secondPage)))
  }

  test("linksFor links penultimate page to last page") {
    val links = penultimatePageLinks
    assert(links.get("next") === Some(simpleLinker(lastPage)))
  }

  test("linksFor does not construct next page link if on the last page") {
    val links = lastPageLinks
    assert(links.get("next") === None)
  }

  test("linksFor constructs a self link") {
    val links = lastPageLinks
    assert(links.get("self") === Some(simpleLinker(lastPage)))
  }

  test("linksFor with an empty page") {
    val links = Page.linksFor(0, PageLimits(limit = 10, offset = 0), simpleLinker)
    assert(links.get("prev") === None)
  }

  private val simpleLinker = new PageLinker ( paging => s"limit:${paging.limit} offset:${paging.offset}" )

  private val count = 102
  private val firstPage       = PageLimits(limit = 10, offset = 0)
  private val secondPage      = PageLimits(limit = 10, offset = 10)
  private val penultimatePage = PageLimits(limit = 10, offset = 90)
  private val lastPage        = PageLimits(limit = 10, offset = 100)

  private val firstPageLinks       = Page.linksFor(count, firstPage,       simpleLinker)
  private val secondPageLinks      = Page.linksFor(count, secondPage,      simpleLinker)
  private val penultimatePageLinks = Page.linksFor(count, penultimatePage, simpleLinker)
  private val lastPageLinks        = Page.linksFor(count, lastPage,        simpleLinker)

}
