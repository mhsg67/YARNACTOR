package ca.usask.agents.macrm.nodemanager.agents

import ca.usask.agents.macrm.nodemanager.utils._
import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.common.agents._
import org.joda.time.DateTime
import akka.actor._

class NodeMonitorAgent(val nodeManager: ActorRef, val serverState: ActorRef) extends Agent {

    import context.dispatcher
    override def preStart() = {
        context.system.scheduler.scheduleOnce(NodeManagerConfig.heartBeatStartDelay, self, "heartBeatEvent")
    }

    def receive = {
        case "initiateEvent"     => Event_initiate()
        case "heartBeatEvent"    => serverState ! "heartBeatEvent"
        case message: _HeartBeat => Handle_heartBeat(message)
        case _                   => Handle_UnknownMessage("NodeMonitorAgent")
    }

    def Event_initiate() = {
        Logger.Log("NodeMonitorAgent Initialization")
    }

    /**
     * Maybe that self scheduled message lose then you should have
     * recovery mechanism, maybe sending two message each after NodeManagerConfig.heartBeatInterval*2
     */
    def Handle_heartBeat(_message: _HeartBeat) = {
        nodeManager ! _message
        context.system.scheduler.scheduleOnce(NodeManagerConfig.heartBeatInterval, self, "heartBeatEvent")
    }

}