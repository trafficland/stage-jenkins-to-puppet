package services.repository.mongo.reactive

import play.api.libs.iteratee.{Enumerator, Enumeratee}
import reactivemongo.bson._
import handlers.{BSONWriter, BSONReader}
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import reactivemongo.core.commands.Count
import util.IConfigurationProvider
import concurrent.{Future, ExecutionContext}
import models.mongo.reactive.BaseMongoModel
import services.repository._
import scala.Some
import reactivemongo.api.QueryOpts
import reactivemongo.api.QueryBuilder
import reactivemongo.bson.BSONInteger

abstract class MongoBaseRepository[TModel <: BaseMongoModel]
  extends IMongoRepository[TModel]
  with IMongoDbProvider
  with IConfigurationProvider {

  implicit val reader: BSONReader[TModel]
  implicit val writer: BSONWriter[TModel]

  protected def collectionName: String

  protected val collection = db.collection(collectionName)

  protected def projection: Option[BSONDocument] = None

  protected def onCreated(model: TModel)
                         (implicit context: ExecutionContext): Future[Option[TModel]] = {
    get(model.id.get)
  }

  protected def onUpdated(originalModel: TModel, updatedModel: TModel)
                         (implicit context: ExecutionContext): Future[Option[TModel]] = {
    get(updatedModel.id.get)
  }

  protected def onDeleted(model: Option[TModel])(implicit context: ExecutionContext): Future[Boolean] = {
    Future(true)
  }

  def create(entity: TModel)(implicit context: ExecutionContext): Future[Option[TModel]] = {
    entity.id match {
      case Some(id) =>
      case None => entity.id = Some(BSONObjectID.generate)
    }

    for {
      _ <- collection.insert[TModel](entity)
      inserted <- onCreated(entity)
    } yield inserted
  }

  def update(entity: TModel)(implicit context: ExecutionContext): Future[Option[TModel]] = {
    get(entity.id.get) flatMap {
      _ match {
        case Some(originalEntity) =>
          for {
            _ <- collection.update[BSONDocument, TModel](BSONDocument("_id" -> entity.id.get), entity)
            updated <- onUpdated(originalEntity, entity)
          } yield updated

        case None => Future(None)
      }
    }
  }

  def remove(id: BSONObjectID)(implicit context: ExecutionContext): Future[Boolean] = {
    for {
      original <- get(id)
      deleted <- collection.remove[BSONDocument](BSONDocument("_id" -> id)) map {
        _.ok
      }
      successful <- onDeleted(original)
    } yield (deleted && successful)
  }

  def get(id: BSONObjectID)(implicit context: ExecutionContext): Future[Option[TModel]] = {
    collection.find[BSONDocument, TModel](BSONDocument("_id" -> id)).headOption()
  }

  def getAll(implicit context: ExecutionContext): Enumerator[TModel] = {
    collection.find[BSONDocument, TModel](BSONDocument()).enumerate()
  }

  def search(criteria: MongoSearchCriteria)(implicit context: ExecutionContext) = {
    val sortDoc = criteria.sort map {
      sort => BSONDocument(sort.field -> BSONInteger(sort.direction))
    }

    db.command(Count(collectionName, Some(criteria.query))) map {
      count =>
        val enumerator = criteria.page match {
          case Some(paging) =>
            collection.find[TModel](QueryBuilder(Some(criteria.query), sortDoc, projectionDoc = projection), QueryOpts(paging.skip)).enumerate() &> Enumeratee.take(paging.limit)
          case None =>
            collection.find[TModel](QueryBuilder(Some(criteria.query), sortDoc, projectionDoc = projection)).enumerate()
        }
        MongoSearchResults[TModel](count, enumerator).asInstanceOf[ISearchResults[TModel]]

    }
  }
}
