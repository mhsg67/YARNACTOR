package ca.usask.agents.yarnactor.jobmanager.agents

import ca.usask.agents.yarnactor.jobmanager.utils._
import ca.usask.agents.yarnactor.common.records._
import ca.usask.agents.yarnactor.common.agents._
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
                      val jobId: Long) extends Agent {

    import context.dispatcher
    var remainingTasksToFinish = 0

    val clusterManager = context.actorSelection(JobManagerConfig.getClusterManagerAddress)

    def receive = {
        case message: _TaskSubmission               => Handle_TaskSubmission(message)
        case message: _TasksExecutionFinished       => Handle_TasksExecutionFinished(message)
        case message: _JobManagerSimulationInitiate => Event_JobManagerSimulationInitiate(message)
        case _                                      => Handle_UnknownMessage
    }

    def Event_JobManagerSimulationInitiate(message: _JobManagerSimulationInitiate) = {
        Logger.Log("JobManagerAgent<ID:" + jobId + "> Initialization")

        val tempTask = message.taskDescriptions.tail.map(x => TaskDescription(self, jobId, x, userId))
        remainingTasksToFinish = tempTask.length
        collection.SortedSet(tempTask.map(x => x.relativeSubmissionTime.getMillis): _*).
            foreach { x =>
                val tasksWithSimilarSubmissionTime = tempTask.filter(y => y.relativeSubmissionTime.getMillis == x)
                context.system.scheduler.scheduleOnce(FiniteDuration(x, MILLISECONDS), self, new _TaskSubmission(tasksWithSimilarSubmissionTime))
            }
    }

    def Handle_TaskSubmission(message: _TaskSubmission) = {
        clusterManager ! new _TaskSubmissionFromJM(self, DateTime.now(), message.taskDescriptions)
    }

    def Handle_TasksExecutionFinished(message: _TasksExecutionFinished) = {
        remainingTasksToFinish -= 1
        if (remainingTasksToFinish == 0) {
            node ! new _ContainerExecutionFinished(containerId, true)
            clusterManager ! new _JobFinished(self, DateTime.now(), jobId)
        }
    }

}
