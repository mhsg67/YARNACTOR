package ca.usask.agents.macrm.common.records

import java.io.Serializable

@SerialVersionUID(100L)
class JobReport(val userId: Int, val jobId: Long, val successfulSamplingRate: Int) extends Serializable

@SerialVersionUID(100L)
class JobReportAdvance(val userId: Int, val jobId: Long, val successfulSamplingRateToConstraint: List[(Int, List[Constraint])]) extends Serializable