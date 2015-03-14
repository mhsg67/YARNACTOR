package ca.usask.agents.macrm.common.agents

import akka.actor._
import scala.collection._
import scala.collection.mutable.ArrayBuffer
import org.joda.time._

import ca.usask.agents.macrm.common.records._

trait BasicMessage

/**
 * These are basic message that each agent (RM,RT,NM,JB) use internally
 */
case class _Resource(val _resource: Resource) extends BasicMessage

case class _ContainerExecutionFinished(val containerId: Long, val isJobManager:Boolean)

case class _AllocateContainer(val userId: Int, val jobId: Long, val taskIndex: Int, val size: Resource, val duration: Duration = null, val isHeartBeatRespond: Boolean = false)

case class _JobSubmission(val jobDescription: JobDescription)

case class _TaskSubmission(val taskDescriptions: List[TaskDescription]) extends BasicMessage

case class _NodeSamplingTimeout(val forWave:Int, val retry:Int) extends BasicMessage

case class _NodeManagerSimulationInitiate(val resource: Resource, val capabilities: List[Constraint])

case class _JobManagerSimulationInitiate(val taskDescriptions: List[TaskDescription]) extends BasicMessage