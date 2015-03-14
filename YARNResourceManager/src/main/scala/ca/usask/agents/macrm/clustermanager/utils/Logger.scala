package ca.usask.agents.macrm.clustermanager.utils

import ca.usask.agents.macrm.common.agents._
import org.joda.time._

object Logger extends DebugingLogger {

    override def Log(message: String) = {
        println("L  " + DateTime.now() + ": " + message)
    }
    
    override def Error(message:String) = {
        println("E  " + DateTime.now() + ": " + message)
    }
}