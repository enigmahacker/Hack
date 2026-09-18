import Foundation

// MARK: - Logger

public struct Logger {
    public static func info(_ message: String, documentID: String? = nil) {
        let ts = ISO8601DateFormatter().string(from: Date())
        let doc = documentID.map { " doc=\($0)" } ?? ""
        fputs("\(ts) INFO\(doc): \(message)\n", stderr)
    }

    public static func error(_ message: String, documentID: String? = nil) {
        let ts = ISO8601DateFormatter().string(from: Date())
        let doc = documentID.map { " doc=\($0)" } ?? ""
        fputs("\(ts) ERROR\(doc): \(message)\n", stderr)
    }

    public static func warn(_ message: String, documentID: String? = nil) {
        let ts = ISO8601DateFormatter().string(from: Date())
        let doc = documentID.map { " doc=\($0)" } ?? ""
        fputs("\(ts) WARN\(doc): \(message)\n", stderr)
    }
}
