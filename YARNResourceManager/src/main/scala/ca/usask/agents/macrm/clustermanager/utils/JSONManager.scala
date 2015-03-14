package ca.usask.agents.macrm.clustermanager.utils

import ca.usask.agents.macrm.common.records._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
import org.joda.time._

case class InputTaskDescription(INX: Int, DUR: Long, RST: Long, CPU: Int, MEM: Int, PRI: Int, TSC: Int)
case class InputConstraintDescription(INX: Int, N: String, OP: Int, V: Int)
case class InputJobDescription(JI: Long, UI: Int, TS: List[InputTaskDescription], CS: List[InputConstraintDescription])

object JSONManager {

    def getJobDescription(messageBody: String): Either[String, JobDescription] = {
        try {

            implicit val constraintRead: Reads[InputConstraintDescription] = (
                (__ \ "INX").read[Int] and
                (__ \ "N").read[String] and
                (__ \ "OP").read[Int] and
                (__ \ "V").read[Int])(InputConstraintDescription)

            implicit val taskRead: Reads[InputTaskDescription] = (
                (__ \ "INX").read[Int] and
                (__ \ "DUR").read[Long] and
                (__ \ "RST").read[Long] and
                (__ \ "CPU").read[Int] and
                (__ \ "MEM").read[Int] and
                (__ \ "PRI").read[Int] and
                (__ \ "TSC").read[Int])(InputTaskDescription)

            implicit val jobRead: Reads[InputJobDescription] = (
                (__ \ "JI").read[Long] and
                (__ \ "UI").read[Int] and
                (__ \ "TS").read(list[InputTaskDescription](taskRead)) and
                (__ \ "CS").read(list[InputConstraintDescription](constraintRead)))(InputJobDescription)

            val json: JsValue = Json.parse(messageBody)
            val tempResult = json.validate(jobRead)
            tempResult match {
                case JsSuccess(x, _) => Right(InputJobDescriptionToJobDescription(x))
                case JsError(x)      => Left(x.toString())
                case _               => Left(tempResult.toString())
            }
        }
        catch {
            case e: Exception => Left(e.getMessage)
        }
    }

    def InputJobDescriptionToJobDescription(x: InputJobDescription) = {
        val constraints = x.CS.groupBy(y => y.INX)
        val tasks = x.TS.map(y => new TaskDescription(null, x.JI, y.INX, new Duration(y.DUR), new Resource(y.MEM, y.CPU), new Duration(y.RST), constraints.getOrElse(y.INX, List()).map(z => new Constraint(z.N, z.OP, z.V))))
        val jobManagerContainerSpecification = new TaskDescription(null, x.JI, 0, new Duration(0), new Resource(ClusterManagerConfig.jobManagerContainerMemorySize, ClusterManagerConfig.jobManagerContainerVirtualCoreSize), new Duration(0), List())
        new JobDescription(x.JI, x.UI, x.TS.length, jobManagerContainerSpecification :: tasks)
    }
}