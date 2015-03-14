package ca.usask.agents.macrm.jobmanager.agents

import ca.usask.agents.macrm.jobmanager.utils._
import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.common.agents._
import scala.collection.mutable._
import org.joda.time._
import akka.actor._
import scala.concurrent.duration._

/**
 * 1. After completing each wave it will submitt a heartbeat
 * 2. there is no single tasks job
 */

class JobManagerAgent(val containerId: Long,
                      val node: ActorRef,
                      val userId: Int,
                      val jobId: Long,
                      val samplingInformation: SamplingInformation) extends Agent {

    import context.dispatcher
    var waveToSamplingRate = Map[Int, Int]()
    var currentWaveOfTasks = 0
    var remainingTasksToFinish = 0

    val samplingManager = new SamplingManager()
    val resourceTracker = context.actorSelection(JobManagerConfig.getResourceTrackerAddress)
    val clusterManager = context.actorSelection(JobManagerConfig.getClusterManagerAddress)

    def receive = {
        case message: _ResourceSamplingResponse     => Handle_ResourceSamplingResponse(message, sender())
        case message: _TaskSubmission               => Handle_TaskSubmission(message)
        case message: _JMHeartBeatResponse          => Handle_JMHeartBeatResponse(message)
        case message: _NodeSamplingTimeout          => Handle_NodeSamplingTimeout(message)
        case message: _TasksExecutionFinished       => Handle_TasksExecutionFinished(message)
        case message: _JobManagerSimulationInitiate => Event_JobManagerSimulationInitiate(message)
        case _                                      => Handle_UnknownMessage
    }

    def Event_JobManagerSimulationInitiate(message: _JobManagerSimulationInitiate) = {
        Logger.Log("JobManagerAgent<ID:" + jobId + "> Initialization")
        samplingManager.loadSamplingInformation(samplingInformation)

        val tempTask = message.taskDescriptions.tail.map(x => TaskDescription(self, jobId, x, userId))
        remainingTasksToFinish = tempTask.length
        collection.SortedSet(tempTask.map(x => x.relativeSubmissionTime.getMillis): _*).
            foreach { x =>
                val tasksWithSimilarSubmissionTime = tempTask.filter(y => y.relativeSubmissionTime.getMillis == x)
                context.system.scheduler.scheduleOnce(FiniteDuration(x, MILLISECONDS), self, new _TaskSubmission(tasksWithSimilarSubmissionTime))
            }
    }

    def Handle_ResourceSamplingResponse(message: _ResourceSamplingResponse, sender: ActorRef) = {
        samplingManager.whichTaskShouldSubmittedToThisNode(currentWaveOfTasks, message._availableResource) match {
            case None    => sender ! new _ResourceSamplingCancel(self, DateTime.now(), jobId)
            case Some(x) => sender ! new _AllocateContainerFromJM(self, DateTime.now(), x)
        }
    }

    def Handle_TaskSubmission(message: _TaskSubmission) = {
        currentWaveOfTasks += 1
        updateWaveToSamplingRate(currentWaveOfTasks, 0)
        samplingManager.addNewSubmittedTasksIntoWaveToTaks(currentWaveOfTasks, message.taskDescriptions)
        val samplingList = samplingManager.getSamplingNode(message.taskDescriptions, 0)

        samplingList.foreach(x => getActorRefFromNodeId(x._1) ! new _ResourceSamplingInquiry(self, DateTime.now(), x._2, jobId))
        context.system.scheduler.scheduleOnce(JobManagerConfig.samplingTimeout, self, new _NodeSamplingTimeout(currentWaveOfTasks, 1))
    }

    def Handle_NodeSamplingTimeout(message: _NodeSamplingTimeout) = {
        val unscheduledTasks = samplingManager.getUnscheduledTaskOfWave(message.forWave)
        if (unscheduledTasks.length > 0) {
            updateWaveToSamplingRate(message.forWave, message.retry)
            if (message.retry <= JobManagerConfig.numberOfAllowedSamplingRetry) {
                val samplingList = samplingManager.getSamplingNode(unscheduledTasks, message.retry + 1)
                samplingList.foreach(x => getActorRefFromNodeId(x._1) ! new _ResourceSamplingInquiry(self, DateTime.now(), x._2, jobId))
                context.system.scheduler.scheduleOnce(JobManagerConfig.samplingTimeout, self, new _NodeSamplingTimeout(currentWaveOfTasks, message.retry + 1))

                println("NodeSamplingTimeout SamplingListSize " + samplingList.length.toString())
            }
            else {
                sendheartBeat(message.forWave)
                clusterManager ! new _TaskSubmissionFromJM(self, DateTime.now(), unscheduledTasks)
            }
        }
        else
            sendheartBeat(message.forWave)
    }

    def Handle_TasksExecutionFinished(message: _TasksExecutionFinished) = {
        remainingTasksToFinish -= 1
        if (remainingTasksToFinish == 0) {
            node ! new _ContainerExecutionFinished(containerId, true)
            clusterManager ! new _JobFinished(self, DateTime.now(), jobId)
        }
    }

    def sendheartBeat(waveNumber: Int) = {
        resourceTracker ! new _JMHeartBeat(self, DateTime.now(), new JobReport(userId, jobId, waveToSamplingRate.get(waveNumber).get))
    }

    def updateWaveToSamplingRate(wave: Int, retry: Int) = waveToSamplingRate.update(wave, (samplingManager.samplingRate * math.pow(2, retry)).toInt)

    def Handle_JMHeartBeatResponse(message: _JMHeartBeatResponse) = samplingManager.loadNewSamplingRate(message._samplingRate)

    def getActorRefFromNodeId(node: NodeId): ActorSelection = context.actorSelection(JobManagerConfig.createNodeManagerAddressString(node.host, node.port))

}
