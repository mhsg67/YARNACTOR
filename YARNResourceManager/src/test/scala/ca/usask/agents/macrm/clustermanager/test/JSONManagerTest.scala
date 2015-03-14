package ca.usask.agents.macrm.clustermanager.test

import ca.usask.agents.macrm.clustermanager.utils._
import ca.usask.agents.macrm.common.records._
import org.joda.time._

class JSONManagerTest extends UnitSpec{
    
    "getJobDescription" should "return JobDescription Object" in {        
        
        val result:Either[String, JobDescription] = JSONManager.getJobDescription("""{"JI": 1,"UI": 1,"TS":[{"INX":0,"DUR":100,"RST":2,"CPU":1,"MEM":250,"PRI":0,"TSC":1},{"INX":1,"DUR":100,"RST":2,"CPU":1,"MEM":250,"PRI":0,"TSC":1}],"CS": []}""")
        
        result should be ('right)
        result.right.get.jobId should be (1)
        result.right.get.userId should be (1)
        result.right.get.numberOfTasks should be (2)
        result.right.get.constraints should be (List())
        result.right.get.tasks(0) shouldBe a [TaskDescription]
        result.right.get.tasks(0).index should be (0)
        result.right.get.tasks(0).duration should be (new Duration(0))
        result.right.get.tasks(0).relativeSubmissionTime should be (new Duration(0))
        result.right.get.tasks(0).resource shouldBe a [Resource]
        result.right.get.tasks(0).resource.memory should be (1000)
        result.right.get.tasks(0).resource.virtualCore should be (1)
        result.right.get.tasks(0).constraints should be (List())
        
        result.right.get.tasks(1) shouldBe a [TaskDescription]
        result.right.get.tasks(1).index should be (0)
        result.right.get.tasks(1).duration should be (new Duration(100))
        result.right.get.tasks(1).relativeSubmissionTime should be (new Duration(2))
        result.right.get.tasks(1).resource shouldBe a [Resource]
        result.right.get.tasks(1).resource.memory should be (250)
        result.right.get.tasks(1).resource.virtualCore should be (1)
        result.right.get.tasks(1).constraints should be (List())
    }
    
    "getJobDescription" should "return JobDescription with constraints" in {  
        
        val result:Either[String, JobDescription] = JSONManager.getJobDescription("""{"JI": 1,"UI": 1,"TS":[{"INX":0,"DUR":100,"RST":2,"CPU":1,"MEM":250,"PRI":0,"TSC":1},{"INX":1,"DUR":100,"RST":2,"CPU":1,"MEM":250,"PRI":0,"TSC":1},{"INX":2,"DUR":200,"RST":3,"CPU":1,"MEM":500,"PRI":0,"TSC":1}],"CS":[{"INX":0,"N":"1","OP":0,"V":1},{"INX":0,"N":"2","OP":0,"V":2},{"INX":0,"N":"3","OP":0,"V":3},{"INX":1,"N":"1","OP":0,"V":2},{"INX":1,"N":"2","OP":0,"V":1},{"INX":2,"N":"1","OP":0,"V":3}]}""")
        
        result should be ('right)
        result.right.get.constraints.length should be (6)
        result.right.get.tasks(1).constraints.length should be (3)
        result.right.get.tasks(1).constraints(0) shouldBe a [Constraint]
        result.right.get.tasks(1).constraints(0).name should be ("1")
        result.right.get.tasks(1).constraints(0).operator should be (0)
        result.right.get.tasks(1).constraints(0).value should be (1)
        result.right.get.tasks(2).constraints.length should be (2)
        result.right.get.tasks(3).constraints.length should be (1)        
    }
}