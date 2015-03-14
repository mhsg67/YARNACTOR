package ca.usask.agents.macrm.common.agents

import ca.usask.agents.macrm.common.records._
import org.joda.time.DateTime
import akka.actor._

/**
 * From ResourceTracker to ClusterManager to inform it about 
 * changes in cluster structure and sampling rates
 */
case class _ClusterState(_source:ActorRef, _time: DateTime, _newSamplingRate:Int = -1, _removedServers:List[NodeId] = null, _addedServers:List[(NodeId,List[Constraint])] = null, _rareResources:List[(Boolean,Constraint)] = null)

/**
 * From ResourceTracker to ClusterManager to inform it about
 * a node with free resources. QueueAgent use this for scheduling JobManager
 * of new submitted job
 */
case class _ServerWithEmptyResources(_source: ActorRef, _time: DateTime, _report: NodeReport)
    extends BasicMessage

/**
 * From ResourceTracker to ClusterManager to inform it about
 * Sharing of cluster among users. It is useful when system is working in
 * centralize way
 *
 * TODO: Add information of user share into the message
 */
case class _EachUserShareOfCluster(_source: ActorRef, _time: DateTime)
    extends BasicMessage

/**
 * From ResourceTracker To ClusterManager to let it know about change in server
 * resource state. It will be used in high load when cluster makes centralize 
 * decision making
 */
case class _ServerStatusUpdate(_source: ActorRef, _time: DateTime, _report: NodeReport)
    extends BasicMessage