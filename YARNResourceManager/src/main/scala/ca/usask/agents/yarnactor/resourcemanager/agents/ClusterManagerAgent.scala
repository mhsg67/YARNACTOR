package ca.usask.agents.yarnactor.resourcemanager.agents

import ca.usask.agents.yarnactor.resourcemanager.utils._
import ca.usask.agents.yarnactor.common.records._
import ca.usask.agents.yarnactor.common.agents._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import scala.collection.immutable._
import akka.actor._

class ClusterManagerAgent extends Agent {

    val queueAgent = context.actorOf(Props[QueueAgent], name = "QueueAgent")
    val userInterfaceAgent = context.actorOf(Props(new UserInterfaceAgent(queueAgent)), name = "UserInterfaceAgent")

    var jobIdToActor = Map[Long, ActorRef]()

    def receive = {
        case "initiateEvent"                => Event_initiate()
        case message: _HeartBeat            => Handle_HeartBeat(sender(), message)
        case message: _TaskSubmissionFromJM => Handle_TaskSubmissionFromJM(sender(), message)
        case message: _JobFinished          => Handle_JobFinished(message)
        case _                              => Handle_UnknownMessage("ClusterManagerAgent")
    }

    def Event_initiate() = {
        Logger.Log("ClusterManagerAgent Initialization")

        //TODO: Should be blocking messaging
        queueAgent ! "initiateEvent"
        userInterfaceAgent ! "initiateEvent"

        Logger.Log("ClusterManagerAgent Initialization End")
    }

    def Handle_HeartBeat(sender: ActorRef, message: _HeartBeat) = {
        sendTaskFinishMessages(message._report.finishedContainers)
        updateClusterDatebaseByNMHeartBeat(sender, message)
        queueAgent ! new _ServerWithEmptyResources(self, DateTime.now(), addIPandPortToNodeReport(message._report, sender))
    }

    def sendTaskFinishMessages(jobIdToTaskIndex: List[(Long, Int)]) = {
        jobIdToTaskIndex.foreach(x => jobIdToActor.get(x._1) match {
            case None => //TODO:throw exception 
            case Some(y) => y ! new _TasksExecutionFinished(self, DateTime.now(), x._1, x._2) 
        })
    }

    def addIPandPortToNodeReport(oldReport: NodeReport, sender: ActorRef) =
        new NodeReport(new NodeId(sender.path.address.host.get, sender.path.address.port.get, sender),
            oldReport.resource, oldReport.capabilities, oldReport.containers, oldReport.finishedContainers,
            oldReport.utilization, oldReport.nodeState, oldReport.queueState)

    def updateClusterDatebaseByNMHeartBeat(sender: ActorRef, message: _HeartBeat) = {
        val nodeId = new NodeId(sender.path.address.host.get, sender.path.address.port.get, message._source)
        val usedResources = message._report.containers.foldLeft(new Resource(0, 0))((x, y) => y.resource + x)
        ClusterDatabase.updateNodeState(nodeId, message._report.resource, usedResources, message._report.capabilities,
            message._report.utilization, message._report.queueState)
        ClusterDatabase.updateNodeContainer(nodeId, message._report.containers)
    }

    def Handle_JobFinished(message: _JobFinished) = {
        userInterfaceAgent ! message
    }

    def Handle_TaskSubmissionFromJM(sender: ActorRef, message: _TaskSubmissionFromJM) = {
        jobIdToActor.updated(message._taskDescriptions(0).jobId, sender)
        val tasks = message._taskDescriptions.map(x => new TaskDescription(sender, x.jobId, x.index, x.duration, x.resource, x.relativeSubmissionTime, x.constraints, x.userId))
        queueAgent ! new _TaskSubmission(tasks)
    }
}