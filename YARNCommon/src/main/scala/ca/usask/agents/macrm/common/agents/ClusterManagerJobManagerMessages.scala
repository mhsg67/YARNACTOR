package ca.usask.agents.macrm.common.agents

import ca.usask.agents.macrm.common.records._
import org.joda.time.DateTime
import akka.actor._

/**
 * From JobManager to ClusterManger for those tasks that JobManager could not
 * find resources for them by sampling or the system is in centralized scheduling 
 * mode then all the JobManager should send the resource requests to ClusterManager
 */
case class _TaskSubmissionFromJM(_source: ActorRef, _time: DateTime, _taskDescriptions: List[TaskDescription])
    extends BasicMessage
    
/**
 * From JobManager to ClusterManager to inform it about completion of job
 */
case class _JobFinished(_source:ActorRef, _time:DateTime, _jobId:Long)