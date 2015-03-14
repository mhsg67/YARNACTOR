package ca.usask.agents.macrm.nodemanager.agents

import ca.usask.agents.macrm.nodemanager.utils._
import ca.usask.agents.macrm.jobmanager.agents._
import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.common.agents._
import scala.concurrent.duration._
import scala.collection.mutable._
import org.joda.time.DateTime
import akka.util._
import akka.pattern._
import akka.actor._
import akka.actor.Status._
import scala.concurrent._

class ContainerManagerAgent(val nodeManager: ActorRef, val serverState: ActorRef) extends Agent {

    var isSimulation = false
    var jobManagerList = List[(ActorRef, ActorSystem)]()
    var ignoreNextResourceMessage = false
    var ignoreResourceSamplingResponseTimeoutEvent = 0
    var resourceSmaplingInquiryList = Queue[_ResourceSamplingInquiry]()
    var recentlyServerdSamplingInquiry: _ResourceSamplingInquiry = null
    var havePendingServing = false

    import context.dispatcher
    val checkContainersEvent = context.system.scheduler.schedule(NodeManagerConfig.allCheckStartDelay, NodeManagerConfig.checkContainersInterval, self, "checkContainersEvent")

    def receive = {
        case "initiateEvent"                        => Event_initiate
        case "simulationInitiateEvent"              => Event_simulationInitiate
        case "checkContainersEvent"                 => Event_checkContainers
        case "resourceSamplingResponseTimeoutEvent" => Event_resourceSamplingResponseTimeout
        case message: _ResourceSamplingInquiry      => Handle_ResourceSamplingInquiry(message)
        case message: _ResourceSamplingCancel       => Handle_ResourceSamplingCancel(message)
        case message: _Resource                     => Handle_Resource(message)
        case message: _AllocateContainerFromCM      => Handle_AllocateContainerFromCM(message)
        case message: _AllocateContainerFromJM      => Handle_AllocateContainerFromJM(message)
        case _                                      => Handle_UnknownMessage("ContainerManagerAgent")
    }

    def Event_initiate = {
        Logger.Log("ContainerManagerAgent Initialization")
    }

    def Event_simulationInitiate = {
        Logger.Log("ContainerManagerAgent Initialization")
        isSimulation = true
    }

    def Event_checkContainers = serverState ! "checkContainersEvent"

    def Handle_ResourceSamplingInquiry(message: _ResourceSamplingInquiry) = {
        resourceSmaplingInquiryList += message
        if (resourceSmaplingInquiryList.length == 1)
            serverState ! "checkAvailableResourcesEvent"
    }

    def Handle_Resource(message: _Resource): Unit = {
        if (ignoreNextResourceMessage) {         
            println("Branch1")
            ignoreNextResourceMessage = false
            serverState ! "checkAvailableResourcesEvent"
        }
        else if (message._resource.isNotUsable()){ 
            println("Branch2")
            resourceSmaplingInquiryList = Queue()
        }
        else if (resourceSmaplingInquiryList(0)._minRequiredResource < message._resource) {  
            println("Branch3")
            havePendingServing = true
            nodeManager ! new _ResourceSamplingResponse(resourceSmaplingInquiryList(0)._source, DateTime.now(), message._resource)
            context.system.scheduler.scheduleOnce(NodeManagerConfig.waitForJMActionToResourceSamplingResponseTimeout, self, "resourceSamplingResponseTimeoutEvent")
        }
        else {            
            println("Branch4")
            resourceSmaplingInquiryList.dequeue()
            if (resourceSmaplingInquiryList.length > 0)
                Handle_Resource(message)
        }
    }

    def Event_resourceSamplingResponseTimeout = {
        println("ResourceSamplingTimeout")
        if (ignoreResourceSamplingResponseTimeoutEvent > 0)
            ignoreResourceSamplingResponseTimeoutEvent -= 1
        else {
            havePendingServing = false
            resourceSmaplingInquiryList.dequeue()
            if (resourceSmaplingInquiryList.length == 1)
                serverState ! "checkAvailableResourcesEvent"
        }
    }

    def Handle_ResourceSamplingCancel(message: _ResourceSamplingCancel) = {        
        if (resourceSmaplingInquiryList.length > 0) {
            if (resourceSmaplingInquiryList(0)._source == message._source) {
                resourceSmaplingInquiryList = resourceSmaplingInquiryList.filter((x) => x._source == message._source)
                if (havePendingServing) {
                    ignoreResourceSamplingResponseTimeoutEvent += 1
                    havePendingServing = false
                }
                else if (resourceSmaplingInquiryList.length > 0)
                    serverState ! "checkAvailableResourcesEvent"
            }
            else {
                resourceSmaplingInquiryList = resourceSmaplingInquiryList.filter((x) => x._source == message._source)
            }
        }
    }

    def Handle_AllocateContainerFromCM(message: _AllocateContainerFromCM) = {
        if (resourceSmaplingInquiryList.length > 0)
            ignoreNextResourceMessage = true

        if (message._jobDescriptions != null) 
            if (startNewJobManagers(message._jobDescriptions) < message._jobDescriptions.length)
                nodeManager ! "ridi"

        if (message._taskDescriptions != null)
            if (startNewTasks(message._taskDescriptions, true) < message._taskDescriptions.length)
                nodeManager ! "ridi"
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
        implicit val timeout = Timeout(2 seconds)
        val future = serverState ? new _AllocateContainer(job.userId, job.jobId, 0, job.tasks(0).resource, null, true)
        Await.result(future, timeout.duration).asInstanceOf[String] match {
            case "ACK" => {
                createJobManagerActor(job, samplInfo)
                true
            }
            case "NACK" =>     false
        }

    }

    import com.typesafe.config.ConfigFactory
    def createJobManagerActor(job: JobDescription, samplInfo: SamplingInformation) = {
        if (isSimulation) {
            val jobMangerSystem = ActorSystem.create("JobManagerAgent", ConfigFactory.load().getConfig("JobManagerAgent"))
            val newJobManager = jobMangerSystem.actorOf(Props(new JobManagerAgentAdvance(job.userId, job.jobId, samplInfo)), name = "JobManagerAgent")
            newJobManager ! new _JobManagerSimulationInitiate(job.tasks)
            jobManagerList = (newJobManager, jobMangerSystem) :: jobManagerList
        }
    }

    def Handle_AllocateContainerFromJM(message: _AllocateContainerFromJM) = {
        ignoreResourceSamplingResponseTimeoutEvent += 1
        if (startNewTasks(message._taskDescriptions, false) < message._taskDescriptions.length)
            nodeManager ! "ridi"
    }

    def startNewTasks(tasks: List[TaskDescription], isHeartBeatRespond: Boolean): Int = tasks match {
        case Nil     => 0
        case x :: xs => if (startANewTask(x, isHeartBeatRespond)) startNewTasks(xs, isHeartBeatRespond) + 1 else 0
    }

    def startANewTask(task: TaskDescription, isHeartBeatRespond: Boolean): Boolean = {
        val future = serverState.ask(new _AllocateContainer(task.userId, task.jobId, task.index, task.resource, task.duration, isHeartBeatRespond))(2 seconds).mapTo[String]
        future onSuccess { case "ACK" => return true }
        return false
    }

}