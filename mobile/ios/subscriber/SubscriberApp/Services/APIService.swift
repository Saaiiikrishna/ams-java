import Foundation
import Combine

class APIService {
    static let shared = APIService()

    private let dynamicAPIService = DynamicAPIService.shared
    private let session = URLSession.shared

    private init() {}
    
    // MARK: - Authentication
    
    func sendOTP(request: OtpRequest) -> AnyPublisher<OtpResponse, Error> {
        return performRequest(
            endpoint: "send-otp",
            method: "POST",
            body: request,
            responseType: OtpResponse.self
        )
    }
    
    func verifyOTP(request: LoginRequest) -> AnyPublisher<LoginResponse, Error> {
        return performRequest(
            endpoint: "verify-otp",
            method: "POST",
            body: request,
            responseType: LoginResponse.self
        )
    }
    
    func loginWithPIN(request: LoginRequest) -> AnyPublisher<LoginResponse, Error> {
        return performRequest(
            endpoint: "login-pin",
            method: "POST",
            body: request,
            responseType: LoginResponse.self
        )
    }
    
    // MARK: - Dashboard

    func getDashboard(mobileNumber: String, entityId: String) -> AnyPublisher<DashboardResponse, Error> {
        let baseURL = dynamicAPIService.getSubscriberBaseURL()
        let url = URL(string: baseURL + "mobile/dashboard?mobileNumber=\(mobileNumber)&entityId=\(entityId)")!

        return session.dataTaskPublisher(for: url)
            .map(\.data)
            .decode(type: DashboardResponse.self, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }

    func getAvailableSessions(mobileNumber: String, entityId: String) -> AnyPublisher<SessionsResponse, Error> {
        let baseURL = dynamicAPIService.getSubscriberBaseURL()
        let url = URL(string: baseURL + "mobile/sessions?mobileNumber=\(mobileNumber)&entityId=\(entityId)")!

        return session.dataTaskPublisher(for: url)
            .map(\.data)
            .decode(type: SessionsResponse.self, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }

    func getAttendanceHistory(mobileNumber: String, entityId: String) -> AnyPublisher<AttendanceHistoryResponse, Error> {
        let baseURL = dynamicAPIService.getSubscriberBaseURL()
        let url = URL(string: baseURL + "mobile/attendance/history?mobileNumber=\(mobileNumber)&entityId=\(entityId)")!

        return session.dataTaskPublisher(for: url)
            .map(\.data)
            .decode(type: AttendanceHistoryResponse.self, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }
    
    // MARK: - Check-in
    
    func qrCheckIn(mobileNumber: String, entityId: String, qrCode: String) -> AnyPublisher<CheckInResponse, Error> {
        let request = [
            "mobileNumber": mobileNumber,
            "entityId": entityId,
            "qrCode": qrCode
        ]
        
        return performRequest(
            endpoint: "mobile/checkin/qr",
            method: "POST",
            body: request,
            responseType: CheckInResponse.self
        )
    }
    
    // MARK: - Generic Request Method
    
    private func performRequest<T: Codable, U: Codable>(
        endpoint: String,
        method: String,
        body: T,
        responseType: U.Type
    ) -> AnyPublisher<U, Error> {

        let baseURL = dynamicAPIService.getSubscriberBaseURL()
        guard let url = URL(string: baseURL + endpoint) else {
            return Fail(error: APIError.invalidURL)
                .eraseToAnyPublisher()
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        do {
            request.httpBody = try JSONEncoder().encode(body)
        } catch {
            return Fail(error: error)
                .eraseToAnyPublisher()
        }
        
        return session.dataTaskPublisher(for: request)
            .map(\.data)
            .decode(type: responseType, decoder: JSONDecoder())
            .eraseToAnyPublisher()
    }
}

enum APIError: Error, LocalizedError {
    case invalidURL
    case noData
    case decodingError
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .noData:
            return "No data received"
        case .decodingError:
            return "Failed to decode response"
        }
    }
}

// MARK: - Response Models

struct DashboardResponse: Codable {
    let subscriber: Subscriber
    let organization: Organization
    let activeSessions: [Session]
    let recentAttendance: [AttendanceRecord]
    let upcomingSessions: [UpcomingSession]
}

struct AttendanceHistoryResponse: Codable {
    let history: [AttendanceRecord]
    let totalSessions: Int
}

struct AttendanceRecord: Codable, Identifiable {
    let id: Int
    let sessionName: String
    let checkInTime: String
    let checkOutTime: String?
    let method: String
    let status: String
}

struct UpcomingSession: Codable, Identifiable {
    let id: Int
    let name: String
    let description: String?
    let startTime: String
    let durationMinutes: Int
    let daysOfWeek: [String]
    let allowedMethods: [String]
    let isActive: Bool
}
