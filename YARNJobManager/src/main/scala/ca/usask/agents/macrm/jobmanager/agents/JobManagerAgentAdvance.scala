package ca.usask.agents.macrm.jobmanager.agents

import ca.usask.agents.macrm.jobmanager.utils._
import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.common.agents._
import scala.collection.mutable._
import org.joda.time._
import akka.actor._
import scala.concurrent.duration._

class JobManagerAgentAdvance(val userId: Int,
                      val jobId: Long,
                      val samplingInformation: SamplingInformation) extends Agent {

    import context.dispatcher
    var waveToHighestSamplingRateWithTaskIndexWithConstraints = Map[Int, List[(Int, Int, List[Constraint])]]()
    var currentWaveOfTasks = 0
    val resourceTracker = context.actorSelection(JobManagerConfig.getResourceTrackerAddress)
    val clusterManager = context.actorSelection(JobManagerConfig.getClusterManagerAddress)

    def receive = {
        case "initiateEvent"                        => Event_initiate()
        case "heartBeatEvent"                       => Event_heartBeat()
        case message: _ResourceSamplingResponse     => Handle_ResourceSamplingResponse(message, sender())
        case message: _TaskSubmission               => Handle_TaskSubmission(message)
        case message: _JMHeartBeatResponse          => Handle_JMHeartBeatResponse(message)
        case message: _NodeSamplingTimeout          => Handle_NodeSamplingTimeout(message)
        case message: _JobManagerSimulationInitiate => Event_JobManagerSimulationInitiate(message)
        case _                                      => Handle_UnknownMessage
    }

    def Event_initiate() = {
        Logger.Log("JobManagerAgent<ID:" + jobId + "> Initialization")
        SamplingManagerAdvance.loadSamplingInformation(samplingInformation)
    }

    def Event_JobManagerSimulationInitiate(message: _JobManagerSimulationInitiate) = {
        Logger.Log("JobManagerAgent<ID:" + jobId + "> Initialization")

        SamplingManagerAdvance.loadSamplingInformation(samplingInformation)

        if (message.taskDescriptions.length > 1) {
            val tempTask = message.taskDescriptions.map(x => TaskDescription(self, jobId, x, userId))

            collection.SortedSet(tempTask.map(x => x.relativeSubmissionTime.getMillis): _*).foreach { x =>
                val tasksWithSimilarSubmissionTime = tempTask.filter(y => y.relativeSubmissionTime.getMillis == x)
                context.system.scheduler.scheduleOnce(FiniteDuration(x, MILLISECONDS), self, new _TaskSubmission(tasksWithSimilarSubmissionTime))
            }
        }
        else {
            //TODO:for jobs with just one task we should set the finish state and send it to RT
        }
    }

    def Event_heartBeat() = {
        //TODO: manager sending heart beat by fetching datat from waveToHighestSamplingRateWithTaskIndexWithConstraints and remove rows after submiting heartbeat
        resourceTracker ! create_JMHeartBeat()
        //context.system.scheduler.scheduleOnce(JobManagerConfig.heartBeatInterval, self, "heartBeatEvent")
    }

    def Handle_ResourceSamplingResponse(message: _ResourceSamplingResponse, sender: ActorRef) =
        SamplingManagerAdvance.whichTaskShouldSubmittedToThisNode(currentWaveOfTasks, message._availableResource, new NodeId(sender.path.address.host.get, sender.path.address.port.get, sender)) match {
            case None    => sender ! new _ResourceSamplingCancel(self, DateTime.now(), jobId)
            case Some(x) => sender ! new _AllocateContainerFromJM(self, DateTime.now(), x)
        }

    def Handle_TaskSubmission(message: _TaskSubmission) = {
        /**NOTE: I have decided to submit heartbeat after each wave of tasks to let RT know about sampling rates**/
        context.system.scheduler.scheduleOnce(JobManagerConfig.heartBeatStartDelay, self, "heartBeatEvent")

        currentWaveOfTasks += 1
        SamplingManagerAdvance.addNewSubmittedTasksIntoWaveToTaks(currentWaveOfTasks, message.taskDescriptions)

        val samplingList = SamplingManagerAdvance.getSamplingNode(message.taskDescriptions, 0)
        samplingList.foreach(x => getActorRefFromNodeId(x._1) ! new _ResourceSamplingInquiry(self, DateTime.now(), x._2, jobId))
        context.system.scheduler.scheduleOnce(JobManagerConfig.samplingTimeout, self, new _NodeSamplingTimeout(currentWaveOfTasks, 0))
    }

    def Handle_NodeSamplingTimeout(message: _NodeSamplingTimeout) = {
        val unscheduledTasks = SamplingManagerAdvance.getUnscheduledTaskOfWave(message.forWave)
        if (unscheduledTasks.length > 0) {
            updateWaveToHighestSamplingRateToConstraints(message.forWave, message.retry, unscheduledTasks)
            if (message.retry < JobManagerConfig.numberOfAllowedSamplingRetry) {
                val samplingList = SamplingManagerAdvance.getSamplingNode(unscheduledTasks, message.retry + 1)
                samplingList.foreach(x => getActorRefFromNodeId(x._1) ! new _ResourceSamplingInquiry(self, DateTime.now(), x._2, jobId))
                context.system.scheduler.scheduleOnce(JobManagerConfig.samplingTimeout, self, new _NodeSamplingTimeout(currentWaveOfTasks, message.retry + 1))
            }
            else {
                clusterManager ! new _TaskSubmissionFromJM(self, DateTime.now(), unscheduledTasks)
            }
        }
    }

    def updateWaveToHighestSamplingRateToConstraints(wave: Int, retry: Int, unscheduledTasks: List[TaskDescription]) = {
        val unscheduledTasksIndexAndConstraints = unscheduledTasks.map(x => (x.index, x.constraints))
        waveToHighestSamplingRateWithTaskIndexWithConstraints.get(wave) match {
            case None =>
                waveToHighestSamplingRateWithTaskIndexWithConstraints.update(wave, unscheduledTasksIndexAndConstraints.map(y => (retry * SamplingManagerAdvance.samplingRate, y._1, y._2)))
            case Some(x) => {
                val oldPart = x.filterNot(p => unscheduledTasksIndexAndConstraints.exists(q => q._1 == p._2))
                val newPart = unscheduledTasksIndexAndConstraints.map(x => (retry * SamplingManagerAdvance.samplingRate, x._1, x._2))
                waveToHighestSamplingRateWithTaskIndexWithConstraints.update(wave, oldPart ++ newPart)
            }
        }
    }

    def Handle_JMHeartBeatResponse(message: _JMHeartBeatResponse) = SamplingManagerAdvance.loadNewSamplingRate(message._samplingRate)

    //TODO: change to refelect the number of retry and if
    //some of the task failed show their constraints
    //TODO: the last argument of JobReport is completely wrong
    def create_JMHeartBeat() = new _JMHeartBeat(self, DateTime.now(), new JobReport(userId, jobId, SamplingManagerAdvance.samplingRate))

    def getActorRefFromNodeId(node: NodeId): ActorSelection = context.actorSelection(JobManagerConfig.createNodeManagerAddressString(node.host, node.port))

}
