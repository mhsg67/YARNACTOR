package ca.usask.agents.macrm.clustermanager.agents

import ca.usask.agents.macrm.clustermanager.utils._
import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.common.agents._
import scala.concurrent.duration._
import org.joda.time._
import akka.actor._
import akka.camel._

class UserInterfaceAgent(val queueAgent: ActorRef) extends Agent with Consumer {

    var jobIdToUserRef = scala.collection.mutable.Map[Long, ActorRef]()

    def endpointUri = "netty:tcp://0.0.0.0:2001?textline=true"

    def receive = {
        case "initiateEvent"       => Event_initiate()
        case "TestStart"           => Test_Handle_UserMessage()
        case message: _JobFinished => Handle_JobFinished(message)
        case message: CamelMessage => Handle_UserMessage(message, sender())
        case _                     => Handle_UnknownMessage("UserInterfaceAgent")
    }

    import context.dispatcher
    def Event_initiate() = {
        Logger.Log("UserInterfaceAgent Initialization")
        context.system.scheduler.scheduleOnce(2000 millis, self, "TestStart")
    }

    def Handle_UserMessage(message: CamelMessage, sender: ActorRef) = {
        JSONManager.getJobDescription(message.body.toString()) match {
            case Left(x) => sender ! "Incorrect job submission format: " + x
            case Right(x) => {
                jobIdToUserRef.update(x.jobId, sender)                
                queueAgent ! new _JobSubmission(x)
            }
        }
    }

    def Test_Handle_UserMessage() = {
        println("YES-1")
        val tempString = """{"JI": 1,"UI": 1,"TS":[{"INX":1,"DUR":100,"RST":1500,"CPU":1,"MEM":250,"PRI":0,"TSC":1},{"INX":2,"DUR":100,"RST":2,"CPU":1,"MEM":250,"PRI":0,"TSC":1}],"CS": []}"""

        JSONManager.getJobDescription(tempString) match {
            case Left(x) => println("Incorrect job submission format: " + x)
            case Right(x) => {
                queueAgent ! new _JobSubmission(x)
            }
        }
    }
    
    def Handle_JobFinished(message:_JobFinished) = {
        println("Job " + message._jobId + " finished")
    }
}