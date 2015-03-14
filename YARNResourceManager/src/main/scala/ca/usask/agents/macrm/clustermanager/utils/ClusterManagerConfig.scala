package ca.usask.agents.macrm.clustermanager.utils

/**
 * Holds the system configuration parameters for central resource manager project
 */
object ClusterManagerConfig {

    def readConfigurationFile() = {
        Logger.Log("Start reading configuration file")
        Logger.Log("Finished reading configuration file")
    }

    def getResourceTrackerAddress() = "akka.tcp://ResourceTrackerAgent@" +
        trackerIPAddress + ":" +
        trackerDefualtPort + "/" +
        "user/ResourceTrackerAgent"

    /**
     * To access resourceTracker actor
     */
    val trackerIPAddress = "127.0.1.1"
    val trackerDefualtPort = "3000"

    /**
     * The default system queue is FIFO Queue
     */
    def QueueType = "FIFOQueue"
    
    /**
     * Container size for a job manager
     * 1000 memory = 1GB memory
     * 1 virtual core
     */
     val jobManagerContainerMemorySize = 1000 
     val jobManagerContainerVirtualCoreSize = 1
}