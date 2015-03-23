package ca.usask.agents.yarnactor.common.agents

import ca.usask.agents.yarnactor.common.records._
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

/**
 * From NodeManager to JobManager to inform it that the a specific task execution finished
 */
case class _TasksExecutionFinished(_source: ActorRef, _time: DateTime, _jobId: Long, _taskIndex: Int) extends BasicMessage