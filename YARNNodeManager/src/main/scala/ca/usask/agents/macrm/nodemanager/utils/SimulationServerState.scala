package ca.usask.agents.macrm.nodemanager.utils

import akka.actor._
import ca.usask.agents.macrm.common.records._
import org.joda.time.DateTime

/**
 * This class is used for simulation in which there is no real server
 */
class SimulationServerState extends ServerState {

    var nextContainerId = 0
    var serverResource: Resource = new Resource(0,0)
    var serverCapabilities: List[Constraint] = List()
    var serverContainers: List[Container] = List()
    var serverNodeState = NodeState("RUNNING")

    def initializeServer(): Boolean = true

    def initializeSimulationServer(resource: Resource, capability: List[Constraint]) = {
        serverResource = resource        
        serverCapabilities = capability
        true
    }

    def getServerStatus(nodeManager: ActorRef) =
        new NodeReport(new NodeId(agent = nodeManager), serverResource, serverCapabilities,
            serverContainers, serverUtilization, serverNodeState, 0)

    def serverUtilization(): Utilization = new Utilization(0.0, 0.0)

    def getServerFreeResources = serverResource - serverContainers.foldLeft(new Resource(0, 0))((x, y) => y.resource + x)

    def getServerResource() = serverResource

    def createContainer(userId: Int, jobId: Long, taskIndex: Int, size: Resource): Option[Long] = {
        if ((serverResource - serverContainers.foldLeft(new Resource(0, 0))((x, y) => y.resource + x)) < size)
            None
        else {            
            serverContainers = new Container(nextContainerId, userId, jobId, taskIndex, size) :: serverContainers
            nextContainerId += 1
            Some(nextContainerId - 1)
        }
    }

    def killContainer(containerId: Long): Option[Int] = {
        if (serverContainers.exists(x => x.containerId == containerId)) {
            val (finishedContainer, runningContainers) = serverContainers.span (x => x.containerId == containerId)
                    serverContainers = runningContainers
            Some(finishedContainer(0).taskIndex)
        }
        else None
    }

}