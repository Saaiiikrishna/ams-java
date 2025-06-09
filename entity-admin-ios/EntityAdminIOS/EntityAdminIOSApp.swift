import SwiftUI

@main
struct EntityAdminIOSApp: App {
    @StateObject private var authManager = AuthenticationManager() // Added

    var body: some Scene {
        WindowGroup {
            // Conditional view rendering based on authentication state
            if authManager.isAuthenticated {
                NavigationView { // NavigationView for the authenticated part of the app
                    SessionsView() // Assuming SessionsView is the main screen after login
                        .environmentObject(authManager) // Pass authManager
                }
                .preferredColorScheme(.dark)
                .transition(.asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .leading))) // Animation
            } else {
                // LoginView doesn't necessarily need its own NavigationView if it's simple
                // or if the App struct handles the top-level navigation context when unauthenticated.
                // For consistency and if LoginView might have its own title/bar items:
                NavigationView {
                     LoginView()
                        .environmentObject(authManager) // Pass authManager
                }
                .preferredColorScheme(.dark)
                .transition(.asymmetric(insertion: .move(edge: .leading), removal: .move(edge: .trailing))) // Animation
            }
        }
    }
}
