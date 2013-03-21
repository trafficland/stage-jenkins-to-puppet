package models.mongo.reactive

import play.api.libs.json._
import reactivemongo.bson.BSONDocument
import services.repository.{Paging, Sorting}
import services.repository.mongo.reactive.MongoSearchCriteria

abstract class BaseCriteriaReader extends Reads[MongoSearchCriteria] {
  def sortAndPage(json: JsValue): (Option[Sorting], Option[Paging]) = {
    val sort = (json \ "sortField").asOpt[String] map { sortField =>
      val sortDirection = (json \ "sortDirection").asOpt[String] map { _.toLowerCase } match {
        case Some("desc") => -1
        case _  => 1
      }
      Sorting(sortField, sortDirection)
    }

    val page = (json \ "currentPage").asOpt[Int] flatMap { currentPage =>
      (json \ "pageSize").asOpt[Int] map { pageSize =>
        Paging((currentPage - 1) * pageSize, pageSize)
      }
    }

    (sort, page)
  }

  def criteria(json: JsValue): BSONDocument

  def reads(json: JsValue) = {
    val (sort, page) = sortAndPage(json)

    val query = (json \ "criteria").asOpt[JsValue] match {
      case Some(js) => criteria(js)
      case None => BSONDocument()
    }

    JsSuccess(MongoSearchCriteria(query, sort, page))
  }
}

