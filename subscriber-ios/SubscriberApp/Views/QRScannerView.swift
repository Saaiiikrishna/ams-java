import SwiftUI
import AVFoundation

struct QRScannerView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var checkInViewModel: CheckInViewModel
    @State private var isShowingScanner = false
    @State private var lastScannedCode: String?
    @State private var lastScanTime: Date = Date()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Camera Preview
                ZStack {
                    CameraPreview(onQRCodeScanned: handleQRCodeScanned)
                        .frame(height: 400)
                        .cornerRadius(12)
                    
                    // Overlay with scanning frame
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.blue, lineWidth: 3)
                        .frame(width: 250, height: 250)
                    
                    // Loading overlay
                    if checkInViewModel.isLoading {
                        Color.black.opacity(0.3)
                            .cornerRadius(12)
                        
                        VStack {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .scaleEffect(1.5)
                            
                            Text("Processing check-in...")
                                .foregroundColor(.white)
                                .fontWeight(.medium)
                                .padding(.top)
                        }
                    }
                }
                
                // Instructions
                VStack(spacing: 12) {
                    HStack {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.title2)
                            .foregroundColor(.blue)
                        
                        Text("Point your camera at a QR code")
                            .font(.headline)
                            .fontWeight(.semibold)
                    }
                    
                    Text("Make sure the QR code is clearly visible and well-lit")
                        .font(.body)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(12)
                
                // Success/Error Messages
                if let successMessage = checkInViewModel.successMessage {
                    MessageCard(message: successMessage, isError: false)
                }
                
                if let errorMessage = checkInViewModel.errorMessage {
                    MessageCard(message: errorMessage, isError: true)
                }
                
                Spacer()
            }
            .padding()
            .navigationTitle("QR Scanner")
            .navigationBarTitleDisplayMode(.inline)
            .onAppear {
                checkInViewModel.clearMessages()
            }
        }
    }
    
    private func handleQRCodeScanned(_ code: String) {
        let now = Date()
        
        // Prevent duplicate scans within 2 seconds
        if code != lastScannedCode || now.timeIntervalSince(lastScanTime) > 2.0 {
            lastScannedCode = code
            lastScanTime = now
            
            // Provide haptic feedback
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.impactOccurred()
            
            checkInViewModel.handleQRCodeCheckIn(code)
        }
    }
}

struct CameraPreview: UIViewRepresentable {
    let onQRCodeScanned: (String) -> Void
    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: UIScreen.main.bounds)
        
        let captureSession = AVCaptureSession()
        
        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
            return view
        }
        
        let videoInput: AVCaptureDeviceInput
        
        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            return view
        }
        
        if captureSession.canAddInput(videoInput) {
            captureSession.addInput(videoInput)
        } else {
            return view
        }
        
        let metadataOutput = AVCaptureMetadataOutput()
        
        if captureSession.canAddOutput(metadataOutput) {
            captureSession.addOutput(metadataOutput)
            
            metadataOutput.setMetadataObjectsDelegate(context.coordinator, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        } else {
            return view
        }
        
        let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.frame = view.layer.bounds
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)
        
        DispatchQueue.global(qos: .background).async {
            captureSession.startRunning()
        }
        
        return view
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        let parent: CameraPreview
        
        init(_ parent: CameraPreview) {
            self.parent = parent
        }
        
        func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
            if let metadataObject = metadataObjects.first {
                guard let readableObject = metadataObject as? AVMetadataMachineReadableCodeObject else { return }
                guard let stringValue = readableObject.stringValue else { return }
                
                parent.onQRCodeScanned(stringValue)
            }
        }
    }
}

struct MessageCard: View {
    let message: String
    let isError: Bool
    
    var body: some View {
        HStack {
            Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                .foregroundColor(isError ? .red : .green)
            
            Text(message)
                .font(.body)
                .foregroundColor(isError ? .red : .green)
        }
        .padding()
        .background((isError ? Color.red : Color.green).opacity(0.1))
        .cornerRadius(8)
    }
}

struct QRScannerView_Previews: PreviewProvider {
    static var previews: some View {
        QRScannerView()
            .environmentObject(AuthViewModel())
            .environmentObject(CheckInViewModel())
    }
}
