package ca.usask.agents.macrm.common.records

import java.io.Serializable

/**
 * @param:  samplingRate  show the number of sampling per tasks
 * defualt value is 2
 * @param:  clusterNodes  show the cluster structure, each element represent
 * a node, the NodeId is its address, the constraint's list represent the node
 * capability that are required by some of this job's tasks
 */
@SerialVersionUID(100L)
class SamplingInformation(val samplingRate:Int,
                          val clusterNodesWithConstraint: List[(NodeId, List[Constraint])],
                          val clusterNodeWithoutConstraint:List[NodeId])
    extends Serializable 