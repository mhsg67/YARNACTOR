package ca.usask.agents.macrm.common.records

import ca.usask.agents.macrm.common.records._
import java.io.Serializable

@SerialVersionUID(100L)
case class Constraint(val name: String,
                      val operator: Int,
                      val value: Int) extends Serializable {
    
    override def equals(input:Any):Boolean = input match {
        case that:Constraint => this.name == that.name && this.operator == that.operator && this.value == that.value 
        case _ => false
    }

    override def toString() = "<" + name + {
        operator match {
            case 0 => "="
            case 1 => "!="
            case 2 => "<"
            case 3 => ">"
        }
    } + value.toString() + ">"
}