package ca.usask.agents.macrm.jobmanager.utils

import ca.usask.agents.macrm.common.records._
import scala.collection.mutable._
import akka.actor._
import scala.util.Random

object SamplingManagerAdvance {

    var samplingRate = 2
    var clusterNodesWithConstraint: List[(NodeId, List[Constraint])] = null
    var clusterNodesWithoutConstraint: List[NodeId] = null
    var waveToTasks = Map[Int, List[(Boolean, TaskDescription)]]() //The Int is waveNumber
    var nodeIdToTaskIndex = Map[NodeId, List[Int]]()
    var completedWave = 0

    def loadNewSamplingRate(newRate: Int) = samplingRate = newRate

    def loadSamplingInformation(samplingInformation: SamplingInformation) {
        samplingRate = samplingInformation.samplingRate
        clusterNodesWithConstraint = samplingInformation.clusterNodesWithConstraint
        clusterNodesWithoutConstraint = samplingInformation.clusterNodeWithoutConstraint
    }

    def getSamplingNode(tasks: List[TaskDescription], retry: Int): List[(NodeId, Resource)] = {
        val samplingRateForThisTry = if (retry > 0) samplingRate * math.pow(2, retry) else samplingRate
        val (tasksWithConstraint, tasksWithoutConstraint) = tasks.partition(x => x.constraints != null || x.constraints != List())
        val (currentSamplingCount, resultPart1) = findSamplingListForTasksWithConstraint(samplingRateForThisTry.toInt, tasks)
        val remainingSamplingCount = (samplingRateForThisTry.toInt * tasks.length) - currentSamplingCount
        val minResource = new Resource(tasksWithoutConstraint.min(Ordering.by((x: TaskDescription) => x.resource.memory)).resource.memory,
            tasksWithoutConstraint.min(Ordering.by((x: TaskDescription) => x.resource.virtualCore)).resource.virtualCore)
        val resultPart2 = Random.shuffle(clusterNodesWithoutConstraint).take(remainingSamplingCount).map(x => (x, minResource))

        val result = resultPart1 ++ resultPart2
        result
    }

    def findSamplingListForTasksWithConstraint(SRFTT: Int, tasks: List[TaskDescription]): (Int, List[(NodeId, Resource)]) = {

        var tempNodeIdToTaskIndex = Map[NodeId, List[TaskDescription]]()
        tasks.foreach { x =>
            val properNodesForThisTask = findNodesWithProperCapabilities(x.constraints)
            properNodesForThisTask.foreach(y => tempNodeIdToTaskIndex.get(y) match {
                case None    => tempNodeIdToTaskIndex.update(y, List(x))
                case Some(z) => tempNodeIdToTaskIndex.update(y, x :: z)
            })
        }

        var finalResult1 = 0
        var taskIndexToCurrSampNum = Map[Int, Int]()
        tasks.foreach(x => taskIndexToCurrSampNum.update(x.index, 0))

        var finalResult2 = List[(NodeId, Resource)]()
        val tempNodeIdToTaskIndexList = tempNodeIdToTaskIndex.toList.sortWith((x, y) => x._2.length > y._2.length).
            map(x => (x._1, x._2.sortWith((m, n) => m.resource < n.resource)))

        tempNodeIdToTaskIndexList.foreach { x =>
            var selected = false
            x._2.foreach { y =>
                if (!selected && taskIndexToCurrSampNum.get(y.index).get < SRFTT) {
                    finalResult1 = 1 + finalResult1
                    finalResult2 = (x._1, y.resource) :: finalResult2
                    taskIndexToCurrSampNum.update(y.index, taskIndexToCurrSampNum.get(y.index).get + 1)
                    selected = true
                    nodeIdToTaskIndex.update(x._1, x._2.map(j => j.index))
                    tempNodeIdToTaskIndex.remove(x._1)
                }
            }
        }

        if (finalResult1 < tasks.length * SRFTT) {
            Random.shuffle(tempNodeIdToTaskIndex.toList).take(tasks.length * SRFTT - finalResult1).foreach { x =>
                val sortedTasksBasedOnResource = x._2.sortWith((m, n) => m.resource < n.resource)
                finalResult1 = 1 + finalResult1
                finalResult2 = (x._1, sortedTasksBasedOnResource(0).resource) :: finalResult2
                nodeIdToTaskIndex.update(x._1, sortedTasksBasedOnResource.map(y => y.index))
            }
        }

        (finalResult1, finalResult2)
    }

    def findNodesWithProperCapabilities(constrints: List[Constraint]): List[NodeId] = {
        val tempListOfNodeId = clusterNodesWithConstraint.filter(x => doesAllCapablitiesMatchAllConstraints(x._2, constrints))
        tempListOfNodeId.map(x => x._1)
    }

