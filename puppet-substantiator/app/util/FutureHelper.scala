package util

import scala.concurrent._

object FutureHelper {
  def futureEitherOfOptionExceptionToOption[T]
  (futEitherOptException: Future[Either[Option[T], Exception]])
  (implicit context: ExecutionContext): Future[Option[T]] = {
    for {
      latestLeftRight <- futEitherOptException
    } yield {
      latestLeftRight match {
        case Left(newOptApp) =>
          newOptApp match {
            case Some(t) =>
              Some(t)
            case None =>
              None
          }
        case Right(ex) =>
          None
      }
    }
  }
}
