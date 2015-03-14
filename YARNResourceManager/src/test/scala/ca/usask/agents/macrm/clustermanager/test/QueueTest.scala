package ca.usask.agents.macrm.clustermanager.test

import ca.usask.agents.macrm.clustermanager.utils._
import ca.usask.agents.macrm.common.records._
import org.scalatest._

class QueueTest extends UnitSpec {

    "getFirstOrBestMatchJob" should "get the first element of the queue" in {
        val queue = AbstractQueue("FIFOQueue")

        val task1 = new TaskDescription(null, 1, 0, new org.joda.time.Duration(100), new Resource(1, 250), new org.joda.time.Duration(2), List())
        val task2 = new TaskDescription(null, 1, 1, new org.joda.time.Duration(200), new Resource(1, 250), new org.joda.time.Duration(2), List())
        val job1 = new JobDescription(1, 1, 2, List(task1, task2))
        queue.EnqueueJob(job1)

        val resource = new Resource(3, 1000)
        val capability = List()
        val result = queue.getFirstOrBestMatchJob(resource, capability)
        result should not be None
        result.get.numberOfTasks should be(2)
    }

    "doesJobDescriptionMatch" should "return right values" in {
        val queue = AbstractQueue("FIFOQueue")

        val const1 = new Constraint("1", 0, 1)

        val task1 = new TaskDescription(null, 1, 0, new org.joda.time.Duration(100), new Resource(1, 250), new org.joda.time.Duration(2), List(const1))
        val job1 = new JobDescription(1, 1, 1, List(task1))
        queue.EnqueueJob(job1)

        val resource = new Resource(3, 1000)
        val capability0 = List()
        var result = queue.asInstanceOf[FIFOQueue].doesJobDescriptionMatch(resource, capability0, job1)
        result should be(false)

        val const2 = new Constraint("1", 0, 2)
        val capability1 = List(const2)
        result = queue.asInstanceOf[FIFOQueue].doesJobDescriptionMatch(resource, capability1, job1)
        result should be(false)

        val capability2 = List(const1)
        result = queue.asInstanceOf[FIFOQueue].doesJobDescriptionMatch(resource, capability2, job1)
        result should be(true)
    }

    "RemoveJob" should "should remove inserted job" in {
        val queue = AbstractQueue("FIFOQueue")

        val task1 = new TaskDescription(null, 1, 0, new org.joda.time.Duration(100), new Resource(1, 250), new org.joda.time.Duration(2), null)
        val task2 = new TaskDescription(null, 1, 1, new org.joda.time.Duration(200), new Resource(1, 250), new org.joda.time.Duration(2), null)
        val job1 = new JobDescription(1, 1, 2, List(task1, task2))
        queue.EnqueueJob(job1)

        val resource = new Resource(3, 1000)
        val capability = List()
        var result = queue.getFirstOrBestMatchJob(resource, capability)
        result should not be (None)
        queue.RemoveJob(result.get)
        result = queue.getFirstOrBestMatchJob(resource, capability)
        result should be(None)
    }

    "doesConstraintMatch" should "work well with all input permutations" in {
        val queue = AbstractQueue("FIFOQueue")

        // suppose constraint "1" has 3 possible values (1,2,3)
        val taskConst1 = new Constraint("1", 0, 1)
        val taskConst2 = new Constraint("1", 1, 1)
        val taskConst3 = new Constraint("1", 2, 3)
        val taskConst4 = new Constraint("1", 3, 1)
        val taskConst5 = new Constraint("1", 2, 1) //Alwasy false
        val taskConst6 = new Constraint("1", 3, 3) //Always false
        val taskConst7 = new Constraint("1", 2, 4) //Always true
        val taskConst8 = new Constraint("1", 3, 0) //Alsways true

        val capability1 = new Constraint("1", 0, 1)
        val capability2 = new Constraint("1", 0, 2)
        val capability3 = new Constraint("1", 0, 3)
        val capability4 = new Constraint("2", 0, 1)

        var capabilityList = List[Constraint]()
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst1) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst2) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst3) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst4) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst5) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst6) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst7) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst8) should be(false)

        capabilityList = List(capability1)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst1) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst2) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst3) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst4) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst5) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst6) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst7) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst8) should be(true)

        capabilityList = List(capability2, capability3)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst1) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst2) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst3) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst4) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst5) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst6) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst7) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst8) should be(true)

        capabilityList = List(capability2, capability3, capability4)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst1) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst2) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst3) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst4) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst5) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst6) should be(false)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst7) should be(true)
        queue.asInstanceOf[FIFOQueue].doesConstraintMatch(capabilityList, taskConst8) should be(true)

    }
}