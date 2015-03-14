package ca.usask.agents.macrm.common.agents

import org.joda.time.DateTime
import akka.actor._
import ca.usask.agents.macrm.common.records.NodeReport
import ca.usask.agents.macrm.common.records.JobReport

/**
 * From JobManager to ResourceTracker to inform it about job
 * current condition
 */
case class _JMHeartBeat(_source: ActorRef, _time: DateTime, _jobReport: JobReport)
    extends BasicMessage

/**
 * From ResourceTracker to JobManager to inform it about
 * failure in one of its container
 *
 * TODO: It can be used for both node failure and
 * container failure, then add required information into
 * the message such as node information and container
 * information
 */
case class _ContainerFailure(_source: ActorRef, _time: DateTime)
    extends BasicMessage

/**
 * From ResourceTracker to JobManager
 */
case class _JMHeartBeatResponse(_source: ActorRef, _time: DateTime, _samplingRate: Int)
    extends BasicMessage
