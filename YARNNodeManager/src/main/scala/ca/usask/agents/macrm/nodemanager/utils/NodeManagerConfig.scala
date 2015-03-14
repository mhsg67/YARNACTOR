package ca.usask.agents.macrm.nodemanager.utils

import scala.concurrent.duration._
import org.joda.time._

/**
 * Holds the system configuration parameters for node manager project
 */
object NodeManagerConfig {

    def readConfigurationFile() = {
        Logger.Log("Start reading configuration file")
        Logger.Log("Finished reading configuration file")
    }

    lazy val getResourceTrackerAddress = "akka.tcp://ResourceTrackerAgent@" +
        trackerIPAddress + ":" +
        trackerDefualtPort + "/" +
        "user/ResourceTrackerAgent"

    /**
     * To access resourceTracker actor
     */
    val trackerIPAddress = "127.0.1.1"
    val trackerDefualtPort = "3000"

    /**
     * Based on YARN configuration we set heart beat interval
     * to RM to 1000
     */
    val heartBeatInterval = 2000 millis

    /**
     * Each node start to send heart beat 3000 millisecond
     * after booting
     */
    val heartBeatStartDelay = 1000 millis

    /**
     * The same as YARN we check containers every 3 sec and 
     * their resource consumption to control their limitation 
     */
    val checkContainersInterval = 3000 millis
    
    /**
     * In case we have resourceSamplingInquiry we check server state
     * to see if we can JobMaganer container request
     */
    val checkAvailableResource = 5 millis
    val firstCheckAvailableResources = 0 millis
    
    /**
     * When ContainerManager realizes that it can server container with the size in 
     * resource sampling inquiry it respond the JobManager and wait for its respond, 
     * if JobManger does not respond back in x millisecond ContainerManager start serving
     * other resource sampling inquiries  
     * 
     * suppose network delay is 2 millis =>
     * 2 millis = send resourceSmaplingResponse to JM
     * 6 millis = JM decision making
     * 2 millis = send resource allocation request back
     */
    val waitForJMActionToResourceSamplingResponseTimeout = 700 millis
    
    /**
     * Based on heartBeatStartDelay, it should be more than
     * that
     */
    val allCheckStartDelay = 1500 millis 
    
    /**
     * After sending heartBeat, containerManager stop serving resource sampling
     * inquiry since the ClusterManager may send the resource request to this node
     * to create container for running jobManager for new submitted job
     *
     * suppose network delay is 2 millis =>
     *  2 millis = send heartbeat from NM to RT
     *  2 millis = send heartbeat from RT to CM
     *  4 millis = make decision in CM
     *  2 millis = send allocation request from CM to NM
     *  ___
     *
     *  10 millis = total
     */
    val stopServingJobManagerRequestAfterHeartBeat = 10 
    val stopServingJobManagerRequestBeforeHeartBeat = 1000 -  waitForJMActionToResourceSamplingResponseTimeout.toMillis

}
