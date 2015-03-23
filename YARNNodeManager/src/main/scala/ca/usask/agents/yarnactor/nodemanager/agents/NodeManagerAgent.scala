package ca.usask.agents.yarnactor.nodemanager.agents

import ca.usask.agents.yarnactor.common.agents._
import ca.usask.agents.yarnactor.jobmanager.agents._
import ca.usask.agents.yarnactor.common.records._
import ca.usask.agents.yarnactor.nodemanager.utils._
import scala.util.control.Exception
import org.joda.time.DateTime
import akka.actor._
import scala.concurrent.duration._
import scala.collection.mutable._

class NodeManagerAgent(val id: Int = 0) extends Agent {

    var serverState: ServerState = null
    var missedHeartBeat = false
    var containerToActorSystem = Map[Long, (ActorSystem, ActorRef)]()
    val clusterManager = context.actorSelection(NodeManagerConfig.getClusterManagerAddress)

    import context.dispatcher

    def receive = {
        case "heartBeatEvent"                        => Event_heartBeat()
        case "emptyHeartBeatResponse"                =>
        case message: _NodeManagerSimulationInitiate => Event_NodeManagerSimulationInitiate(message)
        case message: _ContainerExecutionFinished    => Event_ContainerExecutionFinished(message)
        case message: _AllocateContainerFromCM => {
            println("AllocateContainerFromCM " + id.toString())
            Handle_AllocateContainerFromCM(sender(), message)
        }
        case _ => Handle_UnknownMessage("NodeManagerAgent")
    }

    def Event_NodeManagerSimulationInitiate(message: _NodeManagerSimulationInitiate) = {
        Logger.Log("NodeManagerAgent" + id.toString() + " Initialization Start")
        serverState = new ServerState()
        serverState.initializeSimulationServer(message.resource, message.capabilities)
        context.system.scheduler.scheduleOnce(NodeManagerConfig.heartBeatStartDelay, self, "heartBeatEvent")
        Logger.Log("NodeManagerAgent" + id.toString() + " Initialization End")
    }

    def Event_heartBeat() = {
        clusterManager ! new _HeartBeat(self, DateTime.now(), serverState.getServerStatus(self))
        context.system.scheduler.scheduleOnce(NodeManagerConfig.heartBeatStartDelay, self, "heartBeatEvent")
    }

    def Event_ContainerExecutionFinished(message: _ContainerExecutionFinished) = {
        serverState.killContainer(message.containerId)
        if (message.isJobManager) {
            val (tempSystem, tempActor) = containerToActorSystem.get(message.containerId).get
            tempSystem.stop(tempActor)
        }
    }

    def Handle_AllocateContainerFromCM(sender: ActorRef, message: _AllocateContainerFromCM) = {
        if (message._jobDescriptions != null)
            if (startNewJobManagers(message._jobDescriptions) < message._jobDescriptions.length)
                sender ! "ridi"

        if (message._taskDescriptions != null)
            if (startNewTasks(message._taskDescriptions) < message._taskDescriptions.length)
                sender ! "ridi"
    }

    def startNewJobManagers(jobs: List[(JobDescription, SamplingInformation)]): Int = jobs match {
        case List() => 0
        case x :: xs =>
            if (startAJobManager(x._1, x._2))
                startNewJobManagers(xs) + 1
            else
                startNewJobManagers(xs)
    }

    def startAJobManager(job: JobDescription, samplInfo: SamplingInformation): Boolean = {
        serverState.createContainer(job.userId, job.jobId, 0, job.tasks(0).resource) match {
            case None => false
            case Some(x) => {
                createJobManagerActor(job, samplInfo, x)
                true
            }
        }
    }

    import com.typesafe.config.ConfigFactory
    def createJobManagerActor(job: JobDescription, samplInfo: SamplingInformation, containerId: Long) = {
        val jobMangerSystem = ActorSystem.create("JobManagerAgent", ConfigFactory.load().getConfig("JobManagerAgent"))
        val newJobManager = jobMangerSystem.actorOf(Props(new JobManagerAgent(containerId, self, job.userId, job.jobId)), name = "JobManagerAgent")
        newJobManager ! new _JobManagerSimulationInitiate(job.tasks)
        containerToActorSystem.update(containerId, (jobMangerSystem, newJobManager))
    }

    def startNewTasks(tasks: List[TaskDescription]): Int = tasks match {
        case Nil     => 0
        case x :: xs => if (startANewTask(x, x.jobManagerRef)) startNewTasks(xs) + 1 else 0
    }

    def startANewTask(task: TaskDescription, ownerActor: ActorRef): Boolean = {
        serverState.createContainer(task.userId, task.jobId, task.index, task.resource) match {
            case None => false
            case Some(x) => {
                context.system.scheduler.scheduleOnce(FiniteDuration(task.duration.getMillis, MILLISECONDS), self, new _ContainerExecutionFinished(x, false))
                true
            }
        }
    }
}