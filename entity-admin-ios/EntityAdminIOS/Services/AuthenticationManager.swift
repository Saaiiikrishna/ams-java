// entity-admin-ios/EntityAdminIOS/Services/AuthenticationManager.swift
import SwiftUI
// import Combine // Not strictly needed for simple boolean @Published

class AuthenticationManager: ObservableObject {
    @Published var isAuthenticated: Bool = false

    init() {
        // Check initial authentication state
        if let token = KeychainHelper.standard.read(service: "token", account: "jwt"), !token.isEmpty {
            APIClient.shared.token = token // Pre-load token into APIClient's in-memory variable
            isAuthenticated = true
            print("App initialized with token, user is authenticated.")
        } else {
            print("App initialized without token, user is not authenticated.")
        }
    }

    // Updated login method in AuthenticationManager
    func login(username: String, password: String, completion: @escaping (APIClient.APIError?) -> Void) {
        APIClient.shared.login(username: username, password: password) { result in
            DispatchQueue.main.async {
                switch result {
                case .success:
                    self.isAuthenticated = true
                    print("Login successful, isAuthenticated set to true.")
                    completion(nil) // No error
                case .failure(let apiError):
                    self.isAuthenticated = false
                    print("Login failed with APIError: \(apiError.localizedDescription), isAuthenticated remains false.")
                    completion(apiError) // Pass the specific APIError
                }
            }
        }
    }

    func logout() {
        APIClient.shared.logout() // Clears token in APIClient and Keychain
        DispatchQueue.main.async {
            self.isAuthenticated = false
            print("Logout successful, isAuthenticated set to false.")
        }
    }
}
