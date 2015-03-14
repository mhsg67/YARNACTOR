package ca.usask.agents.macrm.common.records

import java.io.Serializable

@SerialVersionUID(100L)
class Container(val containerId: Long,
                val userId: Int,
                val jobId: Long,
                val taskIndex: Int,
                val resource: Resource)
    extends Serializable {

    override def equals(input: Any): Boolean = input match {
        case that: Container => this.containerId == that.containerId &&
            this.userId == that.userId &&
            this.jobId == that.jobId &&
            this.taskIndex == that.taskIndex &&
            this.resource == that.resource
        case _ => false
    }
}