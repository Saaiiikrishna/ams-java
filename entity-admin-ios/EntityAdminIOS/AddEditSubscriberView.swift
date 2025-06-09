// entity-admin-ios/EntityAdminIOS/AddEditSubscriberView.swift
import SwiftUI

struct AddEditSubscriberView: View {
    enum Mode {
        case add
        case edit(Subscriber)
    }

    var mode: Mode
    var onComplete: (_ shouldRefresh: Bool) -> Void // Callback to refresh list

    @State private var name: String
    @State private var email: String
    @State private var nfcCardUid: String

    @State private var isLoading: Bool = false
    @State private var errorMessage: String?

    @Environment(\.presentationMode) var presentationMode
    @EnvironmentObject var authManager: AuthenticationManager // Added

    private var navigationTitle: String {
        switch mode {
        case .add: return "Add Subscriber"
        case .edit(let subscriber): return "Edit \(subscriber.name)" // Will use subscriber's actual name
        }
    }

    private var saveButtonText: String {
        switch mode {
        case .add: return "Create Subscriber"
        case .edit: return "Save Changes"
        }
    }

    init(mode: Mode, onComplete: @escaping (_ shouldRefresh: Bool) -> Void) {
        self.mode = mode
        self.onComplete = onComplete

        if case .edit(let subscriber) = mode {
            _name = State(initialValue: subscriber.name)
            _email = State(initialValue: subscriber.email)
            _nfcCardUid = State(initialValue: subscriber.nfcCardUid ?? "")
        } else {
            _name = State(initialValue: "")
            _email = State(initialValue: "")
            _nfcCardUid = State(initialValue: "")
        }
    }

    var body: some View {
        NavigationView { // For title and toolbar in sheet context
            Form {
                Section(header: Text("Subscriber Details")) {
                    TextField("Name", text: $name)
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                    TextField("NFC Card UID (Optional)", text: $nfcCardUid)
                }

                if let error = errorMessage {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                    }
                }

                Section {
                    Button(action: saveSubscriber) {
                        HStack {
                            Spacer()
                            if isLoading {
                                ProgressView()
                            } else {
                                Text(saveButtonText)
                            }
                            Spacer()
                        }
                    }
                    .disabled(isLoading || name.isEmpty || email.isEmpty)
                }
            }
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                        onComplete(false) // No refresh needed on cancel
                    }
                }
                // Potentially a save button here too, but Form button is common
            }
        }
    }

    private func saveSubscriber() {
        guard !name.isEmpty, !email.isEmpty else {
            errorMessage = "Name and Email are required."
            return
        }
        // Basic email validation (can be improved with regex)
        let emailPattern = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPred = NSPredicate(format:"SELF MATCHES %@", emailPattern)
        guard emailPred.evaluate(with: email) else {
            errorMessage = "Invalid email format."
            return
        }

        isLoading = true
        errorMessage = nil

        let subscriberRequest = SubscriberRequest(name: name, email: email, nfcCardUid: nfcCardUid.isEmpty ? nil : nfcCardUid)

        switch mode {
        case .add:
            APIClient.shared.createSubscriber(requestBody: subscriberRequest) { result in
                handleAPIResult(result: result)
            }
        case .edit(let existingSubscriber):
            APIClient.shared.updateSubscriber(subscriberId: existingSubscriber.id, requestBody: subscriberRequest) { result in
                handleAPIResult(result: result)
            }
        }
    }

    private func handleAPIResult(result: Result<Subscriber, Error>) {
        DispatchQueue.main.async {
            isLoading = false
            switch result {
            case .success(_):
                presentationMode.wrappedValue.dismiss()
                onComplete(true) // Signal refresh
            case .failure(let error):
                self.errorMessage = (error as? APIClient.APIError)?.errorDescription ?? error.localizedDescription
                onComplete(false) // No refresh on failure
            }
        }
    }
}

// Preview for .add mode
struct AddEditSubscriberView_Add_Previews: PreviewProvider {
    static var previews: some View {
        AddEditSubscriberView(mode: .add, onComplete: { _ in })
    }
}

// Preview for .edit mode
struct AddEditSubscriberView_Edit_Previews: PreviewProvider {
    static var previews: some View {
        let sampleSubscriber = Subscriber(id: "1", name: "Jane Doe", email: "jane@example.com", nfcCardUid: "12345ABC")
        AddEditSubscriberView(mode: .edit(sampleSubscriber), onComplete: { _ in })
    }
}
