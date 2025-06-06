import Foundation

struct AuthRequest: Codable {
    let email: String
    let password: String
}

struct AuthResponse: Codable {
    let token: String
}

struct Session: Codable, Identifiable {
    let id: String
    let name: String
}

class APIClient {
    static let shared = APIClient()
    var token: String?

    func login(email: String, password: String, completion: @escaping (Bool) -> Void) {
        guard let url = URL(string: "http://localhost:8080/admin/authenticate") else { completion(false); return }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        let body = try? JSONEncoder().encode(AuthRequest(email: email, password: password))
        request.httpBody = body

        URLSession.shared.dataTask(with: request) { data, response, error in
            guard let data = data, let auth = try? JSONDecoder().decode(AuthResponse.self, from: data) else {
                completion(false)
                return
            }
            self.token = auth.token
            KeychainHelper.standard.save(auth.token, service: "token", account: "jwt")
            completion(true)
        }.resume()
    }

    func fetchSessions(completion: @escaping ([Session]) -> Void) {
        guard let token = token ?? KeychainHelper.standard.read(service: "token", account: "jwt"),
              let url = URL(string: "http://localhost:8080/admin/sessions") else { completion([]); return }
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        URLSession.shared.dataTask(with: request) { data, _, _ in
            let sessions = (try? JSONDecoder().decode([Session].self, from: data ?? Data())) ?? []
            completion(sessions)
        }.resume()
    }
}
