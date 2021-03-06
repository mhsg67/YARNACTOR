package ca.usask.agents.yarnactor.resourcemanager.utils

import ca.usask.agents.yarnactor.common.records._

trait AbstractQueue {
    def EnqueueJob(e: JobDescription): Unit
    def EnqueueTask(e: TaskDescription)

    def RemoveJob(e: JobDescription)
    def RemoveTask(e: TaskDescription)

    def getFirstOrBestMatchJob(resource: Resource, capability: List[Constraint]): Option[JobDescription]
    def getFirtOrBestMatchTask(resource: Resource, capability: List[Constraint]): Option[TaskDescription]
}

object AbstractQueue {
    def apply(queuetype: String): AbstractQueue = queuetype match {
        case "FIFOQueue" => new FIFOQueue()
    }
}



