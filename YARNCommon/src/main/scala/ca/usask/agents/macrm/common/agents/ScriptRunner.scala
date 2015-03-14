package ca.usask.agents.macrm.common.agents

import scala.sys.process._

object ScriptRunner {
    def runScript(_input: String): String = _input match {
        case "ls" => "python Scripts/script.py".!!
        case _    => ""
    }
}
