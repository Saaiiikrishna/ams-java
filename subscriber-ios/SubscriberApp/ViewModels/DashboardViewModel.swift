import SwiftUI
import Foundation
import Combine

class DashboardViewModel: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var activeSessions: [Session] = []
    @Published var recentAttendance: [AttendanceRecord] = []
    @Published var upcomingSessions: [UpcomingSession] = []
    @Published var attendanceHistory: [AttendanceRecord] = []
    @Published var totalSessionsAttended = 0
    
    private var cancellables = Set<AnyCancellable>()
    private let apiService = APIService.shared
    
    func loadDashboard(mobileNumber: String, entityId: String) {
        isLoading = true
        errorMessage = nil
        
        apiService.getDashboard(mobileNumber: mobileNumber, entityId: entityId)
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
                    self?.activeSessions = response.activeSessions
                    self?.recentAttendance = response.recentAttendance
                    self?.upcomingSessions = response.upcomingSessions
                }
            )
            .store(in: &cancellables)
    }
    
    func loadAttendanceHistory(mobileNumber: String, entityId: String) {
        isLoading = true
        errorMessage = nil
        
        apiService.getAttendanceHistory(mobileNumber: mobileNumber, entityId: entityId)
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
                    self?.attendanceHistory = response.history
                    self?.totalSessionsAttended = response.totalSessions
                }
            )
            .store(in: &cancellables)
    }
    
    func refreshDashboard() {
        guard let currentUser = getCurrentUser(),
              let currentOrganization = getCurrentOrganization() else {
            errorMessage = "User not logged in"
            return
        }
        
        loadDashboard(mobileNumber: currentUser.mobileNumber, entityId: currentOrganization.entityId)
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
