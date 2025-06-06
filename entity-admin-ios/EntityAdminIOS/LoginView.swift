import SwiftUI

struct LoginView: View {
    @State private var email = ""
    @State private var password = ""
    @State private var showingSessions = false

    var body: some View {
        VStack(spacing: 16) {
            TextField("Email", text: $email)
                .textFieldStyle(RoundedBorderTextFieldStyle())
            SecureField("Password", text: $password)
                .textFieldStyle(RoundedBorderTextFieldStyle())
            Button("Login") {
                APIClient.shared.login(email: email, password: password) { success in
                    if success { showingSessions = true }
                }
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
        .navigationTitle("Login")
        .background(
            NavigationLink(destination: SessionsView(), isActive: $showingSessions) { EmptyView() }
        )
    }
}

struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        LoginView()
    }
}
