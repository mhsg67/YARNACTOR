package ca.usask.agents.macrm.clustermanager.agents

import ca.usask.agents.macrm.clustermanager.utils._
import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.common.agents._
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
    val clusterStructure = new ClusterDatastructure()

    def receive = {
        case "initiateEvent"                    => Event_initiate()
        case "getNextTaskForScheduling"         => Handle_getNextTaskForScheduling(sender)
        case message: _ClusterState             => Handle_ClusterState(message)
        case message: _ServerWithEmptyResources => Handle_ServerWithEmptyResources(message)
        case message: _EachUserShareOfCluster   => Handle_EachUserShareOfCluster(message)
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
                if (headOfJobQueue.get.numberOfTasks != 1)
                    schedulerJob(headOfJobQueue.get, message, clusterStructure.getCurrentSamplingInformation(headOfJobQueue.get.constraints()))
                else
                    schedulerJob(headOfJobQueue.get, message, null)
            }
            else{                
                message._report.nodeId.agent ! "emptyHeartBeatResponse"
            }
        }
    }

    def Handle_JobSubmission(message: _JobSubmission) = schedulingQueue.EnqueueJob(message.jobDescription)

    def Handle_TaskSubmission(message: _TaskSubmission) = message.taskDescriptions.foreach(x => schedulingQueue.EnqueueTask(x))

    def scheduleTask(task: TaskDescription, message: _ServerWithEmptyResources) = message._report.nodeId.agent ! new _AllocateContainerFromCM(self, DateTime.now(), List(task), null)

    def schedulerJob(job: JobDescription, message: _ServerWithEmptyResources, samplingInformation: SamplingInformation) = message._report.nodeId.agent ! new _AllocateContainerFromCM(self, DateTime.now(), null, List((job, samplingInformation)))

    def Handle_ClusterState(message: _ClusterState) = clusterStructure.updateClusterStructure(message._newSamplingRate, message._removedServers, message._addedServers, message._rareResources)

    /*
     * TODO: Implement following functions
     */

    def Handle_getNextTaskForScheduling(sender: ActorRef) = {

    }

    def Handle_EachUserShareOfCluster(message: _EachUserShareOfCluster) = {

    }
}
