package ca.usask.agents.macrm.nodemanager.agents

import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.common.agents._
import akka.actor._
import ca.usask.agents.macrm.nodemanager.utils._
import com.typesafe.config.ConfigFactory

/**
 * It is the starter for node manager; this will create a NodeManagerAgent
 * and all the other services will be handle by that actor
 */
object main extends App {
    try {
        NodeManagerConfig.readConfigurationFile()

        val system1 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        val system2 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        val system3 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        val system4 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        val system5 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        val system6 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        val system7 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        val system8 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        val system9 = ActorSystem.create("NodeManagerAgent", ConfigFactory.load().getConfig("NodeManagerAgent"))
        
        val nodeManager1 = system1.actorOf(Props(new NodeManagerAgent(1)), name = "NodeManagerAgent")        
        val nodeManager2 = system2.actorOf(Props(new NodeManagerAgent(2)), name = "NodeManagerAgent")
        val nodeManager3 = system3.actorOf(Props(new NodeManagerAgent(3)), name = "NodeManagerAgent")
        val nodeManager4 = system4.actorOf(Props(new NodeManagerAgent(4)), name = "NodeManagerAgent")
        val nodeManager5 = system5.actorOf(Props(new NodeManagerAgent(5)), name = "NodeManagerAgent")
        val nodeManager6 = system6.actorOf(Props(new NodeManagerAgent(6)), name = "NodeManagerAgent")
        val nodeManager7 = system7.actorOf(Props(new NodeManagerAgent(7)), name = "NodeManagerAgent")
        val nodeManager8 = system8.actorOf(Props(new NodeManagerAgent(8)), name = "NodeManagerAgent")
        val nodeManager9 = system9.actorOf(Props(new NodeManagerAgent(9)), name = "NodeManagerAgent")               
        
        nodeManager1 ! new _NodeManagerSimulationInitiate(new Resource(1000,2), List())
        nodeManager2 ! new _NodeManagerSimulationInitiate(new Resource(1000,2), List())
        nodeManager3 ! new _NodeManagerSimulationInitiate(new Resource(2000,2), List())
        nodeManager4 ! new _NodeManagerSimulationInitiate(new Resource(2000,4), List())
        nodeManager5 ! new _NodeManagerSimulationInitiate(new Resource(4000,2), List())
        nodeManager6 ! new _NodeManagerSimulationInitiate(new Resource(4000,4), List())
        nodeManager7 ! new _NodeManagerSimulationInitiate(new Resource(4000,8), List())
        nodeManager8 ! new _NodeManagerSimulationInitiate(new Resource(8000,4), List())
        nodeManager9 ! new _NodeManagerSimulationInitiate(new Resource(8008,8), List())        
    }
    catch {
        case e: Exception => Logger.Error(e.toString())
    }
}
