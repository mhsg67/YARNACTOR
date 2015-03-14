package ca.usask.agents.macrm.jobmanager.agents

import ca.usask.agents.macrm.jobmanager.utils._
import ca.usask.agents.macrm.common.records._
import com.typesafe.config.ConfigFactory
import akka.actor._

/**
 * It is the starter for JobManager agent
 */
object main extends App {
    try {
        JobManagerConfig.readConfigurationFile()

        val system = ActorSystem.create("JobManagerAgent", ConfigFactory.load().getConfig("JobManagerAgent"))
        //val jobManager = system.actorOf(Props(new JobManagerAgent(JobManagerConfig.userId, JobManagerConfig.jobId)), name = "JobManagerAgent")

        //jobManager ! "initiateEvent"

    }
    catch {
        case e: Exception => Logger.Error(e.toString())
    }
}
