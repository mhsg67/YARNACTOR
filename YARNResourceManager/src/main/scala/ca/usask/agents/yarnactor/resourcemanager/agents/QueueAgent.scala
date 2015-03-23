package ca.usask.agents.yarnactor.resourcemanager.agents

import ca.usask.agents.yarnactor.resourcemanager.utils._
import ca.usask.agents.yarnactor.common.records._
import ca.usask.agents.yarnactor.common.agents._
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import akka.actor._
import java.util.Formatter.DateTime

/**
 * TODO: For adaption part which switch to central decision making
 * create some scheduler actors and forward resource request for allocation to them
 */
class QueueAgent extends Agent {

    val schedulingQueue = AbstractQueue(ClusterManagerConfig.QueueType)

    def receive = {
        case "initiateEvent"                    => Event_initiate()
        case message: _ServerWithEmptyResources => Handle_ServerWithEmptyResources(message)
        case message: _JobSubmission            => Handle_JobSubmission(message)
        case message: _TaskSubmission           => Handle_TaskSubmission(message)
        case _                                  => Handle_UnknownMessage("QueueAgent")
    }

    def Event_initiate() = {
        Logger.Log("QueueAgent Initialization")
    }

    //TODO: try to schedule as much task and job as possible since 
    //the node may have lots of free resources
    //TODO: check if you can make it parallel the headOfTaskQueue and 
    //the headOfJobQueue fetching, be careful about actor system 
    def Handle_ServerWithEmptyResources(message: _ServerWithEmptyResources) = {

        if (message._report.getFreeResources().isNotUsable())
            message._source ! "emptyHeartBeatResponse"
        else {
            val headOfTaskQueue = schedulingQueue.getFirtOrBestMatchTask(message._report.resource, message._report.capabilities)
            if (!headOfTaskQueue.isEmpty) {
                println("**** Schedule a Task")
                schedulingQueue.RemoveTask(headOfTaskQueue.get)
                scheduleTask(headOfTaskQueue.get, message)
            }
            else {
                val headOfJobQueue = schedulingQueue.getFirstOrBestMatchJob(message._report.resource, message._report.capabilities)
                if (!headOfJobQueue.isEmpty) {
                    println("**** Schedule a Job")
                    schedulingQueue.RemoveJob(headOfJobQueue.get)
                    schedulerJob(headOfJobQueue.get, message, null)                    
                }
                else {
                     message._source ! "emptyHeartBeatResponse"
                }
            }
        }
    }

    def Handle_JobSubmission(message: _JobSubmission) = schedulingQueue.EnqueueJob(message.jobDescription)

    def Handle_TaskSubmission(message: _TaskSubmission) = message.taskDescriptions.foreach(x => schedulingQueue.EnqueueTask(x))

    def scheduleTask(task: TaskDescription, message: _ServerWithEmptyResources) = message._report.nodeId.agent ! new _AllocateContainerFromCM(self, DateTime.now(), List(task), null)

    def schedulerJob(job: JobDescription, message: _ServerWithEmptyResources, samplingInformation: SamplingInformation) = message._report.nodeId.agent ! new _AllocateContainerFromCM(self, DateTime.now(), null, List((job, samplingInformation)))
}
