package ca.usask.agents.macrm.common.agents

import org.joda.time.DateTime
import akka.actor._
import ca.usask.agents.macrm.common.records.NodeReport

/**
 * From NodeManger to ResourceTacker to inform it about node
 * current condition
 */
case class _HeartBeat(_source: ActorRef, _time: DateTime, _report: NodeReport)
    extends BasicMessage 
    