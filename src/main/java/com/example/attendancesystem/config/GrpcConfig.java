package com.example.attendancesystem.config;

import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.security.authentication.BasicGrpcAuthenticationReader;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * Configuration class for gRPC server setup and security
 */
@Configuration
public class GrpcConfig {

    private static final Logger logger = LoggerFactory.getLogger(GrpcConfig.class);

    /**
     * Global gRPC server interceptor for logging and monitoring
     */
    @GrpcGlobalServerInterceptor
    public ServerInterceptor loggingInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                
                String methodName = call.getMethodDescriptor().getFullMethodName();
                logger.info("gRPC call started: {}", methodName);
                
                return new ForwardingServerCallListener<ReqT>() {
                    @Override
                    protected ServerCall.Listener<ReqT> delegate() {
                        return next.startCall(new ForwardingServerCall<ReqT, RespT>() {
                            @Override
                            protected ServerCall<ReqT, RespT> delegate() {
                                return call;
                            }

                            @Override
                            public void close(Status status, Metadata trailers) {
                                if (status.isOk()) {
                                    logger.info("gRPC call completed successfully: {}", methodName);
                                } else {
                                    logger.warn("gRPC call failed: {} - Status: {}", methodName, status);
                                }
                                super.close(status, trailers);
                            }
                        }, headers);
                    }
                };
            }
        };
    }

    /**
     * Error handling interceptor for gRPC calls
     */
    @GrpcGlobalServerInterceptor
    public ServerInterceptor errorHandlingInterceptor() {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                
                return new ForwardingServerCallListener<ReqT>() {
                    @Override
                    protected ServerCall.Listener<ReqT> delegate() {
                        return next.startCall(call, headers);
                    }

                    @Override
                    public void onHalfClose() {
                        try {
                            super.onHalfClose();
                        } catch (Exception e) {
                            logger.error("Unexpected error in gRPC call: {}", 
                                    call.getMethodDescriptor().getFullMethodName(), e);
                            call.close(Status.INTERNAL
                                    .withDescription("Internal server error: " + e.getMessage())
                                    .withCause(e), new Metadata());
                        }
                    }
                };
            }
        };
    }

    /**
     * Abstract base class for forwarding server call listeners
     */
    private abstract static class ForwardingServerCallListener<ReqT> extends ServerCall.Listener<ReqT> {
        protected abstract ServerCall.Listener<ReqT> delegate();

        @Override
        public void onMessage(ReqT message) {
            delegate().onMessage(message);
        }

        @Override
        public void onHalfClose() {
            delegate().onHalfClose();
        }

        @Override
        public void onCancel() {
            delegate().onCancel();
        }

        @Override
        public void onComplete() {
            delegate().onComplete();
        }

        @Override
        public void onReady() {
            delegate().onReady();
        }
    }

    /**
     * Abstract base class for forwarding server calls
     */
    private abstract static class ForwardingServerCall<ReqT, RespT> extends ServerCall<ReqT, RespT> {
        protected abstract ServerCall<ReqT, RespT> delegate();

        @Override
        public void request(int numMessages) {
            delegate().request(numMessages);
        }

        @Override
        public void sendHeaders(Metadata headers) {
            delegate().sendHeaders(headers);
        }

        @Override
        public void sendMessage(RespT message) {
            delegate().sendMessage(message);
        }

        @Override
        public void close(Status status, Metadata trailers) {
            delegate().close(status, trailers);
        }

        @Override
        public boolean isCancelled() {
            return delegate().isCancelled();
        }

        @Override
        public io.grpc.MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
            return delegate().getMethodDescriptor();
        }
    }
}
