/**
 * Centralized logging service for the entity dashboard
 * Provides structured logging with different levels and context
 */

export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
}

interface LogEntry {
  timestamp: string;
  level: LogLevel;
  message: string;
  context?: string;
  data?: any;
  error?: Error;
}

class LoggingService {
  private logLevel: LogLevel = LogLevel.INFO;
  private logs: LogEntry[] = [];
  private maxLogs: number = 1000;

  constructor() {
    // Set log level based on environment
    if (process.env.NODE_ENV === 'development') {
      this.logLevel = LogLevel.DEBUG;
    } else {
      this.logLevel = LogLevel.INFO;
    }
  }

  private shouldLog(level: LogLevel): boolean {
    return level >= this.logLevel;
  }

  private createLogEntry(level: LogLevel, message: string, context?: string, data?: any, error?: Error): LogEntry {
    return {
      timestamp: new Date().toISOString(),
      level,
      message,
      context,
      data,
      error,
    };
  }

  private addLog(entry: LogEntry): void {
    this.logs.push(entry);
    
    // Keep only the most recent logs
    if (this.logs.length > this.maxLogs) {
      this.logs = this.logs.slice(-this.maxLogs);
    }

    // Console output for development (errors only)
    if (process.env.NODE_ENV === 'development' && entry.level === LogLevel.ERROR) {
      const logMessage = `[${entry.timestamp}] [${LogLevel[entry.level]}] ${entry.context ? `[${entry.context}] ` : ''}${entry.message}`;
      console.error(logMessage, entry.data, entry.error);
    }
  }

  debug(message: string, context?: string, data?: any): void {
    if (this.shouldLog(LogLevel.DEBUG)) {
      this.addLog(this.createLogEntry(LogLevel.DEBUG, message, context, data));
    }
  }

  info(message: string, context?: string, data?: any): void {
    if (this.shouldLog(LogLevel.INFO)) {
      this.addLog(this.createLogEntry(LogLevel.INFO, message, context, data));
    }
  }

  warn(message: string, context?: string, data?: any): void {
    if (this.shouldLog(LogLevel.WARN)) {
      this.addLog(this.createLogEntry(LogLevel.WARN, message, context, data));
    }
  }

  error(message: string, context?: string, error?: Error, data?: any): void {
    if (this.shouldLog(LogLevel.ERROR)) {
      this.addLog(this.createLogEntry(LogLevel.ERROR, message, context, data, error));
    }
  }

  // API operation logging
  apiRequest(method: string, url: string, data?: any): void {
    this.debug(`API Request: ${method} ${url}`, 'API', data);
  }

  apiResponse(method: string, url: string, status: number, data?: any): void {
    this.debug(`API Response: ${method} ${url} - ${status}`, 'API', data);
  }

  apiError(method: string, url: string, error: any): void {
    this.error(`API Error: ${method} ${url}`, 'API', error, { 
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data 
    });
  }

  // User action logging
  userAction(action: string, context?: string, data?: any): void {
    this.info(`User Action: ${action}`, context || 'USER', data);
  }

  // Authentication logging
  authEvent(event: string, data?: any): void {
    this.info(`Auth Event: ${event}`, 'AUTH', data);
  }

  // Subscriber management logging
  subscriberOperation(operation: string, subscriberId?: string, data?: any): void {
    this.info(`Subscriber Operation: ${operation}${subscriberId ? ` (${subscriberId})` : ''}`, 'SUBSCRIBER', data);
  }

  // Session management logging
  sessionOperation(operation: string, sessionId?: string, data?: any): void {
    this.info(`Session Operation: ${operation}${sessionId ? ` (${sessionId})` : ''}`, 'SESSION', data);
  }

  // NFC operation logging
  nfcOperation(operation: string, cardId?: string, data?: any): void {
    this.info(`NFC Operation: ${operation}${cardId ? ` (${cardId})` : ''}`, 'NFC', data);
  }

  // Attendance logging
  attendanceOperation(operation: string, data?: any): void {
    this.info(`Attendance Operation: ${operation}`, 'ATTENDANCE', data);
  }

  // Get logs for debugging
  getLogs(level?: LogLevel): LogEntry[] {
    if (level !== undefined) {
      return this.logs.filter(log => log.level >= level);
    }
    return [...this.logs];
  }

  // Clear logs
  clearLogs(): void {
    this.logs = [];
    this.info('Logs cleared', 'LOGGING');
  }

  // Export logs as JSON
  exportLogs(): string {
    return JSON.stringify(this.logs, null, 2);
  }
}

// Create singleton instance
const logger = new LoggingService();

export default logger;
