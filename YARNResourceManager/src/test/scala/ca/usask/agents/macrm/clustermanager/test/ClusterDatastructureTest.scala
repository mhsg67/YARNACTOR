package ca.usask.agents.macrm.clustermanager.test

import ca.usask.agents.macrm.clustermanager.utils._
import ca.usask.agents.macrm.common.records._
import org.joda.time._

class ClusterDatastructureTest extends UnitSpec {

    "updateClusterStructure" should "change newSamplingRate && add new Servers && remove some Servers" in {

        val nId1 = NodeId("0.0.0.1", 1)
        val nId2 = NodeId("0.0.0.1", 2)
        val nId3 = NodeId("0.0.0.1", 3)
        val nId4 = NodeId("0.0.0.2", 5)
        val nId5 = NodeId("0.0.0.2", 1)
        val nId6 = NodeId("0.0.0.3", 4)
        val nId7 = NodeId("0.0.0.2", 5)

        val n1 = (nId1, null)
        val n2 = (nId2, null)
        val n3 = (nId3, null)
        val n4 = (nId4, null)
        val n5 = (nId5, null)
        val n6 = (nId6, null)
        val newN1 = (nId1, List(new Constraint("1", 0, 1)))
        val newN4 = (nId7, List(new Constraint("1", 1, 1)))

        val cs = new ClusterDatastructure()

        cs.updateClusterStructure(-1, null, null, null)
        cs.samplingRate should be(2)
        cs.clusterNodes should be(List())

        cs.updateClusterStructure(4, null, null, null)
        cs.samplingRate should be(4)
        cs.clusterNodes should be(List())

        cs.updateClusterStructure(-1, null, List(n1, n2, n3, n4), null)
        cs.samplingRate should be(4)
        cs.clusterNodes.length should be(4)
        cs.clusterNodes(0)._1.host should be("0.0.0.1")

        cs.updateClusterStructure(-1, List(nId2), null, null)
        cs.samplingRate should be(4)
        cs.clusterNodes.length should be(3)
        cs.clusterNodes should not contain (n2)

        cs.updateClusterStructure(8, null, List(newN1), null)
        cs.samplingRate should be(8)
        cs.clusterNodes.length should be(3)
        cs.clusterNodes should not contain (n1)

        cs.updateClusterStructure(-1, List(nId3), List(n5, n6, newN4), null)
        cs.samplingRate should be(8)
        cs.clusterNodes.length should be(4)
        cs.clusterNodes should not contain (n4)
        cs.clusterNodes should not contain (n3)
        cs.clusterNodes should contain(n6)
    }

    "doseCapabilityMatchConstraint" should "return right boolean value" in {

        val co1 = new Constraint("1", 0, 1)
        val co2 = new Constraint("1", 1, 1)
        val co3 = new Constraint("1", 2, 4)
        val co4 = new Constraint("1", 3, 0)
        val co5 = new Constraint("1", 2, 1)
        val co6 = new Constraint("1", 3, 3)

        val cp1 = new Constraint("1", 0, 1)
        val cs = new ClusterDatastructure()

        var result = cs.doseCapabilityMatchConstraint(cp1, co1)
        result should be(true)

        result = cs.doseCapabilityMatchConstraint(cp1, co2)
        result should be(false)

        result = cs.doseCapabilityMatchConstraint(cp1, co3)
        result should be(true)

        result = cs.doseCapabilityMatchConstraint(cp1, co4)
        result should be(true)

        result = cs.doseCapabilityMatchConstraint(cp1, co5)
        result should be(false)

        result = cs.doseCapabilityMatchConstraint(cp1, co6)
        result should be(false)

        result = cs.doseCapabilityMatchConstraint(cp1, co1)
        result should be(true)
    }

