import SwiftUI

struct AttendanceHistoryView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var dashboardViewModel: DashboardViewModel
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // Summary Card
                    SummaryCard(totalSessions: dashboardViewModel.totalSessionsAttended)
                    
                    // Attendance History
                    if dashboardViewModel.attendanceHistory.isEmpty && !dashboardViewModel.isLoading {
                        EmptyHistoryView()
                    } else {
                        ForEach(dashboardViewModel.attendanceHistory) { attendance in
                            AttendanceHistoryCard(attendance: attendance)
                        }
                    }
                    
                    // Loading State
                    if dashboardViewModel.isLoading {
                        ProgressView("Loading history...")
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding()
                    }
                    
                    // Error State
                    if let errorMessage = dashboardViewModel.errorMessage {
                        ErrorCard(message: errorMessage)
                    }
                }
                .padding()
            }
            .navigationTitle("Attendance History")
            .navigationBarTitleDisplayMode(.large)
            .onAppear {
                loadAttendanceHistory()
            }
            .refreshable {
                loadAttendanceHistory()
            }
        }
    }
    
    private func loadAttendanceHistory() {
        guard let user = authViewModel.currentUser,
              let organization = authViewModel.currentOrganization else { return }
        
        dashboardViewModel.loadAttendanceHistory(
            mobileNumber: user.mobileNumber,
            entityId: organization.entityId
        )
    }
}

struct SummaryCard: View {
    let totalSessions: Int
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("Total Sessions Attended")
                    .font(.body)
                    .foregroundColor(.white.opacity(0.9))
                
                Text("\(totalSessions)")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            
            Spacer()
            
            Image(systemName: "calendar.badge.checkmark")
                .font(.system(size: 40))
                .foregroundColor(.white.opacity(0.8))
        }
        .padding()
        .background(
            LinearGradient(
                gradient: Gradient(colors: [.blue, .purple]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .cornerRadius(12)
    }
}

struct AttendanceHistoryCard: View {
    let attendance: AttendanceRecord
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(attendance.sessionName)
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text("Check-in: \(formatTime(attendance.checkInTime))")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    if let checkOut = attendance.checkOutTime {
                        Text("Check-out: \(formatTime(checkOut))")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 8) {
                    MethodBadge(method: attendance.method)
                    
                    StatusBadge(status: attendance.status)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .gray.opacity(0.2), radius: 2, x: 0, y: 1)
    }
    
    private func formatTime(_ timeString: String) -> String {
        // In a real app, you'd format this properly based on your date format
        return timeString
    }
}

struct MethodBadge: View {
    let method: String
    
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: iconForMethod(method))
                .font(.caption)
            Text(method)
                .font(.caption)
                .fontWeight(.medium)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.blue.opacity(0.1))
        .foregroundColor(.blue)
        .cornerRadius(6)
    }
    
    private func iconForMethod(_ method: String) -> String {
        switch method.uppercased() {
        case "QR": return "qrcode"
        case "NFC": return "creditcard"
        case "BLUETOOTH": return "bluetooth"
        case "WIFI": return "wifi"
        case "MOBILE_NFC": return "wave.3.right"
        default: return "checkmark.circle"
        }
    }
}

struct StatusBadge: View {
    let status: String
    
    var body: some View {
        Text(status.uppercased())
            .font(.caption)
            .fontWeight(.semibold)
            .foregroundColor(status == "completed" ? .green : .orange)
    }
}

struct EmptyHistoryView: View {
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "clock.badge.xmark")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
            
            Text("No Attendance History")
                .font(.headline)
                .fontWeight(.semibold)
            
            Text("Your attendance history will appear here once you start checking in to sessions.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct ErrorCard: View {
    let message: String
    
    var body: some View {
        HStack {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundColor(.red)
            
            Text(message)
                .font(.body)
                .foregroundColor(.red)
        }
        .padding()
        .background(Color.red.opacity(0.1))
        .cornerRadius(8)
    }
}

struct AttendanceHistoryView_Previews: PreviewProvider {
    static var previews: some View {
        AttendanceHistoryView()
            .environmentObject(AuthViewModel())
            .environmentObject(DashboardViewModel())
    }
}
