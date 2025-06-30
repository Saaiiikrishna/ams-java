// entity-admin-ios/EntityAdminIOS/CreateSessionView.swift
import SwiftUI

struct CreateSessionView: View {
    @State private var sessionName: String = ""
    @Environment(\.presentationMode) var presentationMode
    @State private var errorMessage: String?
    @State private var isLoading: Bool = false

    // Closure to be called when session creation is triggered
    var onCreate: (String, @escaping (Bool, String?) -> Void) -> Void

    var body: some View {
        NavigationView { // Embed in NavigationView for title and toolbar items
            VStack(spacing: 20) {
                // Text("Create New Session") // Title is handled by navigationTitle now
                //    .font(.headline)

                TextField("Session Name or Purpose", text: $sessionName)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.horizontal)

                if let error = errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .padding(.horizontal)
                }

                Button(action: {
                    guard !sessionName.isEmpty else {
                        errorMessage = "Session name cannot be empty."
                        return
                    }
                    isLoading = true
                    errorMessage = nil
                    onCreate(sessionName) { success, errorMsg in
                        isLoading = false
                        if success {
                            presentationMode.wrappedValue.dismiss()
                        } else {
                            errorMessage = errorMsg ?? "Failed to create session."
                        }
                    }
                }) {
                    if isLoading {
                        ProgressView()
                    } else {
                        Text("Create Session")
                    }
                }
                .buttonStyle(.borderedProminent)
                .disabled(isLoading || sessionName.isEmpty)

                Spacer()
            }
            .padding(.top, 20) // Reduced top padding as NavigationView provides space
            .navigationTitle("New Session")
            .navigationBarTitleDisplayMode(.inline) // Use inline for sheet view title
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
    }
}
