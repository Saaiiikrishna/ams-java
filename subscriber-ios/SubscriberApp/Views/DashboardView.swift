import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var checkInViewModel: CheckInViewModel
    @StateObject private var dashboardViewModel = DashboardViewModel()
    @State private var selectedTab = 0
    
    var body: some View {
        TabView(selection: $selectedTab) {
            // Main Dashboard Tab
            MainDashboardView()
                .environmentObject(authViewModel)
                .environmentObject(dashboardViewModel)
                .tabItem {
                    Image(systemName: "house.fill")
                    Text("Dashboard")
                }
                .tag(0)
            
            // Sessions Tab
            SessionsView()
                .environmentObject(authViewModel)
                .environmentObject(checkInViewModel)
                .tabItem {
                    Image(systemName: "calendar")
                    Text("Sessions")
                }
                .tag(1)
            
            // QR Scanner Tab
            QRScannerView()
                .environmentObject(authViewModel)
                .environmentObject(checkInViewModel)
                .tabItem {
                    Image(systemName: "qrcode.viewfinder")
                    Text("Scan QR")
                }
                .tag(2)
            
            // History Tab
            AttendanceHistoryView()
                .environmentObject(authViewModel)
                .environmentObject(dashboardViewModel)
                .tabItem {
                    Image(systemName: "clock.fill")
                    Text("History")
                }
                .tag(3)
            
            // Profile Tab
            ProfileView()
                .environmentObject(authViewModel)
                .tabItem {
                    Image(systemName: "person.fill")
                    Text("Profile")
                }
                .tag(4)
        }
        .onAppear {
            loadDashboardData()
        }
    }
    
    private func loadDashboardData() {
        guard let user = authViewModel.currentUser,
              let organization = authViewModel.currentOrganization else { return }
        
        dashboardViewModel.loadDashboard(
            mobileNumber: user.mobileNumber,
            entityId: organization.entityId
        )
        
        checkInViewModel.loadAvailableSessions(
            mobileNumber: user.mobileNumber,
            entityId: organization.entityId
        )
    }
}

struct MainDashboardView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var dashboardViewModel: DashboardViewModel
    
    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // Welcome Card
                    if let user = authViewModel.currentUser,
                       let organization = authViewModel.currentOrganization {
                        WelcomeCard(
                            userName: "\(user.firstName) \(user.lastName)",
                            organizationName: organization.name
                        )
                    }
                    
                    // Quick Actions
                    QuickActionsCard()
                    
                    // Active Sessions
                    if !dashboardViewModel.activeSessions.isEmpty {
                        SectionHeader(title: "Active Sessions")
                        ForEach(dashboardViewModel.activeSessions) { session in
                            ActiveSessionCard(session: session)
                        }
                    }
                    
                    // Recent Attendance
                    if !dashboardViewModel.recentAttendance.isEmpty {
                        SectionHeader(title: "Recent Attendance")
                        ForEach(dashboardViewModel.recentAttendance.prefix(5)) { attendance in
                            AttendanceCard(attendance: attendance)
                        }
                    }
                    
                    // Upcoming Sessions
                    if !dashboardViewModel.upcomingSessions.isEmpty {
                        SectionHeader(title: "Upcoming Sessions")
                        ForEach(dashboardViewModel.upcomingSessions.prefix(3)) { session in
                            UpcomingSessionCard(session: session)
                        }
                    }
                    
                    // Loading State
                    if dashboardViewModel.isLoading {
                        ProgressView("Loading...")
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
            .navigationTitle(authViewModel.currentOrganization?.name ?? "Dashboard")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button("Refresh") {
                            dashboardViewModel.refreshDashboard()
                        }
                        Button("Logout") {
                            authViewModel.logout()
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .refreshable {
                dashboardViewModel.refreshDashboard()
            }
        }
    }
}

struct WelcomeCard: View {
    let userName: String
    let organizationName: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Welcome back, \(userName)!")
                .font(.title2)
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text(organizationName)
                .font(.body)
                .foregroundColor(.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
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

struct QuickActionsCard: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Quick Actions")
                .font(.headline)
                .fontWeight(.semibold)
            
            HStack(spacing: 20) {
                QuickActionButton(
                    icon: "qrcode.viewfinder",
                    title: "Scan QR",
                    action: { /* Navigate to QR scanner */ }
                )
                
                QuickActionButton(
                    icon: "calendar",
                    title: "Sessions",
                    action: { /* Navigate to sessions */ }
                )
                
                QuickActionButton(
                    icon: "clock",
                    title: "History",
                    action: { /* Navigate to history */ }
                )
                
                QuickActionButton(
                    icon: "person",
                    title: "Profile",
                    action: { /* Navigate to profile */ }
                )
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
    }
}

struct QuickActionButton: View {
    let icon: String
    let title: String
    let action: () -> Void
    
