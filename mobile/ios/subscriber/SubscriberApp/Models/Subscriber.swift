import Foundation

// MARK: - Core Models
struct Subscriber: Codable, Identifiable {
    let id: Int
    let firstName: String
    let lastName: String
    let mobileNumber: String
    let email: String?
    let hasNfcCard: Bool
    
    var fullName: String {
        "\(firstName) \(lastName)"
    }
}

struct Organization: Codable {
    let entityId: String
    let name: String
    let address: String?
}

struct Session: Codable, Identifiable {
    let id: Int
    let name: String
    let description: String?
    let startTime: String
    let allowedMethods: [String]
}

// MARK: - Request Models
struct LoginRequest: Codable {
    let mobileNumber: String
    let pin: String?
    let otpCode: String?
    let deviceId: String
    let deviceInfo: String
    let entityId: String
}

struct OtpRequest: Codable {
    let mobileNumber: String
    let entityId: String
}

struct CheckInRequest: Codable {
    let sessionId: Int
    let checkInMethod: String
    let qrCode: String?
    let deviceId: String?
    let deviceInfo: String?
    let locationInfo: String?
    let nfcData: String?
    let latitude: Double?
    let longitude: Double?
}

// MARK: - Response Models
struct LoginResponse: Codable {
    let token: String
    let subscriber: Subscriber
    let organization: Organization
    let message: String
}

struct OtpResponse: Codable {
    let message: String
    let mobileNumber: String
    let expiryTime: String
    let otp: String? // For development only
}

struct CheckInResponse: Codable {
    let action: String // CHECK_IN or CHECK_OUT
    let message: String
    let session: String
    let time: String
    let method: String
}

struct SessionsResponse: Codable {
    let sessions: [Session]
    let count: Int
}

// MARK: - Enums
enum CheckInMethod: String, CaseIterable {
    case nfc = "NFC"
    case qr = "QR"
    case bluetooth = "BLUETOOTH"
    case wifi = "WIFI"
    case mobileNfc = "MOBILE_NFC"
    
    var displayName: String {
        switch self {
        case .nfc: return "NFC Card"
        case .qr: return "QR Code"
        case .bluetooth: return "Bluetooth"
        case .wifi: return "WiFi"
        case .mobileNfc: return "Mobile NFC"
        }
    }
    
    var icon: String {
        switch self {
        case .nfc: return "creditcard"
        case .qr: return "qrcode"
        case .bluetooth: return "bluetooth"
        case .wifi: return "wifi"
        case .mobileNfc: return "wave.3.right"
        }
    }
}

enum AuthState {
    case idle
    case loading
    case loggedIn
    case error(String)
}

enum CheckInState {
    case idle
    case scanning
    case processing
    case success(String)
    case error(String)
}
