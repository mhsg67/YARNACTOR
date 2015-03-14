package ca.usask.agents.macrm.nodemanager.agents

import ca.usask.agents.macrm.common.agents._
import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.nodemanager.utils._
import scala.util.control.Exception
import org.joda.time.DateTime
import akka.actor._

class RealNodeManagerAgent(val id: Int = 0) extends Agent {

    val resourceTracker = context.actorSelection(NodeManagerConfig.getResourceTrackerAddress)
    val serverState = context.actorOf(Props(new ServerStateAgent(self)), name = "ServerStateAgent" + id.toString())
    val nodeMonitor = context.actorOf(Props(new NodeMonitorAgent(self, serverState)), name = "nodeMonitorAgnet" + id.toString())
    val containerManager = context.actorOf(Props(new ContainerManagerAgent(self, serverState)), name = "ContainerManagerAgent" + id.toString())

    def receive = {
        case "ridi"                                  => println("ridid")
        case "initiateEvent"                         => Event_initiate()
        case message: _NodeManagerSimulationInitiate => Event_NodeManagerSimulationInitiate(message)
        case message: _HeartBeat                     => resourceTracker ! new _HeartBeat(self, DateTime.now(), message._report)
        case message: _ResourceSamplingInquiry => {
            println("ResourceSamplingInquiry " + id.toString())
            containerManager ! new _ResourceSamplingInquiry(sender(), message._time, message._minRequiredResource, message._jobId)
        }
        case message: _ResourceSamplingCancel => {
            println("ResourceSamplingCancel " + id.toString())
            containerManager ! new _ResourceSamplingCancel(sender(), message._time, message._jobId)
        }
        case message: _AllocateContainerFromCM => {
            println("AllocateContainerFromCM " + id.toString())
            containerManager ! message
        }
        case message: _AllocateContainerFromJM => {
            println("AllocateContainerFromJM " + id.toString())
            containerManager ! message
        }
        case message: _ResourceSamplingResponse => {
            println("ResourceSamplingResponse " + id.toString())
            Handle_ResourceSamplingResponse(message)
        }

        case _                                  => Handle_UnknownMessage("NodeManagerAgent")
    }

    def Event_initiate() = {
        Logger.Log("NodeManagerAgent Initialization Start")

        //TODO: Should be blocking messaging
        serverState ! "initiateEvent"
        nodeMonitor ! "initiateEvent"
        containerManager ! "initiateEvent"

        Logger.Log("NodeManagerAgent Initialization End")

    }

    def Handle_ResourceSamplingResponse(message: _ResourceSamplingResponse) = {
        message._source ! new _ResourceSamplingResponse(self, message._time, message._availableResource)
    }

    def Event_NodeManagerSimulationInitiate(message: _NodeManagerSimulationInitiate) = {
        Logger.Log("NodeManagerAgent" + id.toString() + " Initialization Start")

        serverState ! message
        nodeMonitor ! "initiateEvent"
        containerManager ! "simulationInitiateEvent"

        Logger.Log("NodeManagerAgent" + id.toString() + " Initialization End")
    }
}
