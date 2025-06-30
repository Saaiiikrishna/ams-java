import Foundation
import Combine
import Network

/**
 * Dynamic API Service for Entity Admin with mDNS discovery and automatic server detection
 * Replaces hardcoded localhost with dynamic network discovery
 */
class DynamicAPIService: ObservableObject {
    static let shared = DynamicAPIService()
    
    @Published var currentServerURL: String?
    @Published var isDiscovering: Bool = false
    @Published var discoveryStatus: String = "Not started"
    
    private let session = URLSession.shared
    private var cancellables = Set<AnyCancellable>()
    private let networkMonitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "NetworkMonitor")
    
    // Server discovery configuration
    private let serviceType = "_attendanceapi._tcp"
    private let serviceDomain = "local."
    private let healthEndpoint = "api/health"
    
    // Fallback servers for testing
    private let fallbackServers = [
        "http://192.168.31.4:8080/",      // Current known server
        "http://192.168.31.209:8080/",    // Alternative server
        "http://restaurant.local:8080/",   // mDNS hostname
        "http://localhost:8080/",          // Local development
        "http://127.0.0.1:8080/"          // Localhost IP
    ]
    
    private init() {
        startNetworkMonitoring()
        discoverServer()
    }
    
    deinit {
        networkMonitor.cancel()
    }
    
    // MARK: - Network Monitoring
    
    private func startNetworkMonitoring() {
        networkMonitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                if path.status == .satisfied {
                    print("📶 Entity Admin: Network connection available")
                    // Rediscover server when network changes
                    self?.discoverServer()
                } else {
                    print("❌ Entity Admin: Network connection lost")
                    self?.discoveryStatus = "Network unavailable"
                }
            }
        }
        networkMonitor.start(queue: monitorQueue)
    }
    
    // MARK: - Server Discovery
    
    func discoverServer() {
        guard !isDiscovering else { return }
        
        isDiscovering = true
        discoveryStatus = "Discovering server..."
        
        print("🚀 Entity Admin: Starting server discovery...")
        
        // Try cached server first
        if let cachedServer = getCachedServer() {
            print("🔍 Entity Admin: Testing cached server: \(cachedServer)")
            testServer(url: cachedServer) { [weak self] success in
                if success {
                    print("✅ Entity Admin: Cached server is working: \(cachedServer)")
                    self?.setCurrentServer(cachedServer)
                    return
                }
                
                // Cached server failed, try discovery
                self?.performServerDiscovery()
            }
        } else {
            performServerDiscovery()
        }
    }
    
    private func performServerDiscovery() {
        print("📡 Entity Admin: Starting mDNS discovery...")
        discoveryStatus = "Scanning network..."
        
        // Try mDNS discovery first
        discoverViaMDNS { [weak self] discoveredURL in
            if let url = discoveredURL {
                print("✅ Entity Admin: mDNS discovery successful: \(url)")
                self?.setCurrentServer(url)
            } else {
                print("❌ Entity Admin: mDNS discovery failed, trying fallback servers...")
                self?.tryFallbackServers()
            }
        }
    }
    
    private func discoverViaMDNS(completion: @escaping (String?) -> Void) {
        // For iOS, we'll use a simplified approach since iOS doesn't have built-in mDNS discovery
        // We'll try to resolve the mDNS hostname directly
        
        let hostname = "restaurant.local"
        
        // Try to resolve the hostname
        var hints = addrinfo()
        hints.ai_family = AF_INET // IPv4
        hints.ai_socktype = SOCK_STREAM
        
        var result: UnsafeMutablePointer<addrinfo>?
        let status = getaddrinfo(hostname, "8080", &hints, &result)
        
        defer {
            if let result = result {
                freeaddrinfo(result)
            }
        }
        
        if status == 0, let result = result {
            var addr = result.pointee.ai_addr.pointee
            let ipString = String(cString: inet_ntoa(addr.sa_data.withUnsafeBytes { $0.load(as: in_addr.self) }))
            let serverURL = "http://\(ipString):8080/"
            
            print("🏠 Entity Admin: Resolved \(hostname) to \(ipString)")
            
            // Test the resolved server
            testServer(url: serverURL) { success in
                completion(success ? serverURL : nil)
            }
        } else {
            print("❌ Entity Admin: Failed to resolve \(hostname)")
            completion(nil)
        }
    }
    
    private func tryFallbackServers() {
        discoveryStatus = "Trying fallback servers..."
        
        let group = DispatchGroup()
        var workingServer: String?
        
        for server in fallbackServers {
            group.enter()
            testServer(url: server) { success in
                if success && workingServer == nil {
                    workingServer = server
                }
                group.leave()
            }
        }
        
        group.notify(queue: .main) { [weak self] in
            if let server = workingServer {
                print("✅ Entity Admin: Fallback server found: \(server)")
                self?.setCurrentServer(server)
            } else {
                print("❌ Entity Admin: No working servers found")
                self?.discoveryStatus = "No servers available"
                self?.isDiscovering = false
                // Use last resort server
                self?.setCurrentServer(self?.fallbackServers.first ?? "http://localhost:8080/")
            }
        }
    }
    
    // MARK: - Server Testing
    
    private func testServer(url: String, completion: @escaping (Bool) -> Void) {
        let healthURL = url.hasSuffix("/") ? url + healthEndpoint : url + "/" + healthEndpoint
        
        guard let testURL = URL(string: healthURL) else {
            completion(false)
            return
        }
        
        var request = URLRequest(url: testURL)
        request.timeoutInterval = 5.0
        request.httpMethod = "GET"
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            let success = error == nil &&
                         (response as? HTTPURLResponse)?.statusCode == 200 &&
                         data?.isEmpty == false
            
            DispatchQueue.main.async {
                completion(success)
            }
        }.resume()
    }
    
    // MARK: - Server Management
    
    private func setCurrentServer(_ url: String) {
        currentServerURL = url
        cacheServer(url)
        discoveryStatus = "Connected to \(url)"
        isDiscovering = false
        
        print("🎯 Entity Admin: Current server set to: \(url)")
    }
    
    private func getCachedServer() -> String? {
        return UserDefaults.standard.string(forKey: "entity_admin_cached_server_url")
    }
    
    private func cacheServer(_ url: String) {
        UserDefaults.standard.set(url, forKey: "entity_admin_cached_server_url")
    }
    
    func clearCache() {
        UserDefaults.standard.removeObject(forKey: "entity_admin_cached_server_url")
        currentServerURL = nil
        discoveryStatus = "Cache cleared"
    }
    
    func forceRediscovery() {
        clearCache()
        discoverServer()
    }
    
    // MARK: - API Methods
    
    func getBaseURL() -> String {
        return currentServerURL ?? "http://localhost:8080/"
    }
    
    func getAPIBaseURL() -> String {
        let baseURL = getBaseURL()
        return baseURL.hasSuffix("/") ? baseURL : baseURL + "/"
    }
}

// MARK: - Network Reachability Extension

extension DynamicAPIService {
    func isNetworkAvailable() -> Bool {
        return networkMonitor.currentPath.status == .satisfied
    }
    
    func getNetworkType() -> String {
        let path = networkMonitor.currentPath
        if path.usesInterfaceType(.wifi) {
            return "WiFi"
        } else if path.usesInterfaceType(.cellular) {
            return "Cellular"
        } else if path.usesInterfaceType(.wiredEthernet) {
            return "Ethernet"
        } else {
            return "Unknown"
        }
    }
}
