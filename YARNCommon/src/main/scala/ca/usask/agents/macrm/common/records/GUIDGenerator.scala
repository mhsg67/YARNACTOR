package ca.usask.agents.macrm.common.records

object GUIDGenerator {
    def getNextGUID = java.util.UUID.randomUUID().toString()
}