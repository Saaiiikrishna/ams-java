import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authManager: AuthenticationManager // Added
    @State private var username = ""
    @State private var password = ""
    // @State private var showingSessions = false // Removed, global state handles this
    @State private var errorMessage: String? // Added for displaying login errors
    @State private var isLoading: Bool = false // Added for loading indicator

    @State private var showPassword = false

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // Background Gradient
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color(red: 0.4, green: 0.49, blue: 0.92),
                        Color(red: 0.46, green: 0.29, blue: 0.64)
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 0) {
                        // Top spacing
                        Spacer()
                            .frame(height: geometry.size.height * 0.1)

                        // Logo and Title Section
                        VStack(spacing: 16) {
                            // Logo
                            Image(systemName: "building.2.crop.circle.fill")
                                .font(.system(size: 80))
                                .foregroundColor(.white)
                                .shadow(color: .black.opacity(0.3), radius: 10, x: 0, y: 5)

                            // Title
                            Text("Entity Admin")
                                .font(.system(size: 32, weight: .bold, design: .rounded))
                                .foregroundColor(.white)
                                .shadow(color: .black.opacity(0.3), radius: 5, x: 0, y: 2)

                            // Subtitle
                            Text("Attendance Management System")
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(.white.opacity(0.9))
                                .multilineTextAlignment(.center)
                        }
                        .padding(.bottom, 40)

                        // Login Card
                        VStack(spacing: 24) {
                            // Card Header
                            VStack(spacing: 8) {
                                Text("Sign In")
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundColor(.primary)

                                Text("Enter your credentials to access the admin dashboard")
                                    .font(.system(size: 14))
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.center)
                            }

                            // Form Fields
                            VStack(spacing: 20) {
                                // Username Field
                                VStack(alignment: .leading, spacing: 8) {
                                    HStack {
                                        Image(systemName: "person.fill")
                                            .foregroundColor(.blue)
                                            .frame(width: 20)
                                        TextField("Username", text: $username)
                                            .textFieldStyle(PlainTextFieldStyle())
                                            .autocapitalization(.none)
                                            .disableAutocorrection(true)
                                    }
                                    .padding()
                                    .background(Color(.systemGray6))
                                    .cornerRadius(12)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(Color.blue.opacity(0.3), lineWidth: 1)
                                    )
                                }

                                // Password Field
                                VStack(alignment: .leading, spacing: 8) {
                                    HStack {
                                        Image(systemName: "lock.fill")
                                            .foregroundColor(.blue)
                                            .frame(width: 20)

                                        if showPassword {
                                            TextField("Password", text: $password)
                                                .textFieldStyle(PlainTextFieldStyle())
                                        } else {
                                            SecureField("Password", text: $password)
                                                .textFieldStyle(PlainTextFieldStyle())
                                        }

                                        Button(action: { showPassword.toggle() }) {
                                            Image(systemName: showPassword ? "eye.slash.fill" : "eye.fill")
                                                .foregroundColor(.gray)
                                        }
                                    }
                                    .padding()
                                    .background(Color(.systemGray6))
                                    .cornerRadius(12)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(Color.blue.opacity(0.3), lineWidth: 1)
                                    )
                                }
                            }

                            // Error Message
                            if let error = errorMessage {
                                HStack {
                                    Image(systemName: "exclamationmark.triangle.fill")
                                        .foregroundColor(.red)
                                    Text(error)
                                        .font(.system(size: 14))
                                        .foregroundColor(.red)
                                }
                                .padding()
                                .background(Color.red.opacity(0.1))
                                .cornerRadius(8)
                            }

                            // Login Button
                            Button(action: {
                                isLoading = true
                                errorMessage = nil
                                authManager.login(username: username, password: password) { apiError in
                                    isLoading = false
                                    if let error = apiError {
                                        self.errorMessage = error.localizedDescription
                                    }
                                }
                            }) {
                                HStack {
                                    if isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            .scaleEffect(0.8)
                                    } else {
                                        Image(systemName: "arrow.right.circle.fill")
                                            .font(.system(size: 16))
                                        Text("Sign In")
                                            .font(.system(size: 16, weight: .semibold))
                                    }
                                }
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 50)
                                .background(
                                    LinearGradient(
                                        gradient: Gradient(colors: [Color.blue, Color.blue.opacity(0.8)]),
                                        startPoint: .leading,
                                        endPoint: .trailing
                                    )
                                )
                                .cornerRadius(12)
                                .shadow(color: .blue.opacity(0.3), radius: 5, x: 0, y: 3)
                            }
                            .disabled(isLoading || username.isEmpty || password.isEmpty)
                            .opacity((isLoading || username.isEmpty || password.isEmpty) ? 0.6 : 1.0)
                        }
                        .padding(24)
                        .background(
                            RoundedRectangle(cornerRadius: 20)
                                .fill(Color(.systemBackground))
                                .shadow(color: .black.opacity(0.1), radius: 20, x: 0, y: 10)
                        )
                        .padding(.horizontal, 24)

                        Spacer()
                            .frame(height: geometry.size.height * 0.1)
                    }
                }
            }
        }
        .navigationBarHidden(true)
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
