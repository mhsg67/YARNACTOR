package ca.usask.agents.yarnactor.common.records

object GUIDGenerator {
    def getNextGUID = java.util.UUID.randomUUID().toString()
}