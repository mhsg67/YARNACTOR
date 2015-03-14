package ca.usask.agents.macrm.clustermanager.utils

import ca.usask.agents.macrm.common.records._

class ClusterDatastructure {

    var samplingRate = 2
    var clusterNodes = List[(NodeId, List[Constraint])]()
    var rareResource = List[Constraint]()

    def updateClusterStructure(newSamplingRate: Int = -1, removedServers: List[NodeId] = null, addedServers: List[(NodeId, List[Constraint])] = null, rareResourcesUpdate: List[(Boolean, Constraint)] = null) = {
        if (newSamplingRate != -1) samplingRate = newSamplingRate
        if (removedServers != null) clusterNodes = clusterNodes.filterNot(x => removedServers.exists(y => y == x._1))
        if (addedServers != null) {
            clusterNodes = clusterNodes.filterNot(x => addedServers.exists(y => y._1 == x._1))
            clusterNodes ++= addedServers
            println("ClusterNodes " + clusterNodes.length)
        }
        if (rareResourcesUpdate != null) rareResourcesUpdate.foreach(x => addOrDropRareResources(x._1, x._2))
    }

    def addOrDropRareResources(add: Boolean, constraint: Constraint) = {
        if (add) rareResource = constraint :: rareResource
        else rareResource = rareResource.filterNot(x => x.name == constraint.name && x.value == constraint.value)
    }

    def getCurrentSamplingInformation(constraints: List[Constraint]): SamplingInformation = {
        val (nodeWithConstraints, nodeWithoutConstraints) =
            clusterNodes.map(x => (x._1, getMatchCapablitiesOfNode(x._2, constraints))).filterNot(x => x._2 == List()).partition(x => x._2 != null)
        new SamplingInformation(samplingRate, nodeWithConstraints, nodeWithoutConstraints.map(x => x._1))
    }

    def getMatchCapablitiesOfNode(nodeCapabilities: List[Constraint], jobConstraints: List[Constraint]) = {
        val result = nodeCapabilities.filter(x => doesCapabilityMatchConstraints(x, jobConstraints))
        result match {
            case List() => if (doesHaveRareResources(nodeCapabilities)) List() else null
            case _      => result
        }
    }

    def doesHaveRareResources(nodeCapabilities: List[Constraint]): Boolean =
        rareResource.foldRight(false)((x, y) => doesCapabilityMatchConstraints(x, nodeCapabilities) || y)

    def doesCapabilityMatchConstraints(capability: Constraint, jobConstraints: List[Constraint]) = {
        val temp = jobConstraints.filter(x => x.name == capability.name)
        temp match {
            case List() => false
            case _      => temp.foldLeft(true)((x, y) => x && doseCapabilityMatchConstraint(capability, y))
        }
    }

    def doseCapabilityMatchConstraint(capability: Constraint, constraint: Constraint) = constraint.operator match {
        case 0 => if (capability.value == constraint.value) true else false
        case 1 => if (capability.value != constraint.value) true else false
        case 2 => if (capability.value < constraint.value) true else false
        case 3 => if (capability.value > constraint.value) true else false
    }

}