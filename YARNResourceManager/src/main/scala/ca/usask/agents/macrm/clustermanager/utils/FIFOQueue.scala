package ca.usask.agents.macrm.clustermanager.utils

import ca.usask.agents.macrm.common.records._
import ca.usask.agents.macrm.common.agents._
import scala.collection.mutable._
import akka.actor._

class FIFOQueue extends AbstractQueue {

    var JobQueue = new MutableList[JobDescription]()

    var TaskQueue = new MutableList[TaskDescription]()

    def EnqueueJob(e: JobDescription) = JobQueue += e

    def EnqueueTask(e: TaskDescription) = TaskQueue += e

    def RemoveJob(e: JobDescription) = JobQueue = JobQueue.filter(x => x.jobId != e.jobId)

    def RemoveTask(e: TaskDescription) = TaskQueue = TaskQueue.filter(x => (x.jobId != e.jobId && x.index != e.index))

    def getFirstOrBestMatchJob(resource: Resource, capability: List[Constraint]): Option[JobDescription] = JobQueue match {
        case MutableList() => None
        case _             => JobQueue.find(x => doesJobDescriptionMatch(resource, capability, x))
    }

    def getFirtOrBestMatchTask(resource: Resource, capability: List[Constraint]): Option[TaskDescription] = TaskQueue match {
        case MutableList() => None
        case _             => TaskQueue.find(x => doesTaskDescriptionMatch(resource, capability, x))
    }

    def doesJobDescriptionMatch(resource: Resource, capability: List[Constraint], jobDescription: JobDescription) = {
        if (jobDescription.numberOfTasks != 1)
            true
        else if (jobDescription.constraints.isEmpty && jobDescription.tasks(0).resource < resource)
            true
        else if (!jobDescription.constraints.isEmpty && capability.isEmpty)
            false
        else if (jobDescription.constraints.foldLeft(true)((x, y) => doesConstraintMatch(capability, y) && x) && jobDescription.tasks(0).resource < resource)
            true
        else
            false
    }

    def doesTaskDescriptionMatch(resource: Resource, capability: List[Constraint], taskDescription: TaskDescription) = {
        if (taskDescription.constraints.isEmpty && taskDescription.resource < resource)
            true
        else if (capability.isEmpty)
            false
        else if (taskDescription.constraints.foldLeft(true)((x, y) => doesConstraintMatch(capability, y) && x) && taskDescription.resource < resource)
            true
        else
            false
    }

    def doesConstraintMatch(resourceCapabilityList: List[Constraint], taskConstraint: Constraint) = resourceCapabilityList.find(x => x.name == taskConstraint.name) match {
        case None => false
        case Some(x) =>
            taskConstraint.operator match {
                case 0 => if (x.value == taskConstraint.value) true else false
                case 1 => if (x.value != taskConstraint.value) true else false
                case 2 => if (x.value < taskConstraint.value) true else false
                case 3 => if (x.value > taskConstraint.value) true else false
            }
    }
}