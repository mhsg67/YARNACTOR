package ca.usask.agents.macrm.common.agents

import ca.usask.agents.macrm.common.records._
import org.joda.time.DateTime
import akka.actor._

/**
 * From clusterManager(either queueAgent or SchedulingAgent) For allocating a
 * container for executing a JobManager for a submitted job
 *
 * From clusterManager(either queueAgent or SchedulingAgent) For allocating a
 * container for executing a tasks for submitted Task
 */
case class _AllocateContainerFromCM(_source: ActorRef, _time: DateTime, _taskDescriptions: List[TaskDescription], _jobDescriptions: List[(JobDescription, SamplingInformation)])
    extends BasicMessage