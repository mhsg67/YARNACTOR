package ca.usask.agents.macrm.common.records

import java.io.Serializable

@SerialVersionUID(100L)
class Utilization(val memoryUtilization: Double = 0.0, val virtualCoreUtilization: Double = 0.0)
    extends Serializable {

    override def equals(input: Any): Boolean = input match {
        case that: Utilization => this.memoryUtilization == that.memoryUtilization &&
            this.virtualCoreUtilization == that.virtualCoreUtilization
        case _ => false
    }

    override def toString() = "<memUtil:" + memoryUtilization.toString() + " ,cpuUtil:" + virtualCoreUtilization.toString() + ">"
}
