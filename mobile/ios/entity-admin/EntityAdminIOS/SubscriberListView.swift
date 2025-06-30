// entity-admin-ios/EntityAdminIOS/SubscriberListView.swift
import SwiftUI

struct SubscriberListView: View {
    @EnvironmentObject var authManager: AuthenticationManager // Added
    @State private var subscribers: [Subscriber] = []
    @State private var isLoading: Bool = false
    @State private var errorMessage: String?

    // Placeholder for Add/Edit sheet presentation
    @State private var showingAddEditSheet = false
    // @State private var selectedSubscriberId: String? = nil // Replaced by selectedSubscriberForEdit
    @State private var selectedSubscriberForEdit: Subscriber? = nil // For editing

    // For Delete confirmation
    @State private var subscriberToDelete: Subscriber?
    @State private var showingDeleteConfirmationAlert: Bool = false

    var body: some View {
        // The NavigationView might be redundant if this view is already pushed onto a NavigationView stack.
        // However, to manage its own title and toolbar items independently, it can be useful.
        // If issues arise with double navigation bars, this is the first place to check.
        // For now, we'll assume it's managed by the parent NavigationView if pushed.
        // Let's remove the explicit NavigationView here and assume it's part of a larger flow.

        VStack {
            if isLoading {
                ProgressView("Loading Subscribers...")
                    .padding()
            } else if let error = errorMessage {
                VStack {
                    Text("Error: \(error)")
                        .foregroundColor(.red)
                        .padding()
                    Button("Retry") {
                        fetchSubscribers()
                    }
                    .buttonStyle(.bordered)
                }
            } else if subscribers.isEmpty {
                Text("No subscribers found.")
                    .padding()
            } else {
                        List { // Removed direct (subscribers) to use ForEach for tap gestures
                    ForEach(subscribers) { subscriber in
                                HStack { // Added HStack to allow content and spacer for tap area
                            VStack(alignment: .leading) {
                                Text(subscriber.name)
                                    .font(.headline)
                                Text(subscriber.email)
                                    .font(.subheadline)
                                    .foregroundColor(.gray)
                                if let nfcUid = subscriber.nfcCardUid, !nfcUid.isEmpty {
                                    Text("NFC: \(nfcUid)")
                                        .font(.caption)
                                        .foregroundColor(.orange)
                                }
                            }
                                    Spacer() // Ensures the tap area extends
                        }
                                .contentShape(Rectangle()) // Makes the whole HStack area tappable
                                .onTapGesture {
                                    self.selectedSubscriberForEdit = subscriber
                                    self.showingAddEditSheet = true
                                }
                    }
                    .onDelete(perform: prepareForDelete) // Added swipe-to-delete
                }
            }
        }
        .navigationTitle("Subscribers")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: {
                    self.selectedSubscriberForEdit = nil // Ensure it's for adding mode
                    self.showingAddEditSheet = true
                }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showingAddEditSheet) {
            AddEditSubscriberView(
                mode: selectedSubscriberForEdit == nil ? .add : .edit(selectedSubscriberForEdit!),
                onComplete: { shouldRefresh in
                    if shouldRefresh {
                        fetchSubscribers()
                    }
                }
            )
            .environmentObject(authManager) // Pass authManager to sheet
        }
        .alert(isPresented: $showingDeleteConfirmationAlert) {
            Alert(
                title: Text("Confirm Delete"),
                message: Text("Are you sure you want to delete subscriber \"\(subscriberToDelete?.name ?? "this subscriber")\"? This action cannot be undone."),
                primaryButton: .destructive(Text("Delete")) {
                    confirmDelete()
                },
                secondaryButton: .cancel {
                    subscriberToDelete = nil // Clear selection on cancel
                }
            )
        }
        .onAppear {
            fetchSubscribers() // Simplified for now, always fetch on appear
        }
    }

    private func prepareForDelete(at offsets: IndexSet) {
        // Get the subscriber to delete based on offsets
        if let index = offsets.first {
            self.subscriberToDelete = subscribers[index]
            self.showingDeleteConfirmationAlert = true
        }
    }

    private func confirmDelete() {
        guard let subscriber = subscriberToDelete else { return }

        // Not re-using global isLoading to avoid whole screen loader for a row action.
        // UI can be updated optimistically or on completion.
        // For simplicity, we will just call API and refresh.

        APIClient.shared.deleteSubscriber(subscriberId: subscriber.id) { result in
            DispatchQueue.main.async {
                switch result {
                case .success:
                    // Option 1: Remove locally (optimistic update)
                    // self.subscribers.removeAll { $0.id == subscriber.id }
                    // Option 2: Re-fetch the list to ensure consistency (safer)
                    fetchSubscribers()
                    // self.subscriberToDelete = nil // Not strictly needed if re-fetching clears/rebuilds list
                case .failure(let error):
                    self.errorMessage = (error as? APIClient.APIError)?.errorDescription ?? error.localizedDescription
                    // self.subscriberToDelete = nil // Clear selection even on error
                }
                self.subscriberToDelete = nil // Always clear after attempt
            }
        }
    }

    private func fetchSubscribers() {
        isLoading = true
        errorMessage = nil
        APIClient.shared.fetchSubscribers { result in
            DispatchQueue.main.async {
                isLoading = false
                switch result {
                case .success(let fetchedSubscribers):
                    self.subscribers = fetchedSubscribers
                case .failure(let error):
                    if let apiError = error as? APIClient.APIError {
                        self.errorMessage = apiError.errorDescription
                    } else {
                        self.errorMessage = error.localizedDescription
                    }
                }
            }
        }
    }
}

struct SubscriberListView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView { // Wrap in NavigationView for preview
             SubscriberListView()
        }
    }
}
