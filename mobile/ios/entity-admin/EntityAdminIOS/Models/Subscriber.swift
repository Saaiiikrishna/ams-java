// entity-admin-ios/EntityAdminIOS/Models/Subscriber.swift
import Foundation

struct Subscriber: Codable, Identifiable {
    let id: String // Assuming String ID from backend, adjust if Int/UUID
    let name: String
    let email: String
    let nfcCardUid: String? // Optional, as per Android model

    // If your backend uses different keys, define CodingKeys:
    // enum CodingKeys: String, CodingKey {
    //     case id
    //     case name
    //     case email
    //     case nfcCardUid = "nfc_card_uid" // Example for backend key "nfc_card_uid"
    // }
}