    def getBestTaskTachCanFitThisResource(resource: Resource, tasks: List[TaskDescription], order: List[Int], result: List[TaskDescription]): List[TaskDescription] = order match {
        case List() => result
        case x :: xs => {
            val taskDescTemp = tasks.find(y => y.index == x)
            taskDescTemp match {
                case None => getBestTaskTachCanFitThisResource(resource, tasks, xs, result)
                case Some(x) => if (x.resource < resource)
                    getBestTaskTachCanFitThisResource(resource - x.resource, tasks, xs, x :: result)
                else
                    getBestTaskTachCanFitThisResource(resource, tasks, xs, result)
            }
        }
    }

    def whichTaskShouldSubmittedToThisNode(lastWave: Int, resource: Resource, nodeId: NodeId): Option[List[TaskDescription]] = {
        val unscheduledTasks = getUnscheduledTasks(completedWave + 1, List())
        var result: Option[List[TaskDescription]] = None

        if (unscheduledTasks == List()) {
            completedWave = lastWave
            result = None
        }
        else {
            if (nodeIdToTaskIndex.get(nodeId) != None) {
                val unscheduledTasksIndexesForThisNodeId = nodeIdToTaskIndex.get(nodeId).get.filter(x => unscheduledTasks.exists(y => y.index == x))
                if (unscheduledTasksIndexesForThisNodeId.length > 0) {
                    val unscheduledTasksIndexSortedBasedOnLowestPossibleOptions = nodeIdToTaskIndex.toList.map(x => x._2).flatten.filter(x => unscheduledTasksIndexesForThisNodeId.contains(x)).groupBy(x => x).toList.sortWith((x, y) => x._2.length < y._2.length).map(x => x._1)
                    val temp = getBestTaskTachCanFitThisResource(resource, unscheduledTasks, unscheduledTasksIndexSortedBasedOnLowestPossibleOptions, List())
                    temp match {
                        case List() => result = None
                        case _      => result = Some(temp)
                    }
                }
                else
                    None //It's resource with constraint let preserver it
                nodeIdToTaskIndex.remove(nodeId)
            }
            else {
                val unscheduledTasksWithOutConstraint = unscheduledTasks.filter(x => x.constraints == null || x.constraints == List()).sortWith((x, y) => x.resource > y.resource)
                //TODO:it can be knapstak 
                val temp = getBigestTasksThatCanFitThisResource(resource, unscheduledTasksWithOutConstraint, List())
                temp match {
                    case List() => result = None
                    case _      => result = Some(temp)
                }
            }
        }
        result
    }

    @annotation.tailrec
    def getBigestTasksThatCanFitThisResource(resource: Resource, tasks: List[TaskDescription], result: List[TaskDescription]): List[TaskDescription] = tasks match {
        case List() => result
        case x :: xs => if (x.resource < resource)
            getBigestTasksThatCanFitThisResource(resource - x.resource, xs, x :: result)
        else
            getBigestTasksThatCanFitThisResource(resource, xs, result)
    }

    def doesAllCapablitiesMatchAllConstraints(cap: List[Constraint], con: List[Constraint]): Boolean = {
        con.foldLeft(true)((x, y) => doesCapablitiesMatchTheConstraint(cap, y) && x)
    }

    def doesCapablitiesMatchTheConstraint(cap: List[Constraint], con: Constraint): Boolean = {
        val temp = cap.filter(x => x.name == con.name)
        temp match {
            case List() => false
            case _      => doseCapabilityMatchConstraint(temp(0), con)
        }
    }

    def doseCapabilityMatchConstraint(cap: Constraint, con: Constraint) = con.operator match {
        case 0 => if (cap.value == con.value) true else false
        case 1 => if (cap.value != con.value) true else false
        case 2 => if (cap.value < con.value) true else false
        case 3 => if (cap.value > con.value) true else false
    }

    def getUnscheduledTasks(waveNumber: Int, tasks: List[TaskDescription]): List[TaskDescription] = waveToTasks.get(waveNumber) match {
        case None    => tasks
        case Some(x) => getUnscheduledTasks(waveNumber + 1, getUnscheduledTaskOfWave(waveNumber) ++ tasks)
    }

    def getUnscheduledTaskOfWave(waveNumber: Int) = waveToTasks.get(waveNumber).get.filter(x => x._1 == false).map(y => y._2)
    
     def addNewSubmittedTasksIntoWaveToTaks(waveNumber: Int, tasks: List[TaskDescription]) = {
        waveToTasks.update(waveNumber, tasks.map(x => (false, x)))
    }
    
    //TODO:should be implemented
    def canFindProperResourcesForTheseConstraints(constraints: List[Constraint]): Boolean = true
}