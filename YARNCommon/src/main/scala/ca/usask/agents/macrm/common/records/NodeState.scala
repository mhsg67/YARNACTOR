package ca.usask.agents.macrm.common.records

import java.io.Serializable

sealed class NodeState

@SerialVersionUID(100L)
case object NEW extends NodeState with Serializable

@SerialVersionUID(100L)
case object RUNNING extends NodeState with Serializable

@SerialVersionUID(100L)
case object UNHEALTHY extends NodeState with Serializable

@SerialVersionUID(100L)
case object DECOMMISSIONED extends NodeState with Serializable

@SerialVersionUID(100L)
case object LOST extends NodeState with Serializable

@SerialVersionUID(100L)
case object REBOOTED extends NodeState with Serializable

object NodeState {
    def isUnusable(input: NodeState): Boolean = {
        if (input == UNHEALTHY || input == DECOMMISSIONED || input == LOST)
            return true
        else
            return false;
    }

    def apply(input: String): NodeState = input match {
        case "NEW"            => NEW
        case "RUNNING"        => RUNNING
        case "UNHEALTHY"      => UNHEALTHY
        case "DECOMMISSIONED" => DECOMMISSIONED
        case "LOST"           => LOST
        case "REBOOTED"       => REBOOTED
    }
}
