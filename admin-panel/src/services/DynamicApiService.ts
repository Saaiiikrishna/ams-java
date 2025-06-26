/**
 * Dynamic API Service for Admin Panel
 * Provides dynamic server discovery and connection management
 * Replaces hardcoded IP addresses with intelligent network discovery
 */

interface ServerInfo {
  url: string;
  lastTested: number;
  isWorking: boolean;
  responseTime?: number;
}

class DynamicApiService {
  private static instance: DynamicApiService;
  private currentServerUrl: string | null = null;
  private serverCache: Map<string, ServerInfo> = new Map();
  private isDiscovering: boolean = false;
  private discoveryCallbacks: Array<(url: string | null) => void> = [];
  
  // Configuration
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
  private readonly DISCOVERY_TIMEOUT = 3000; // 3 seconds per server test
  private readonly HEALTH_ENDPOINT = '/api/health';
  private readonly STORAGE_KEY = 'admin_panel_server_url';

  private constructor() {
    this.loadCachedServer();
    this.startPeriodicHealthCheck();
  }

  public static getInstance(): DynamicApiService {
    if (!DynamicApiService.instance) {
      DynamicApiService.instance = new DynamicApiService();
    }
    return DynamicApiService.instance;
  }

  /**
   * Get the current API base URL
   */
  public async getApiBaseUrl(): Promise<string> {
    // If we're served from the backend (port 8080), use relative URLs
    if (window.location.port === '8080') {
      console.log('✅ Admin Panel: Using same-origin API calls (relative URLs)');
      return '';
    }

    // If we have a cached working server, use it
    if (this.currentServerUrl && this.isServerCacheValid(this.currentServerUrl)) {
      console.log('✅ Admin Panel: Using cached server:', this.currentServerUrl);
      return this.currentServerUrl;
    }

    // Discover a new server
    const discoveredUrl = await this.discoverServer();
    if (discoveredUrl) {
      this.currentServerUrl = discoveredUrl;
      this.cacheServer(discoveredUrl);
      return discoveredUrl;
    }

    // Fallback to current host
    const fallbackUrl = `http://${window.location.hostname}:8080`;
    console.warn('⚠️ Admin Panel: Using fallback server:', fallbackUrl);
    return fallbackUrl;
  }

  /**
   * Discover available servers
   */
  private async discoverServer(): Promise<string | null> {
    if (this.isDiscovering) {
      return new Promise((resolve) => {
        this.discoveryCallbacks.push(resolve);
      });
    }

    this.isDiscovering = true;
    console.log('🚀 Admin Panel: Starting server discovery...');

    try {
      // Get potential server URLs
      const serverUrls = this.getPotentialServers();
      console.log('🔍 Admin Panel: Testing', serverUrls.length, 'potential servers...');

      // Test servers in parallel with timeout
      const testPromises = serverUrls.map(url => this.testServerWithTimeout(url));
      const results = await Promise.allSettled(testPromises);

      // Find the first working server
      for (let i = 0; i < results.length; i++) {
        const result = results[i];
        if (result.status === 'fulfilled' && result.value) {
          const workingUrl = serverUrls[i];
          console.log('✅ Admin Panel: Found working server:', workingUrl);
          
          // Notify waiting callbacks
          this.discoveryCallbacks.forEach(callback => callback(workingUrl));
          this.discoveryCallbacks = [];
          
          return workingUrl;
        }
      }

      console.warn('❌ Admin Panel: No working servers found');
      this.discoveryCallbacks.forEach(callback => callback(null));
      this.discoveryCallbacks = [];
      
      return null;

    } catch (error) {
      console.error('❌ Admin Panel: Server discovery failed:', error);
      this.discoveryCallbacks.forEach(callback => callback(null));
      this.discoveryCallbacks = [];
      return null;
    } finally {
      this.isDiscovering = false;
    }
  }

  /**
   * Get list of potential servers to test
   */
  private getPotentialServers(): string[] {
    const servers: string[] = [];
    const currentHost = window.location.hostname;

    // Add current host first (most likely to work)
    servers.push(`http://${currentHost}:8080`);

    // Add mDNS hostname
    servers.push('http://restaurant.local:8080');

    // Add common network addresses based on current host
    if (currentHost !== 'localhost' && currentHost !== '127.0.0.1') {
      const networkBase = this.getNetworkBase(currentHost);
      if (networkBase) {
        // Add common server IPs in the same network
        servers.push(`http://${networkBase}.1:8080`);   // Gateway
        servers.push(`http://${networkBase}.4:8080`);   // Common server IP
        servers.push(`http://${networkBase}.100:8080`); // Common server IP
        servers.push(`http://${networkBase}.209:8080`); // Alternative server
      }
    }

    // Add localhost as fallback
    servers.push('http://localhost:8080');
    servers.push('http://127.0.0.1:8080');

    // Remove duplicates and return
    return [...new Set(servers)];
  }