    "getMatchCapablitiesOfNode" should "return list of node with related capablity" in {

        val co1 = new Constraint("1", 0, 1)
        val co2 = new Constraint("1", 0, 2)
        val co3 = new Constraint("2", 0, 3)
        val co4 = new Constraint("2", 0, 3)
        val co5 = new Constraint("3", 0, 5)
        val co6 = new Constraint("3", 0, 6)
        val co7 = new Constraint("4", 0, 0)
        val co8 = new Constraint("5", 0, 1)

        val cp1 = new Constraint("1", 1, 1)
        val cp2 = new Constraint("2", 2, 4)
        val cp3 = new Constraint("3", 3, 5)

        val nId1 = NodeId("0.0.0.1", 1)
        val nId2 = NodeId("0.0.0.1", 2)
        val nId3 = NodeId("0.0.0.1", 3)
        val nId4 = NodeId("0.0.0.2", 5)
        val nId5 = NodeId("0.0.0.2", 1)
        val nId6 = NodeId("0.0.0.3", 4)
        val nId7 = NodeId("0.0.0.4", 5)
        val nId8 = NodeId("0.0.0.4", 6)

        val n1 = (nId1, List(co7))
        val n2 = (nId2, List(co1, co8))
        val n3 = (nId3, List(co2))
        val n4 = (nId4, List(co2, co4, co8))
        val n5 = (nId5, List(co3, co8))
        val n6 = (nId6, List(co4, co2))
        val n7 = (nId7, List(co5, co1, co3, co7))
        val n8 = (nId8, List(co6, co2, co3, co7))

        val cs = new ClusterDatastructure()
        cs.updateClusterStructure(4, null, List(n1, n2, n3, n4, n5, n6, n7, n8), null)
        var result = cs.getCurrentSamplingInformation(List(cp1, cp2, cp3))
        result.samplingRate should be(4)
        result.clusterNodes.length should be(8)
        result.clusterNodes(0) should be((nId1, null))
        result.clusterNodes(1) should be((nId2, null))
        result.clusterNodes(2) should be((nId3, List(co2)))
        result.clusterNodes(3) should be((nId4, List(co2, co4)))
        result.clusterNodes(6) should be((nId7, List(co3)))
        result.clusterNodes(7) should be((nId8, List(co6, co2, co3)))

        cs.updateClusterStructure(-1, null, null, List((true, co7)))
        result = cs.getCurrentSamplingInformation(List(cp1, cp2, cp3))
        result.samplingRate should be(4)
        result.clusterNodes.length should be(7)
        result.clusterNodes(0) should be((nId2, null))
        result.clusterNodes(1) should be((nId3, List(co2)))
        result.clusterNodes(2) should be((nId4, List(co2, co4)))
        result.clusterNodes(5) should be((nId7, List(co3)))
        result.clusterNodes(6) should be((nId8, List(co6, co2, co3)))

        cs.updateClusterStructure(-1, null, null, List((false, co7), (true, co8)))
        result = cs.getCurrentSamplingInformation(List(cp1, cp2, cp3))
        result.samplingRate should be(4)
        result.clusterNodes.length should be(7)
        cs.rareResource.length should be(1)
        cs.rareResource(0).name should be("5")
        cs.rareResource(0).value should be(1)
        result.clusterNodes should contain((nId1, null))
        result.clusterNodes should contain((nId3, List(co2)))
        result.clusterNodes should contain((nId4, List(co2, co4)))
        result.clusterNodes should contain((nId7, List(co3)))
        result.clusterNodes should contain((nId8, List(co6, co2, co3)))
        result.clusterNodes should not contain ((nId2, null))

        cs.updateClusterStructure(-1, null, null, List((true, co7)))
        result = cs.getCurrentSamplingInformation(List(cp1, cp2, cp3))
        result.clusterNodes.length should be(6)
        cs.rareResource.length should be(2)
        result.clusterNodes should contain((nId3, List(co2)))
        result.clusterNodes should contain((nId4, List(co2, co4)))
        result.clusterNodes should contain((nId7, List(co3)))
        result.clusterNodes should contain((nId8, List(co6, co2, co3)))
        result.clusterNodes should not contain ((nId2, null))
        result.clusterNodes should not contain ((nId1, null))
    }

}