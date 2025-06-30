import SwiftUI
import Foundation
import Combine

class CheckInViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var successMessage: String?
    @Published var availableSessions: [Session] = []
    
    private var cancellables = Set<AnyCancellable>()
    private let apiService = APIService.shared
    
    func loadAvailableSessions(mobileNumber: String, entityId: String) {
        isLoading = true
        errorMessage = nil
        
        apiService.getAvailableSessions(mobileNumber: mobileNumber, entityId: entityId)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    switch completion {
                    case .failure(let error):
                        self?.errorMessage = error.localizedDescription
                    case .finished:
                        break
                    }
                },
                receiveValue: { [weak self] response in
                    self?.availableSessions = response.sessions
                }
            )
            .store(in: &cancellables)
    }
    
    func handleQRCodeCheckIn(_ qrCode: String) {
        guard let currentUser = getCurrentUser(),
              let currentOrganization = getCurrentOrganization() else {
            errorMessage = "User not logged in"
            return
        }
        
        performQRCheckIn(
            mobileNumber: currentUser.mobileNumber,
            entityId: currentOrganization.entityId,
            qrCode: qrCode
        )
    }
    
    func performQRCheckIn(mobileNumber: String, entityId: String, qrCode: String) {
        isLoading = true
        errorMessage = nil
        successMessage = nil
        
        apiService.qrCheckIn(mobileNumber: mobileNumber, entityId: entityId, qrCode: qrCode)
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    switch completion {
                    case .failure(let error):
                        self?.errorMessage = error.localizedDescription
                    case .finished:
                        break
                    }
                },
                receiveValue: { [weak self] response in
                    self?.successMessage = response.message
                    // Refresh available sessions after check-in
                    if let user = self?.getCurrentUser(),
                       let org = self?.getCurrentOrganization() {
                        self?.loadAvailableSessions(mobileNumber: user.mobileNumber, entityId: org.entityId)
                    }
                }
            )
            .store(in: &cancellables)
    }
    
    func clearMessages() {
        errorMessage = nil
        successMessage = nil
    }
    
    private func getCurrentUser() -> Subscriber? {
        guard let userData = UserDefaults.standard.data(forKey: "currentUser"),
              let user = try? JSONDecoder().decode(Subscriber.self, from: userData) else {
            return nil
        }
        return user
    }
    
    private func getCurrentOrganization() -> Organization? {
        guard let orgData = UserDefaults.standard.data(forKey: "currentOrganization"),
              let org = try? JSONDecoder().decode(Organization.self, from: orgData) else {
            return nil
        }
        return org
    }
}
