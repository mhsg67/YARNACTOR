package ca.usask.agents.yarnactor.common.agents

import org.joda.time.DateTime
import akka.actor._
import ca.usask.agents.yarnactor.common.records.NodeReport

/**
 * From NodeManger to ResourceTacker to inform it about node
 * current condition
 */
case class _HeartBeat(_source: ActorRef, _time: DateTime, _report: NodeReport)
    extends BasicMessage 
    