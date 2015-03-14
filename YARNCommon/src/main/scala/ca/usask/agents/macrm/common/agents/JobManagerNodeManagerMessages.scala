package ca.usask.agents.macrm.common.agents

import ca.usask.agents.macrm.common.records.NodeReport
import org.joda.time.DateTime
import akka.actor._
import ca.usask.agents.macrm.common.records._

/**
 * From JobManager to NodeManager to inquiry about node resource
 * availability
 */
case class _ResourceSamplingInquiry(_source: ActorRef, _time: DateTime, _minRequiredResource: Resource, _jobId: Long) extends BasicMessage

/**
 * From JobManager to NodeManger to cancel previous resource inquiry
 * request
 */
case class _ResourceSamplingCancel(_source: ActorRef, _time: DateTime, _jobId: Long) extends BasicMessage

/**
 * From NodeManager to JobManager as a response for resource availability
 * inquiry (_ResourceSamplingInquiry)
 */
case class _ResourceSamplingResponse(_source: ActorRef, _time: DateTime, _availableResource: Resource) extends BasicMessage

/**
 * From JobManager to NodeManager in response to _ResourceSamplingResponse for Allocating
 * container for provided list of tasks
 */
case class _AllocateContainerFromJM(_source: ActorRef, _time: DateTime, _taskDescriptions: List[TaskDescription])
    extends BasicMessage

/**
 * From NodeManager to JobManager to inform it that the a specific task execution finished
 */
case class _TasksExecutionFinished(_source: ActorRef, _time: DateTime, _taskIndex: Int) extends BasicMessage