    var body: some View {
        VStack(spacing: 8) {
            Button(action: action) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(.blue)
                    .frame(width: 44, height: 44)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(8)
            }
            
            Text(title)
                .font(.caption)
                .foregroundColor(.primary)
        }
    }
}

struct SectionHeader: View {
    let title: String
    
    var body: some View {
        HStack {
            Text(title)
                .font(.headline)
                .fontWeight(.semibold)
            Spacer()
        }
    }
}

struct ActiveSessionCard: View {
    let session: Session
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(session.name)
                .font(.headline)
                .fontWeight(.semibold)
            
            if let description = session.description {
                Text(description)
                    .font(.body)
                    .foregroundColor(.secondary)
            }
            
            Text("Started: \(session.startTime)")
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text("Methods: \(session.allowedMethods.joined(separator: ", "))")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
    }
}

struct AttendanceCard: View {
    let attendance: AttendanceRecord
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(attendance.sessionName)
                    .font(.headline)
                    .fontWeight(.medium)
                
                Text("Check-in: \(attendance.checkInTime)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                if let checkOut = attendance.checkOutTime {
                    Text("Check-out: \(checkOut)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 4) {
                Text(attendance.method)
                    .font(.caption)
                    .fontWeight(.medium)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(4)
                
                Text(attendance.status.uppercased())
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(attendance.status == "completed" ? .green : .orange)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
    }
}

struct UpcomingSessionCard: View {
    let session: UpcomingSession
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(session.name)
                .font(.headline)
                .fontWeight(.semibold)
            
            if let description = session.description {
                Text(description)
                    .font(.body)
                    .foregroundColor(.secondary)
            }
            
            Text("Time: \(session.startTime)")
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text("Duration: \(session.durationMinutes) minutes")
                .font(.caption)
                .foregroundColor(.secondary)
            
            Text("Days: \(session.daysOfWeek.joined(separator: ", "))")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
    }
}

struct ErrorCard: View {
    let message: String
    
    var body: some View {
        Text(message)
            .foregroundColor(.red)
            .padding()
            .background(Color.red.opacity(0.1))
            .cornerRadius(8)
    }
}

// MARK: - Additional Views

struct SessionsView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var checkInViewModel: CheckInViewModel

    var body: some View {
        NavigationView {
            ScrollView {
                LazyVStack(spacing: 12) {
                    if checkInViewModel.availableSessions.isEmpty && !checkInViewModel.isLoading {
                        EmptyStateView(
                            icon: "calendar.badge.exclamationmark",
                            title: "No Active Sessions",
                            message: "There are currently no active sessions available for check-in."
                        )
                    } else {
                        ForEach(checkInViewModel.availableSessions) { session in
                            SessionCard(session: session) {
                                // Handle check-in action
                            }
                        }
                    }

                    if checkInViewModel.isLoading {
                        ProgressView("Loading sessions...")
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding()
                    }

                    if let errorMessage = checkInViewModel.errorMessage {
                        ErrorCard(message: errorMessage)
                    }
                }
                .padding()
            }
            .navigationTitle("Available Sessions")
            .refreshable {
                if let user = authViewModel.currentUser,
                   let org = authViewModel.currentOrganization {
                    checkInViewModel.loadAvailableSessions(
                        mobileNumber: user.mobileNumber,
                        entityId: org.entityId
                    )
                }
            }
        }
    }
}

struct SessionCard: View {
    let session: Session
    let onCheckIn: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(session.name)
                        .font(.headline)
                        .fontWeight(.semibold)

                    if let description = session.description {
                        Text(description)
                            .font(.body)
                            .foregroundColor(.secondary)
                    }

                    Text("Started: \(session.startTime)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                Button("Check In") {
                    onCheckIn()
                }
                .buttonStyle(.borderedProminent)
            }

            if !session.allowedMethods.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Available Methods:")
                        .font(.caption)
                        .fontWeight(.medium)

                    LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 8) {
                        ForEach(session.allowedMethods, id: \.self) { method in
                            MethodChip(method: method)
                        }
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
    }
}

struct MethodChip: View {
    let method: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: iconForMethod(method))
                .font(.caption)
            Text(method)
                .font(.caption)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.blue.opacity(0.1))
        .cornerRadius(8)
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

struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundColor(.secondary)

            Text(title)
                .font(.headline)
                .fontWeight(.semibold)

            Text(message)
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}

struct DashboardView_Previews: PreviewProvider {
    static var previews: some View {
        DashboardView()
            .environmentObject(AuthViewModel())
            .environmentObject(CheckInViewModel())
    }
}
