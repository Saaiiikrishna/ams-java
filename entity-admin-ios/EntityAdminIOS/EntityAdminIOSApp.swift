import SwiftUI

@main
struct EntityAdminIOSApp: App {
    var body: some Scene {
        WindowGroup {
            NavigationView {
                LoginView()
            }
            .preferredColorScheme(.dark)
        }
    }
}
