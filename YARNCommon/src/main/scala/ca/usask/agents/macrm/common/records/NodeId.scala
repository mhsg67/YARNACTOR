package ca.usask.agents.macrm.common.records

import akka.actor._
import java.io.Serializable

/*
 * TODO: defualt port should be read from config
 */
@SerialVersionUID(100L)
case class NodeId(val host: String = "0.0.0.0",
                  val port: Int = 0,
                  val agent: ActorRef = null)
    extends Serializable {

    override def equals(input: Any): Boolean = input match {
        case that: NodeId => this.agent match {
            case null => this.host == that.host && this.port == that.port
            case _    => this.agent == that.agent
        }
        case _ => false
    }

    override def toString() = "<host:" + host + " port:" + port.toString() + " hash: " + this.hashCode().toString() + s">"

}