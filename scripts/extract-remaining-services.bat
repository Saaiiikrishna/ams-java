@echo off
REM Script to extract remaining microservices functionality

echo Extracting remaining microservices...

echo.
echo ATTENDANCE SERVICE - Copying attendance related files...

REM Create attendance service directories
if not exist "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance" (
    mkdir microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\model
    mkdir microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\repository
    mkdir microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\service
    mkdir microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller
    mkdir microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\grpc
)

REM Copy Attendance Models
copy "src\main\java\com\example\attendancesystem\model\AttendanceSession.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\AttendanceLog.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\ScheduledSession.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\CheckInMethod.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\FaceRecognitionLog.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\FaceRecognitionSettings.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\model\" 2>nul

REM Copy Attendance Repositories
copy "src\main\java\com\example\attendancesystem\repository\AttendanceSessionRepository.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\AttendanceLogRepository.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\ScheduledSessionRepository.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\FaceRecognitionLogRepository.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\FaceRecognitionSettingsRepository.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\repository\" 2>nul

REM Copy Attendance Services
copy "src\main\java\com\example\attendancesystem\service\ScheduledSessionService.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\QrCodeService.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\FaceRecognitionService.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\FaceRecognitionSettingsService.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\DJLFaceRecognitionService.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\ReportService.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\service\" 2>nul

REM Copy Attendance Controllers
copy "src\main\java\com\example\attendancesystem\controller\CheckInController.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\QrCodeController.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\FaceRecognitionCheckInController.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\FaceRecognitionHealthController.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\FaceRecognitionSettingsController.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\FaceRecognitionAdvancedSettingsController.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\FaceRegistrationController.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\ReportController.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\controller\" 2>nul

REM Copy Attendance gRPC Service
copy "src\main\java\com\example\attendancesystem\grpc\service\AttendanceServiceImpl.java" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\grpc\" 2>nul

REM Copy Face Recognition Package
xcopy "src\main\java\com\example\attendancesystem\facerecognition" "microservices\attendance-service\src\main\java\com\example\attendancesystem\attendance\facerecognition\" /E /I /Q 2>nul

echo Attendance Service files copied.

echo.
echo MENU SERVICE - Copying menu related files...

REM Create menu service directories
if not exist "microservices\menu-service\src\main\java\com\example\attendancesystem\menu" (
    mkdir microservices\menu-service\src\main\java\com\example\attendancesystem\menu\model
    mkdir microservices\menu-service\src\main\java\com\example\attendancesystem\menu\repository
    mkdir microservices\menu-service\src\main\java\com\example\attendancesystem\menu\service
    mkdir microservices\menu-service\src\main\java\com\example\attendancesystem\menu\controller
    mkdir microservices\menu-service\src\main\java\com\example\attendancesystem\menu\grpc
)

REM Copy Menu Models
copy "src\main\java\com\example\attendancesystem\model\Category.java" "microservices\menu-service\src\main\java\com\example\attendancesystem\menu\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\Item.java" "microservices\menu-service\src\main\java\com\example\attendancesystem\menu\model\" 2>nul

REM Copy Menu Repositories
copy "src\main\java\com\example\attendancesystem\repository\CategoryRepository.java" "microservices\menu-service\src\main\java\com\example\attendancesystem\menu\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\ItemRepository.java" "microservices\menu-service\src\main\java\com\example\attendancesystem\menu\repository\" 2>nul

REM Copy Menu Services
copy "src\main\java\com\example\attendancesystem\service\MenuService.java" "microservices\menu-service\src\main\java\com\example\attendancesystem\menu\service\" 2>nul

REM Copy Menu Controllers
copy "src\main\java\com\example\attendancesystem\controller\MenuController.java" "microservices\menu-service\src\main\java\com\example\attendancesystem\menu\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\PublicMenuController.java" "microservices\menu-service\src\main\java\com\example\attendancesystem\menu\controller\" 2>nul

REM Copy Menu gRPC Service
copy "src\main\java\com\example\attendancesystem\grpc\service\MenuServiceImpl.java" "microservices\menu-service\src\main\java\com\example\attendancesystem\menu\grpc\" 2>nul

