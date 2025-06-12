import SwiftUI
import Foundation
import Combine

class AuthViewModel: ObservableObject {
    @Published var isLoggedIn = false
    @Published var authState: AuthState = .idle
    @Published var successMessage: String?
    @Published var otpSent = false
    
    @Published var currentUser: Subscriber?
    @Published var currentOrganization: Organization?
    
    private var cancellables = Set<AnyCancellable>()
    private let apiService = APIService.shared
    
    init() {
        checkLoginStatus()
    }
    
    func checkLoginStatus() {
        // Check if user is already logged in (e.g., from UserDefaults)
        if let userData = UserDefaults.standard.data(forKey: "currentUser"),
           let user = try? JSONDecoder().decode(Subscriber.self, from: userData),
           let orgData = UserDefaults.standard.data(forKey: "currentOrganization"),
           let org = try? JSONDecoder().decode(Organization.self, from: orgData) {
            
            self.currentUser = user
            self.currentOrganization = org
            self.isLoggedIn = true
        }
    }
    
    func sendOTP(mobileNumber: String, entityId: String) {
        authState = .loading
        
        let request = OtpRequest(mobileNumber: mobileNumber, entityId: entityId)
        
        apiService.sendOTP(request: request)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    switch completion {
                    case .failure(let error):
                        self?.authState = .error(error.localizedDescription)
                    case .finished:
                        break
                    }
                },
                receiveValue: { [weak self] response in
                    self?.authState = .idle
                    self?.otpSent = true
                    self?.successMessage = response.message
                }
            )
            .store(in: &cancellables)
    }
    
    func verifyOTP(mobileNumber: String, entityId: String, otpCode: String) {
        authState = .loading
        
        let request = LoginRequest(
            mobileNumber: mobileNumber,
            pin: nil,
            otpCode: otpCode,
            deviceId: getDeviceId(),
            deviceInfo: getDeviceInfo(),
            entityId: entityId
        )
        
        apiService.verifyOTP(request: request)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    switch completion {
                    case .failure(let error):
                        self?.authState = .error(error.localizedDescription)
                    case .finished:
                        break
                    }
                },
                receiveValue: { [weak self] response in
                    self?.handleLoginSuccess(response)
                }
            )
            .store(in: &cancellables)
    }
    
    func loginWithPin(mobileNumber: String, pin: String) {
        authState = .loading

        let request = LoginRequest(
            mobileNumber: mobileNumber,
            pin: pin,
            otpCode: nil,
            deviceId: getDeviceId(),
            deviceInfo: getDeviceInfo(),
            entityId: nil
        )

        apiService.loginWithPIN(request: request)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    switch completion {
                    case .failure(let error):
                        self?.authState = .error(error.localizedDescription)
                    case .finished:
                        break
                    }
                },
                receiveValue: { [weak self] response in
                    self?.handleLoginSuccess(response)
                }
            )
            .store(in: &cancellables)
    }
    
    private func handleLoginSuccess(_ response: LoginResponse) {
        self.currentUser = response.subscriber
        self.currentOrganization = response.organization
        self.isLoggedIn = true
        self.authState = .loggedIn
        self.successMessage = response.message
        
        // Save to UserDefaults for persistence
        if let userData = try? JSONEncoder().encode(response.subscriber) {
            UserDefaults.standard.set(userData, forKey: "currentUser")
        }
        if let orgData = try? JSONEncoder().encode(response.organization) {
            UserDefaults.standard.set(orgData, forKey: "currentOrganization")
        }
        UserDefaults.standard.set(response.token, forKey: "authToken")
    }
    
    func logout() {
        isLoggedIn = false
        authState = .idle
        currentUser = nil
        currentOrganization = nil
        otpSent = false
        successMessage = nil
        
        // Clear UserDefaults
        UserDefaults.standard.removeObject(forKey: "currentUser")
        UserDefaults.standard.removeObject(forKey: "currentOrganization")
        UserDefaults.standard.removeObject(forKey: "authToken")
    }
    
    private func getDeviceId() -> String {
        if let deviceId = UserDefaults.standard.string(forKey: "deviceId") {
            return deviceId
        } else {
            let newDeviceId = "ios_device_\(Int(Date().timeIntervalSince1970))"
            UserDefaults.standard.set(newDeviceId, forKey: "deviceId")
            return newDeviceId
        }
    }
    
    private func getDeviceInfo() -> String {
        let device = UIDevice.current
        return "iOS \(device.systemVersion) - \(device.model)"
    }
}

enum AuthState {
    case idle
    case loading
    case loggedIn
    case error(String)
}
