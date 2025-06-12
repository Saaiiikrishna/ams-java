import SwiftUI

struct ProfileView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var showingLogoutAlert = false
    @State private var showingPINChangeSheet = false
    @State private var showingProfileEditSheet = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Profile Header
                    if let user = authViewModel.currentUser {
                        ProfileHeaderView(user: user)
                    }
                    
                    // Personal Information Section
                    if let user = authViewModel.currentUser {
                        PersonalInfoSection(user: user)
                    }
                    
                    // Account Actions Section
                    AccountActionsSection(
                        onChangePIN: { showingPINChangeSheet = true },
                        onEditProfile: { showingProfileEditSheet = true }
                    )
                    
                    // Logout Section
                    LogoutSection(onLogout: { showingLogoutAlert = true })
                }
                .padding()
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.large)
            .alert("Logout", isPresented: $showingLogoutAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Logout", role: .destructive) {
                    authViewModel.logout()
                }
            } message: {
                Text("Are you sure you want to logout?")
            }
            .sheet(isPresented: $showingPINChangeSheet) {
                PINChangeView()
            }
            .sheet(isPresented: $showingProfileEditSheet) {
                ProfileEditView()
                    .environmentObject(authViewModel)
            }
        }
    }
}

struct ProfileHeaderView: View {
    let user: Subscriber
    
    var body: some View {
        VStack(spacing: 16) {
            // Profile Image
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            gradient: Gradient(colors: [.blue, .purple]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 100, height: 100)
                
                Text("\(user.firstName.prefix(1))\(user.lastName.prefix(1))")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }
            
            // User Name
            Text("\(user.firstName) \(user.lastName)")
                .font(.title2)
                .fontWeight(.bold)
            
            // Mobile Number
            Text(user.mobileNumber)
                .font(.body)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(12)
    }
}

struct PersonalInfoSection: View {
    let user: Subscriber
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Personal Information")
                .font(.headline)
                .fontWeight(.semibold)
            
            VStack(spacing: 12) {
                InfoRow(icon: "person.fill", label: "First Name", value: user.firstName)
                InfoRow(icon: "person.fill", label: "Last Name", value: user.lastName)
                InfoRow(icon: "phone.fill", label: "Mobile Number", value: user.mobileNumber)
                
                if let email = user.email {
                    InfoRow(icon: "envelope.fill", label: "Email", value: email)
                }
                
                InfoRow(
                    icon: user.hasNfcCard ? "creditcard.fill" : "creditcard",
                    label: "NFC Card",
                    value: user.hasNfcCard ? "Assigned" : "Not Assigned"
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .gray.opacity(0.2), radius: 2, x: 0, y: 1)
    }
}

struct InfoRow: View {
    let icon: String
    let label: String
    let value: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.body)
                .foregroundColor(.blue)
                .frame(width: 20)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Text(value)
                    .font(.body)
                    .fontWeight(.medium)
            }
            
            Spacer()
        }
    }
}

struct AccountActionsSection: View {
    let onChangePIN: () -> Void
    let onEditProfile: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Account Actions")
                .font(.headline)
                .fontWeight(.semibold)
            
            VStack(spacing: 12) {
                ActionButton(
                    icon: "lock.fill",
                    title: "Change PIN",
                    subtitle: "Update your 4-digit PIN",
                    action: onChangePIN
                )
                
                ActionButton(
                    icon: "pencil",
                    title: "Edit Profile",
                    subtitle: "Update your personal information",
                    action: onEditProfile
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: .gray.opacity(0.2), radius: 2, x: 0, y: 1)
    }
}

struct ActionButton: View {
    let icon: String
    let title: String
    let subtitle: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundColor(.blue)
                    .frame(width: 24)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.body)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                    
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct LogoutSection: View {
    let onLogout: () -> Void
    
    var body: some View {
        Button(action: onLogout) {
            HStack {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .font(.title3)
                
                Text("Logout")
                    .font(.body)
                    .fontWeight(.medium)
            }
            .foregroundColor(.red)
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.red.opacity(0.1))
            .cornerRadius(12)
        }
    }
}

// Placeholder views for sheets
struct PINChangeView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var currentPIN = ""
    @State private var newPIN = ""
    @State private var confirmPIN = ""
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Change PIN")
                    .font(.title2)
                    .fontWeight(.bold)
                    .padding(.top)
                
                VStack(spacing: 16) {
                    SecureField("Current PIN", text: $currentPIN)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .keyboardType(.numberPad)
                    
                    SecureField("New PIN", text: $newPIN)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .keyboardType(.numberPad)
                    
                    SecureField("Confirm New PIN", text: $confirmPIN)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .keyboardType(.numberPad)
                }
                
                Button("Update PIN") {
                    // TODO: Implement PIN change
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
                .disabled(currentPIN.isEmpty || newPIN.isEmpty || confirmPIN.isEmpty)
                
                Spacer()
            }
            .padding()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }
}

struct ProfileEditView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var firstName = ""
    @State private var lastName = ""
    @State private var email = ""
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                Text("Edit Profile")
                    .font(.title2)
                    .fontWeight(.bold)
                    .padding(.top)
                
                VStack(spacing: 16) {
                    TextField("First Name", text: $firstName)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                    
                    TextField("Last Name", text: $lastName)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                    
                    TextField("Email", text: $email)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                }
                
                Button("Save Changes") {
                    // TODO: Implement profile update
                    dismiss()
                }
                .buttonStyle(.borderedProminent)
                .disabled(firstName.isEmpty || lastName.isEmpty)
                
                Spacer()
            }
            .padding()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .onAppear {
                if let user = authViewModel.currentUser {
                    firstName = user.firstName
                    lastName = user.lastName
                    email = user.email ?? ""
                }
            }
        }
    }
}

struct ProfileView_Previews: PreviewProvider {
    static var previews: some View {
        ProfileView()
            .environmentObject(AuthViewModel())
    }
}
