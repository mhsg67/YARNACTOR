package ca.usask.agents.macrm.clustermanager.agents

import ca.usask.agents.macrm.clustermanager.utils._
import ca.usask.agents.macrm.common.agents._
import akka.actor._

class SchedulerAgent(val queueAgent: ActorRef, val myId: Int, var RackAgents: List[ActorRef]) extends Agent {

    def receive = {
        case "initiateEvent"           => Event_initiate()
        case "changeToDistributedMode" => Handle_ChangeToDistributedMode()
        case message: _JobSubmission   => Handle_JobSubmission(message)
        case message: _TaskSubmission  => Handle_TaskSubmission(message)
        case _                         => Handle_UnknownMessage

    }

    def Event_initiate() = {
        Logger.Log(("SchedulerAgent" + myId.toString() + " Initialization"))

        queueAgent ! "getNextTaskForScheduling"
    }

    def Handle_ChangeToDistributedMode() = {
        //TODO: it should stop asking task from queueAgent 
        //also finish scheduling of its current task scheduling
    }

    def Handle_JobSubmission(message: _JobSubmission) = {

    }

    def Handle_TaskSubmission(message: _TaskSubmission) = {

    }

}
