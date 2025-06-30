import SwiftUI

struct SessionsView: View {
    @EnvironmentObject var authManager: AuthenticationManager // Added
    @State private var sessions: [Session] = []
    @State private var showingCreateSheet = false

    var body: some View {
        List {
            ForEach(sessions) { session in
                Text(session.name)
            }
        }
        .onAppear {
            fetchSessions() // Extracted fetch logic
        }
        .navigationTitle("Sessions")
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarLeading) { // Group for leading items
                NavigationLink(destination: SubscriberListView().environmentObject(authManager)) { // Pass authManager
                    Image(systemName: "person.2.fill")
                }
            }
            ToolbarItemGroup(placement: .navigationBarTrailing) { // Group for trailing items
                Button {
                    showingCreateSheet = true
                } label: {
                    Image(systemName: "plus")
                }
                Button("Logout") { // Added Logout Button
                    authManager.logout()
                }
            }
        }
        .sheet(isPresented: $showingCreateSheet) {
            // Pass authManager to CreateSessionView if it needs it, though not directly used for creation logic
            // Pass authManager to CreateSessionView if it needs it
            CreateSessionView { sessionName, completion in
                APIClient.shared.createSession(name: sessionName) { result in // Changed to handle Result
                    DispatchQueue.main.async {
                        switch result {
                        case .success(_): // Don't need the createdSession object here, just success
                            fetchSessions() // Refresh list
                            completion(true, nil)
                        case .failure(let error):
                            completion(false, (error as? APIClient.APIError)?.errorDescription ?? error.localizedDescription)
                        }
                    }
                }
            }
            .environmentObject(authManager) // Pass authManager to CreateSessionView
        }
    }

    func fetchSessions() {
        // Add isLoading and errorMessage states to SessionsView if detailed error/loading for session list is needed
        // For now, just updating sessions or printing error to console.
        APIClient.shared.fetchSessions { result in
            DispatchQueue.main.async {
                switch result {
                case .success(let fetchedSessions):
                    self.sessions = fetchedSessions
                case .failure(let error):
                    self.sessions = [] // Clear sessions on error
                    // Handle error display, e.g., by adding @State var sessionListError: String?
                    print("Error fetching sessions: \((error as? APIClient.APIError)?.errorDescription ?? error.localizedDescription)")
                }
            }
        }
    }
}

struct SessionsView_Previews: PreviewProvider {
    static var previews: some View {
        SessionsView()
    }
}
