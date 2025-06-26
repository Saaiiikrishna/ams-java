import Foundation

struct AuthRequest: Codable {
    let username: String
    let password: String
}

struct AuthResponse: Codable {
    let token: String
}

struct Session: Codable, Identifiable {
    let id: String
    let name: String
}

struct SessionCreateRequest: Codable { // Added
    let name: String
}

class APIClient {
    static let shared = APIClient()
    var token: String? // In-memory token
    private let dynamicAPIService = DynamicAPIService.shared

    // Updated login method
    func login(username: String, password: String, completion: @escaping (Result<Void, APIError>) -> Void) {
        let baseURL = dynamicAPIService.getAPIBaseURL()
        guard let url = URL(string: baseURL + "api/auth/login") else {
            completion(.failure(APIError.invalidURLOrToken))
            return
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        do {
            request.httpBody = try JSONEncoder().encode(AuthRequest(username: username, password: password))
        } catch {
            completion(.failure(APIError.encodingError(error)))
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(APIError.serverError(message: error.localizedDescription, statusCode: nil))) // Or a more specific network error
                return
            }
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                let errorMsg = self.parseErrorMessage(data: data, response: response)
                completion(.failure(APIError.serverError(message: errorMsg ?? "Login failed", statusCode: (response as? HTTPURLResponse)?.statusCode)))
                return
            }
            guard let data = data else {
                completion(.failure(APIError.noData))
                return
            }
            do {
                let authResponse = try JSONDecoder().decode(AuthResponse.self, from: data)
                self.token = authResponse.token
                KeychainHelper.standard.save(authResponse.token, service: "token", account: "jwt")
                completion(.success(()))
            } catch {
                completion(.failure(APIError.decodingError(error)))
            }
        }.resume()
    }

    // Updated fetchSessions method
    func fetchSessions(completion: @escaping (Result<[Session], APIError>) -> Void) {
        guard let token = token ?? KeychainHelper.standard.read(service: "token", account: "jwt"),
              let url = URL(string: "http://localhost:8080/api/sessions") else {
            completion(.failure(APIError.invalidURLOrToken))
            return
        }
        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(APIError.serverError(message: error.localizedDescription, statusCode: nil)))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                let errorMsg = self.parseErrorMessage(data: data, response: response)
                completion(.failure(APIError.serverError(message: errorMsg ?? "Fetch sessions failed", statusCode: (response as? HTTPURLResponse)?.statusCode)))
                return
            }
            guard let data = data else {
                completion(.failure(APIError.noData))
                return
            }
            do {
                let sessions = try JSONDecoder().decode([Session].self, from: data)
                completion(.success(sessions))
            } catch {
                completion(.failure(APIError.decodingError(error)))
            }
        }.resume()
    }

    // Updated createSession method
    func createSession(name: String, completion: @escaping (Result<Session, APIError>) -> Void) {
        guard let token = token ?? KeychainHelper.standard.read(service: "token", account: "jwt"),
              let url = URL(string: "http://localhost:8080/api/sessions") else {
            completion(.failure(APIError.invalidURLOrToken))
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        do {
            request.httpBody = try JSONEncoder().encode(SessionCreateRequest(name: name))
        } catch {
            completion(.failure(APIError.encodingError(error)))
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(APIError.serverError(message: error.localizedDescription, statusCode: nil)))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                let errorMsg = self.parseErrorMessage(data: data, response: response)
                completion(.failure(APIError.serverError(message: errorMsg ?? "Create session failed", statusCode: (response as? HTTPURLResponse)?.statusCode)))
                return
            }
            guard let data = data else {
                completion(.failure(APIError.noData))
                return
            }
            do {
                let createdSession = try JSONDecoder().decode(Session.self, from: data)
                completion(.success(createdSession))
            } catch {
                completion(.failure(APIError.decodingError(error)))
            }
        }.resume()
    }

    // MARK: - Subscriber Methods (ensure Error type is APIError)

    func fetchSubscribers(completion: @escaping (Result<[Subscriber], APIError>) -> Void) {
        guard let token = token ?? KeychainHelper.standard.read(service: "token", account: "jwt"),
              let url = URL(string: "http://localhost:8080/api/subscribers") else {
            completion(.failure(APIError.invalidURLOrToken))
            return
        }

        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                // Pass underlying error, wrapped in APIError or handle as specific network error
                completion(.failure(APIError.serverError(message: error.localizedDescription, statusCode: nil)))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                let errorMsg = self.parseErrorMessage(data: data, response: response)
                completion(.failure(APIError.serverError(message: errorMsg ?? "Fetch subscribers failed", statusCode: (response as? HTTPURLResponse)?.statusCode)))
                return
            }
            guard let data = data else {
                completion(.failure(APIError.noData))
                return
            }
            do {
                let subscribers = try JSONDecoder().decode([Subscriber].self, from: data)
                completion(.success(subscribers))
            } catch {
                completion(.failure(APIError.decodingError(error)))
            }
        }.resume()
    }

    func createSubscriber(requestBody: SubscriberRequest, completion: @escaping (Result<Subscriber, APIError>) -> Void) {
        guard let token = token ?? KeychainHelper.standard.read(service: "token", account: "jwt"),
              let url = URL(string: "http://localhost:8080/api/subscribers") else {
            completion(.failure(APIError.invalidURLOrToken))
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        do {
            request.httpBody = try JSONEncoder().encode(requestBody)
        } catch {
            completion(.failure(APIError.encodingError(error)))
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(APIError.serverError(message: error.localizedDescription, statusCode: nil)))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                let errorMsg = self.parseErrorMessage(data: data, response: response)
                completion(.failure(APIError.serverError(message: errorMsg ?? "Create subscriber failed", statusCode: (response as? HTTPURLResponse)?.statusCode)))
                return
            }
            guard let data = data else {
                completion(.failure(APIError.noData))
                return
            }
            do {
                let subscriber = try JSONDecoder().decode(Subscriber.self, from: data)
                completion(.success(subscriber))
            } catch {
                completion(.failure(APIError.decodingError(error)))
            }
        }.resume()
    }

    func updateSubscriber(subscriberId: String, requestBody: SubscriberRequest, completion: @escaping (Result<Subscriber, APIError>) -> Void) {
        guard let token = token ?? KeychainHelper.standard.read(service: "token", account: "jwt"),
              let url = URL(string: "http://localhost:8080/admin/subscribers/\(subscriberId)") else {
            completion(.failure(APIError.invalidURLOrToken))
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        do {
            request.httpBody = try JSONEncoder().encode(requestBody)
        } catch {
            completion(.failure(APIError.encodingError(error)))
            return
        }

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(APIError.serverError(message: error.localizedDescription, statusCode: nil)))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                let errorMsg = self.parseErrorMessage(data: data, response: response)
                completion(.failure(APIError.serverError(message: errorMsg ?? "Update subscriber failed", statusCode: (response as? HTTPURLResponse)?.statusCode)))
                return
            }
            guard let data = data else {
                completion(.failure(APIError.noData))
                return
            }
            do {
                let subscriber = try JSONDecoder().decode(Subscriber.self, from: data)
                completion(.success(subscriber))
            } catch {
                completion(.failure(APIError.decodingError(error)))
            }
        }.resume()
    }

    func deleteSubscriber(subscriberId: String, completion: @escaping (Result<Void, APIError>) -> Void) {
        guard let token = token ?? KeychainHelper.standard.read(service: "token", account: "jwt"),
              let url = URL(string: "http://localhost:8080/admin/subscribers/\(subscriberId)") else {
            completion(.failure(APIError.invalidURLOrToken))
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        URLSession.shared.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(APIError.serverError(message: error.localizedDescription, statusCode: nil)))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                 let errorMsg = self.parseErrorMessage(data: data, response: response)
                completion(.failure(APIError.serverError(message: errorMsg ?? "Delete subscriber failed", statusCode: (response as? HTTPURLResponse)?.statusCode)))
                return
            }
            if httpResponse.statusCode == 204 || httpResponse.statusCode == 200 {
                 completion(.success(()))
            } else {
                completion(.failure(APIError.unknownError))
            }
        }.resume()
    }

    // Helper to parse error messages
    private func parseErrorMessage(data: Data?, response: URLResponse?) -> String? {
        guard let data = data, let httpResponse = response as? HTTPURLResponse, !(200...299).contains(httpResponse.statusCode) else {
            return nil
        }
        if let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
           let message = json["message"] as? String {
            return message
        } else if let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any], // Check for "error" too
                  let errorStr = json["error"] as? String {
            return errorStr
        }
        // Fallback if "message" or "error" not found or not a string
        if let responseString = String(data: data, encoding: .utf8), !responseString.isEmpty {
            return responseString // Return raw response string if it's not JSON or doesn't match expected structure
        }
        return "Server error occurred."
    }

    // Define APIError enum
    enum APIError: Error, LocalizedError {
        case invalidURLOrToken
        case noData
        case decodingError(Error)
        case encodingError(Error)
        case serverError(message: String, statusCode: Int?)
        case unknownError

        var errorDescription: String? {
            switch self {
            case .invalidURLOrToken: return "Invalid URL or authentication token."
            case .noData: return "No data received from server."
            case .decodingError(let error): return "Failed to decode response: \(error.localizedDescription)"
            case .encodingError(let error): return "Failed to encode request: \(error.localizedDescription)"
            case .serverError(let message, let statusCode):
                var finalMessage = message
                if let code = statusCode, message == "Server error occurred." { // Avoid "Server error occurred. (Status: 500)"
                     return "Server error (Status: \(code))"
                } else if let code = statusCode {
                    finalMessage += " (Status: \(code))"
                }
                return finalMessage
            case .unknownError: return "An unknown error occurred."
            }
        }
    }

    func logout() { // Added
        self.token = nil // Clear in-memory token
        KeychainHelper.standard.delete(service: "token", account: "jwt") // Delete from Keychain
    }
}
