import SwiftUI

struct LoginView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var mobileNumber = ""
    @State private var pin = ""
    @State private var showPin = false
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // App Header
                VStack(spacing: 8) {
                    Image(systemName: "person.badge.clock")
                        .font(.system(size: 60))
                        .foregroundColor(.blue)
                    
                    Text("Attendance App")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    
                    Text("Subscriber Check-in")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 40)
                
                // Input Fields
                VStack(spacing: 16) {
                    // Mobile Number
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Mobile Number")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        TextField("Enter mobile number", text: $mobileNumber)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                            .keyboardType(.phonePad)
                    }

                    // PIN Input
                    pinLoginSection
                }
                .padding(.horizontal)
                
                // Error/Success Messages
                if case .error(let message) = authViewModel.authState {
                    Text(message)
                        .foregroundColor(.red)
                        .padding()
                        .background(Color.red.opacity(0.1))
                        .cornerRadius(8)
                        .padding(.horizontal)
                }
                
                if let successMessage = authViewModel.successMessage {
                    Text(successMessage)
                        .foregroundColor(.green)
                        .padding()
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(8)
                        .padding(.horizontal)
                }
                
                Spacer()
            }
        }
        .navigationBarHidden(true)
    }
    
    private var pinLoginSection: some View {
        VStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text("4-Digit PIN")
                    .font(.caption)
                    .foregroundColor(.secondary)

                HStack {
                    if showPin {
                        TextField("0000", text: $pin)
                            .keyboardType(.numberPad)
                            .onChange(of: pin) { newValue in
                                // Limit to 4 digits
                                let filtered = String(newValue.prefix(4).filter { $0.isNumber })
                                if filtered != newValue {
                                    pin = filtered
                                }
                            }
                    } else {
                        SecureField("0000", text: $pin)
                            .keyboardType(.numberPad)
                            .onChange(of: pin) { newValue in
                                // Limit to 4 digits
                                let filtered = String(newValue.prefix(4).filter { $0.isNumber })
                                if filtered != newValue {
                                    pin = filtered
                                }
                            }
                    }

                    Button(action: { showPin.toggle() }) {
                        Image(systemName: showPin ? "eye.slash" : "eye")
                            .foregroundColor(.secondary)
                    }
                }
                .textFieldStyle(RoundedBorderTextFieldStyle())
            }

            Button(action: loginWithPin) {
                HStack {
                    if case .loading = authViewModel.authState {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(0.8)
                    }
                    Text("Login")
                        .fontWeight(.semibold)
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(isLoginEnabled ? Color.blue : Color.gray)
                .foregroundColor(.white)
                .cornerRadius(10)
            }
            .disabled(!isLoginEnabled || authViewModel.authState == .loading)
        }
    }
    

    
    private var isLoginEnabled: Bool {
        !mobileNumber.isEmpty && pin.count == 4
    }

    private func loginWithPin() {
        authViewModel.loginWithPin(
            mobileNumber: mobileNumber,
            pin: pin
        )
    }

}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
            .environmentObject(AuthViewModel())
    }
}
