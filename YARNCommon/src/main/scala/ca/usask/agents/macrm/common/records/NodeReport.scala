package ca.usask.agents.macrm.common.records

import org.joda.time.DateTime
import java.io.Serializable

@SerialVersionUID(100L)
class NodeReport(val nodeId: NodeId,
                  val resource: Resource,
                  val capabilities: List[Constraint],
                  val containers: List[Container],
                  val utilization: Utilization,
                  val nodeState: NodeState,
                  val queueState: Int)
    extends Serializable {

    def getFreeResources() = resource - containers.foldLeft(new Resource(0,0))((x,y) => y.resource + x)
    
    override def toString = "<nodeId:" + nodeId.toString() + " resource:" + resource.toString() + ">"
}
