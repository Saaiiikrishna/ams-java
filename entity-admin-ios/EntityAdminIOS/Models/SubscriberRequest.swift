// entity-admin-ios/EntityAdminIOS/Models/SubscriberRequest.swift
import Foundation

struct SubscriberRequest: Codable {
    let name: String
    let email: String
    let nfcCardUid: String?
}
