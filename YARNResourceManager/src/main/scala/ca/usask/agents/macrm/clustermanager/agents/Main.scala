package ca.usask.agents.macrm.clustermanager.agents

import akka.actor._
import ca.usask.agents.macrm.clustermanager.utils._
import com.typesafe.config.ConfigFactory

/**
 * It is the starter for ClusterManager
 */
object main extends App {
    try {
        ClusterManagerConfig.readConfigurationFile()

        val system = ActorSystem.create("ClusterManagerAgent", ConfigFactory.load().getConfig("ClusterManagerAgent"))
        val clusterManagerAgent = system.actorOf(Props[ClusterManagerAgent], name = "ClusterManagerAgent")
        
        clusterManagerAgent ! "initiateEvent"   
    }
    catch {
        case e: Exception => Logger.Error(e.toString())
    }
}