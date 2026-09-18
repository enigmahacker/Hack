// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "LegalPipeline",
    platforms: [
        .macOS(.v14)
    ],
    dependencies: [
        .package(url: "https://github.com/apple/swift-argument-parser", from: "1.4.0"),
        .package(url: "https://github.com/stephencelis/SQLite.swift", from: "0.15.0"),
        .package(url: "https://github.com/scinfu/SwiftSoup", from: "2.6.0"),
    ],
    targets: [
        .executableTarget(
            name: "legal",
            dependencies: [
                .product(name: "ArgumentParser", package: "swift-argument-parser"),
                .product(name: "SQLite", package: "SQLite.swift"),
                .product(name: "SwiftSoup", package: "SwiftSoup"),
            ],
            path: "Sources/LegalPipeline"
        ),
        .testTarget(
            name: "LegalPipelineTests",
            dependencies: ["legal"],
            path: "Tests/LegalPipelineTests"
        ),
    ]
)