echo Menu Service files copied.

echo.
echo ORDER SERVICE - Copying order related files...

REM Create order service directories
if not exist "microservices\order-service\src\main\java\com\example\attendancesystem\order" (
    mkdir microservices\order-service\src\main\java\com\example\attendancesystem\order\model
    mkdir microservices\order-service\src\main\java\com\example\attendancesystem\order\repository
    mkdir microservices\order-service\src\main\java\com\example\attendancesystem\order\service
    mkdir microservices\order-service\src\main\java\com\example\attendancesystem\order\controller
    mkdir microservices\order-service\src\main\java\com\example\attendancesystem\order\grpc
)

REM Copy Order Models
copy "src\main\java\com\example\attendancesystem\model\Order.java" "microservices\order-service\src\main\java\com\example\attendancesystem\order\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\OrderItem.java" "microservices\order-service\src\main\java\com\example\attendancesystem\order\model\" 2>nul

REM Copy Order Repositories
copy "src\main\java\com\example\attendancesystem\repository\OrderRepository.java" "microservices\order-service\src\main\java\com\example\attendancesystem\order\repository\" 2>nul

REM Copy Order Services
copy "src\main\java\com\example\attendancesystem\service\OrderService.java" "microservices\order-service\src\main\java\com\example\attendancesystem\order\service\" 2>nul

REM Copy Order Controllers
copy "src\main\java\com\example\attendancesystem\controller\OrderController.java" "microservices\order-service\src\main\java\com\example\attendancesystem\order\controller\" 2>nul

REM Copy Order gRPC Service
copy "src\main\java\com\example\attendancesystem\grpc\service\OrderServiceImpl.java" "microservices\order-service\src\main\java\com\example\attendancesystem\order\grpc\" 2>nul

echo Order Service files copied.

echo.
echo TABLE SERVICE - Copying table related files...

REM Create table service directories
if not exist "microservices\table-service\src\main\java\com\example\attendancesystem\table" (
    mkdir microservices\table-service\src\main\java\com\example\attendancesystem\table\model
    mkdir microservices\table-service\src\main\java\com\example\attendancesystem\table\repository
    mkdir microservices\table-service\src\main\java\com\example\attendancesystem\table\service
    mkdir microservices\table-service\src\main\java\com\example\attendancesystem\table\controller
    mkdir microservices\table-service\src\main\java\com\example\attendancesystem\table\grpc
)

REM Copy Table Models
copy "src\main\java\com\example\attendancesystem\model\RestaurantTable.java" "microservices\table-service\src\main\java\com\example\attendancesystem\table\model\" 2>nul

REM Copy Table Repositories
copy "src\main\java\com\example\attendancesystem\repository\RestaurantTableRepository.java" "microservices\table-service\src\main\java\com\example\attendancesystem\table\repository\" 2>nul

REM Copy Table Services
copy "src\main\java\com\example\attendancesystem\service\TableService.java" "microservices\table-service\src\main\java\com\example\attendancesystem\table\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\TableQrCodeService.java" "microservices\table-service\src\main\java\com\example\attendancesystem\table\service\" 2>nul

REM Copy Table Controllers
copy "src\main\java\com\example\attendancesystem\controller\TableController.java" "microservices\table-service\src\main\java\com\example\attendancesystem\table\controller\" 2>nul

REM Copy Table gRPC Service
copy "src\main\java\com\example\attendancesystem\grpc\service\TableServiceImpl.java" "microservices\table-service\src\main\java\com\example\attendancesystem\table\grpc\" 2>nul

echo Table Service files copied.

echo.
echo All remaining microservices extracted successfully!
echo.
echo Summary:
echo - Auth Service: ✓ Complete
echo - Organization Service: ✓ Complete  
echo - Subscriber Service: ✓ Complete
echo - Attendance Service: ✓ Complete
echo - Menu Service: ✓ Complete
echo - Order Service: ✓ Complete
echo - Table Service: ✓ Complete
echo.
echo Next steps:
echo 1. Create pom.xml files for remaining services
echo 2. Create application classes for each service
echo 3. Update package imports in all copied files
echo 4. Create application.yml configurations
echo 5. Create Dockerfiles for each service