  /**
   * Get network base from IP address (e.g., "192.168.1" from "192.168.1.100")
   */
  private getNetworkBase(ip: string): string | null {
    const parts = ip.split('.');
    if (parts.length >= 3 && parts.every(part => /^\d+$/.test(part))) {
      return `${parts[0]}.${parts[1]}.${parts[2]}`;
    }
    return null;
  }

  /**
   * Test a server with timeout
   */
  private async testServerWithTimeout(url: string): Promise<boolean> {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), this.DISCOVERY_TIMEOUT);

    try {
      const startTime = Date.now();
      const response = await fetch(url + this.HEALTH_ENDPOINT, {
        method: 'GET',
        signal: controller.signal,
        headers: {
          'Accept': 'application/json',
        },
      });

      const responseTime = Date.now() - startTime;
      const isWorking = response.ok;

      // Cache the result
      this.serverCache.set(url, {
        url,
        lastTested: Date.now(),
        isWorking,
        responseTime,
      });

      console.log(`🔍 Admin Panel: ${url} - ${isWorking ? '✅ PASS' : '❌ FAIL'} (${responseTime}ms)`);
      return isWorking;

    } catch (error) {
      console.log(`🔍 Admin Panel: ${url} - ❌ FAIL (${error instanceof Error ? error.message : 'Unknown error'})`);
      
      // Cache the failure
      this.serverCache.set(url, {
        url,
        lastTested: Date.now(),
        isWorking: false,
      });

      return false;
    } finally {
      clearTimeout(timeoutId);
    }
  }

  /**
   * Check if server cache is still valid
   */
  private isServerCacheValid(url: string): boolean {
    const cached = this.serverCache.get(url);
    if (!cached) return false;
    
    const age = Date.now() - cached.lastTested;
    return age < this.CACHE_DURATION && cached.isWorking;
  }

  /**
   * Load cached server from localStorage
   */
  private loadCachedServer(): void {
    try {
      const cached = localStorage.getItem(this.STORAGE_KEY);
      if (cached) {
        this.currentServerUrl = cached;
        console.log('📱 Admin Panel: Loaded cached server:', cached);
      }
    } catch (error) {
      console.warn('⚠️ Admin Panel: Failed to load cached server:', error);
    }
  }

  /**
   * Cache server URL in localStorage
   */
  private cacheServer(url: string): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, url);
      console.log('💾 Admin Panel: Cached server:', url);
    } catch (error) {
      console.warn('⚠️ Admin Panel: Failed to cache server:', error);
    }
  }

  /**
   * Start periodic health check
   */
  private startPeriodicHealthCheck(): void {
    setInterval(async () => {
      if (this.currentServerUrl && !this.isDiscovering) {
        const isHealthy = await this.testServerWithTimeout(this.currentServerUrl);
        if (!isHealthy) {
          console.warn('⚠️ Admin Panel: Current server is unhealthy, rediscovering...');
          this.currentServerUrl = null;
          localStorage.removeItem(this.STORAGE_KEY);
        }
      }
    }, 60000); // Check every minute
  }

  /**
   * Force rediscovery of servers
   */
  public async forceRediscovery(): Promise<string | null> {
    console.log('🔄 Admin Panel: Forcing server rediscovery...');
    this.currentServerUrl = null;
    this.serverCache.clear();
    localStorage.removeItem(this.STORAGE_KEY);
    
    return await this.discoverServer();
  }

  /**
   * Get current server info
   */
  public getCurrentServerInfo(): { url: string | null; isDiscovering: boolean } {
    return {
      url: this.currentServerUrl,
      isDiscovering: this.isDiscovering,
    };
  }

  /**
   * Test current server health
   */
  public async testCurrentServer(): Promise<boolean> {
    if (!this.currentServerUrl) return false;
    return await this.testServerWithTimeout(this.currentServerUrl);
  }
}

export default DynamicApiService;
