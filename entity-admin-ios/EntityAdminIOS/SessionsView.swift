import SwiftUI

struct SessionsView: View {
    @State private var sessions: [Session] = []

    var body: some View {
        List(sessions) { session in
            Text(session.name)
        }
        .onAppear {
            APIClient.shared.fetchSessions { result in
                DispatchQueue.main.async {
                    self.sessions = result
                }
            }
        }
        .navigationTitle("Sessions")
    }
}

struct SessionsView_Previews: PreviewProvider {
    static var previews: some View {
        SessionsView()
    }
}
