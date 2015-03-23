package ca.usask.agents.yarnactor.resourcemanager.utils

import ca.usask.agents.yarnactor.common.records._
import scala.collection.mutable._
import org.joda.time._

case class nodeIdToNodeStateRow(var totalResource: Resource,
                                var usedResources: Resource,
                                var utilization: Utilization,
                                var queueState: Int,
                                var lastReportTime: DateTime)

object ClusterDatabase {

    var nodeIdToNodeStateTable = Map[NodeId, nodeIdToNodeStateRow]()

    var nodeIdToContainerTable = Map[NodeId, List[Container]]()

    var userIdToUserShareTable = Map[Int, Resource]()

    var capabilityToNodeIdTable = Map[Constraint, List[NodeId]]()

    def updateNodeState(nodeId: NodeId, totalResource: Resource, usedResources: Resource, capabilities: List[Constraint], utilization: Utilization, queueState: Int) = {
        nodeIdToNodeStateTable.update(nodeId, new nodeIdToNodeStateRow(totalResource, usedResources, utilization, queueState, DateTime.now()))
        capabilities.foreach(x => updateCapabilityTable(x, nodeId))
    }

    //TODO: test this
    def updateCapabilityTable(capability: Constraint, nodeId: NodeId) = capabilityToNodeIdTable.get(capability) match {
        case None    => capabilityToNodeIdTable.update(capability, List(nodeId))
        case Some(x) => if (!x.contains(nodeId)) capabilityToNodeIdTable.update(capability, nodeId :: x)
    }

    def updateNodeContainer(nodeId: NodeId, containers: List[Container]) = nodeIdToContainerTable.get(nodeId) match {
        case None => {
            containers.foreach(x => increaseUserShare(x.userId, x.resource))
            nodeIdToContainerTable.update(nodeId, containers)
        }
        case Some(x) => {
            containers.diff(x).foreach(x => increaseUserShare(x.userId, x.resource))
            x.diff(containers).foreach(x => reduceUserShare(x.userId, x.resource))
            nodeIdToContainerTable.update(nodeId, containers)
        }
    }

    def reduceUserShare(userId: Int, resource: Resource) = userIdToUserShareTable.get(userId) match {
        case None    => Logger.Error("Reducing nonexistence user share")
        case Some(x) => userIdToUserShareTable.update(userId, x - resource)
    }

    def increaseUserShare(userId: Int, resource: Resource) = userIdToUserShareTable.get(userId) match {
        case None    => userIdToUserShareTable.update(userId, resource)
        case Some(x) => userIdToUserShareTable.update(userId, x + resource)
    }

    //TODO: test this
    def getNodeIdToContaintsMaping(): List[(NodeId, List[Constraint])] = {
        val result = nodeIdToNodeStateTable.map(x => (x._1, List[Constraint]()))
        capabilityToNodeIdTable.foreach(x => x._2.foreach(y => result.update(y, x._1 :: result.get(y).get)))
        result.toList
    }

    def getCurrentClusterLoad(): Utilization = {
        val (totalResource, usedResource) = nodeIdToNodeStateTable.toList.foldLeft((new Resource(0, 0), new Resource(0, 0)))((x, y) => (x._1 + y._2.totalResource, x._2 + y._2.usedResources))
        new Utilization(usedResource.memory.toDouble / totalResource.memory.toDouble, usedResource.virtualCore.toDouble / totalResource.virtualCore.toDouble)        
    }
}