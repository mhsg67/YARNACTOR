package ca.usask.agents.macrm.jobmanager.utils

import ca.usask.agents.macrm.common.records._
import scala.collection.mutable._
import akka.actor._
import scala.util.Random

class SamplingManager {

    var samplingRate = 2
    var clusterNodes: List[NodeId] = null
    var waveToTasks = Map[Int, List[(Boolean, TaskDescription)]]() //The Int is waveNumber    
    var completedWave = 0

    def loadNewSamplingRate(newSamplingRate: Int) = samplingRate = newSamplingRate

    def loadSamplingInformation(samplingInformation: SamplingInformation) {
        samplingRate = samplingInformation.samplingRate
        clusterNodes = samplingInformation.clusterNodeWithoutConstraint
    }

    def getSamplingNode(tasks: List[TaskDescription], retry: Int): List[(NodeId, Resource)] = {
        val samplingRateForThisTry = if (retry > 0) samplingRate * math.pow(2, retry) else samplingRate
        val samplingCount = (samplingRateForThisTry.toInt * tasks.length)
        val minResource = new Resource(tasks.min(Ordering.by((x: TaskDescription) => x.resource.memory)).resource.memory,
            tasks.min(Ordering.by((x: TaskDescription) => x.resource.virtualCore)).resource.virtualCore)
        Random.shuffle(clusterNodes).take(samplingCount).map(x => (x, minResource))
    }

    def whichTaskShouldSubmittedToThisNode(lastWave: Int, resource: Resource): Option[List[TaskDescription]] = {
        val unscheduledTasks = getUnscheduledTasks(completedWave + 1, List())
        unscheduledTasks match {
            case List() => {
                completedWave = lastWave
                None
            }
            case _ => {
                val temp = getBigestTasksThatCanFitThisResource(resource, unscheduledTasks.sortWith((x, y) => x.resource > y.resource), List())
                temp match {
                    case List() => None
                    case _ => {
                        removeFromUnscheduledTasks(completedWave + 1, temp)
                        Some(temp)
                    }
                }
            }
        }
    }

    @annotation.tailrec
    final def getBigestTasksThatCanFitThisResource(resource: Resource, tasks: List[TaskDescription], result: List[TaskDescription]): List[TaskDescription] = tasks match {
        case List() => result
        case x :: xs =>
            if (x.resource < resource)
                getBigestTasksThatCanFitThisResource(resource - x.resource, xs, x :: result)
            else
                getBigestTasksThatCanFitThisResource(resource, xs, result)
    }

    @annotation.tailrec
    final def getUnscheduledTasks(waveNumber: Int, tasks: List[TaskDescription]): List[TaskDescription] = waveToTasks.get(waveNumber) match {
        case None    => tasks
        case Some(x) => getUnscheduledTasks(waveNumber + 1, getUnscheduledTaskOfWave(waveNumber) ++ tasks)
    }

    @annotation.tailrec
    final def removeFromUnscheduledTasks(waveNumber: Int, tasks: List[TaskDescription]):Unit = waveToTasks.get(waveNumber) match {
        case None    => ()
        case Some(x) => removeFromUnscheduledTasks(waveNumber + 1, removeUnscheduledTasksOfWave(waveNumber, tasks))
    }

    def removeUnscheduledTasksOfWave(waveNumber: Int, scheduledTasks: List[TaskDescription]): List[TaskDescription] = {
        val waveOldState = waveToTasks.get(waveNumber).get
        var waveNewState = List[(Boolean, TaskDescription)]()
        val scheduledTasksIndex = scheduledTasks.map(x => x.index)
        waveOldState.foreach { x =>
            if (scheduledTasksIndex.contains(x._2.index)) {
                (true, x._2) :: waveNewState
            }
            else {
                (x._1, x._2) :: waveNewState
            }
        }
        waveToTasks.update(waveNumber, waveNewState)
        scheduledTasks
    }

    def getUnscheduledTaskOfWave(waveNumber: Int) = waveToTasks.get(waveNumber).get.filter(x => x._1 == false).map(y => y._2)

    def addNewSubmittedTasksIntoWaveToTaks(waveNumber: Int, tasks: List[TaskDescription]) = waveToTasks.update(waveNumber, tasks.map(x => (false, x)))

}
