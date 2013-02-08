package services.evaluations

import util.evaluations._
import models.mongo.reactive._
import play.api.mvc.Results._
import controllers._

  case class AppEvaluate(app: App) extends IEvaluate[App] {
    def evaluate: IEvaluated[App] = {
      if (app.actualCluster.forall(_.actual == app.expected))
        Pass(app)
      else {
        Fail(app)
      }
    }

    def failAction(result: App) = Redirect(routes.ScriptController.rollBack(result.name))


    def passAction(result: App) {
      //send email on pass?
    }

    def name = app.name

}
