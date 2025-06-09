import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthenticationManager // Added
    @State private var username = ""
    @State private var password = ""
    // @State private var showingSessions = false // Removed, global state handles this
    @State private var errorMessage: String? // Added for displaying login errors
    @State private var isLoading: Bool = false // Added for loading indicator

    var body: some View {
        VStack(spacing: 16) {
            TextField("Username", text: $username)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .autocapitalization(.none)
                .disableAutocorrection(true)
            SecureField("Password", text: $password)
                .textFieldStyle(RoundedBorderTextFieldStyle())

            if let error = errorMessage {
                Text(error)
                    .foregroundColor(.red)
                    .padding(.bottom, 5)
            }

            if isLoading {
                ProgressView()
            } else {
                Button("Login") {
                    isLoading = true
                    errorMessage = nil
                    authManager.login(username: username, password: password) { apiError in
                        isLoading = false
                        if let error = apiError {
                            self.errorMessage = error.localizedDescription // Use APIError's localizedDescription
                        }
                        // Navigation is handled by EntityAdminIOSApp based on authManager.isAuthenticated
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(username.isEmpty || password.isEmpty)
            }
        }
        .padding()
        .navigationTitle("Login")
        // Removed background NavigationLink, as navigation is now global
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
