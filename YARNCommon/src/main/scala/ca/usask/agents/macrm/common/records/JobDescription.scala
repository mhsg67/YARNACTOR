package ca.usask.agents.macrm.common.records

import ca.usask.agents.macrm.common.records._
import org.joda.time.DateTime
import java.io.Serializable
import scala.collection.mutable._

@SerialVersionUID(100L)
case class JobDescription(val jobId: Long,
                          val userId: Int,
                          val numberOfTasks: Int,
                          var tasks: List[TaskDescription])
    extends Serializable {

    override def toString() = "<jobId:" + jobId.toString() + " userId:" + userId.toString() + " #tasks:" + numberOfTasks.toString() +
        "\ntasks:[" + tasks.foreach(x => x.toString() + "\n") + "]\nconstraints:[" + constraints.foreach(x => x.toString() + "\n") + "]>"

    def constraints(): List[Constraint] = tasks.flatMap(x => x.constraints)
}