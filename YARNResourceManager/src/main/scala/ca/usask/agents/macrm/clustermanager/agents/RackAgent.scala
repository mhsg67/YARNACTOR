package ca.usask.agents.macrm.clustermanager.agents

import ca.usask.agents.macrm.clustermanager.utils._
import ca.usask.agents.macrm.common.agents._
import akka.actor._

class RackAgent(val myId: Int) extends Agent {

    def receive = {
        case "initiateEvent" => Event_initiate()
        case _               => Handle_UnknownMessage
    }

    def Event_initiate() {
        Logger.Log("QueueAgent" + myId.toString() + " Initialization")
    }
}