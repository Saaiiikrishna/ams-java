import SwiftUI

struct ContentView: View {
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var checkInViewModel = CheckInViewModel()
    
    var body: some View {
        NavigationView {
            if authViewModel.isLoggedIn {
                DashboardView()
                    .environmentObject(authViewModel)
                    .environmentObject(checkInViewModel)
            } else {
                LoginView()
                    .environmentObject(authViewModel)
            }
        }
        .onAppear {
            authViewModel.checkLoginStatus()
        }
        .onOpenURL { url in
            handleDeepLink(url)
        }
    }
    
    private func handleDeepLink(_ url: URL) {
        guard url.scheme == "ams", url.host == "checkin" else { return }
        
        if let qrCode = URLComponents(url: url, resolvingAgainstBaseURL: false)?
            .queryItems?.first(where: { $0.name == "qr" })?.value {
            // Handle QR code check-in
            checkInViewModel.handleQRCodeCheckIn(qrCode)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
