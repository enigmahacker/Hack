import Foundation
import SQLite

// Swift also declares `Expression` in Foundation; SQLite's version collides.
// Resolve the collision at point of use by qualifying `SQLite.Expression`.

// MARK: - Failure Record

public struct FailureRecord: Codable {
    public let documentID: String
    public let stage: String
    public let errorMessage: String
    public let timestamp: Date
    public var retryCount: Int

    public init(documentID: String, stage: String, errorMessage: String, timestamp: Date = Date(), retryCount: Int = 0) {
        self.documentID = documentID
        self.stage = stage
        self.errorMessage = errorMessage
        self.timestamp = timestamp
        self.retryCount = retryCount
      }
}

public class FailureStore {
    private let db: Connection
    private let table: Table
    private let cols: (id: SQLite.Expression<String>, stage: SQLite.Expression<String>, error: SQLite.Expression<String>,
                       timestamp: SQLite.Expression<Date>, retryCount: SQLite.Expression<Int>)

    public init(path: URL) throws {
        let dir = path.deletingLastPathComponent()
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        self.db = try Connection(path.path)
        self.db.busyTimeout = 5.0
        self.cols = (
            SQLite.Expression<String>("document_id"),
            SQLite.Expression<String>("stage"),
            SQLite.Expression<String>("error_message"),
            SQLite.Expression<Date>("timestamp"),
            SQLite.Expression<Int>("retry_count")
         )
        self.table = Table("failures")
        try createTable()
    }

    private func createTable() throws {
        try db.run(table.create(ifNotExists: true) { t in
            t.column(cols.id, primaryKey: true)
            t.column(cols.stage)
            t.column(cols.error)
            t.column(cols.timestamp)
            t.column(cols.retryCount, defaultValue: 0)
        })
    }

    public func record(_ failure: FailureRecord) throws {
        let insert = table.insert(or: .replace,
            cols.id <- failure.documentID,
            cols.stage <- failure.stage,
            cols.error <- failure.errorMessage,
            cols.timestamp <- failure.timestamp,
            cols.retryCount <- failure.retryCount
        )
        try db.run(insert)
    }

    public func get(documentID: String, stage: String) throws -> FailureRecord? {
        guard let row = try db.pluck(table.filter(cols.id == documentID && cols.stage == stage)) else { return nil }
        return FailureRecord(
            documentID: row[cols.id],
            stage: row[cols.stage],
            errorMessage: row[cols.error],
            timestamp: row[cols.timestamp],
            retryCount: row[cols.retryCount]
        )
    }

    public func listByStage(_ stage: String, limit: Int = 100) throws -> [FailureRecord] {
        let query = table
            .filter(cols.stage == stage)
            .order(cols.timestamp.desc)
            .limit(limit)
        return try db.prepare(query).map { row in
            FailureRecord(
                documentID: row[cols.id],
                stage: row[cols.stage],
                errorMessage: row[cols.error],
                timestamp: row[cols.timestamp],
                retryCount: row[cols.retryCount]
            )
        }
    }

    public func incrementRetry(documentID: String, stage: String) throws {
        let row = table.filter(cols.id == documentID && cols.stage == stage)
        try db.run(row.update(cols.retryCount++))
    }

    public func clear(documentID: String, stage: String) throws {
        let row = table.filter(cols.id == documentID && cols.stage == stage)
        try db.run(row.delete())
    }

    public func countByStage() throws -> [String: Int] {
        var counts: [String: Int] = [:]
        let stages = ["download", "parse", "ocr", "metadata", "citation", "index"]
        for stage in stages {
            counts[stage] = try db.scalar(table.filter(cols.stage == stage).count)
        }
        return counts
    }

    public func allFailures(limit: Int = 500) throws -> [FailureRecord] {
        let query = table.order(cols.timestamp.desc).limit(limit)
        return try db.prepare(query).map { row in
            FailureRecord(
                documentID: row[cols.id],
                stage: row[cols.stage],
                errorMessage: row[cols.error],
                timestamp: row[cols.timestamp],
                retryCount: row[cols.retryCount]
            )
        }
    }
}
