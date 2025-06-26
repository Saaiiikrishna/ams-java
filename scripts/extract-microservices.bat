@echo off
REM Script to extract existing functionality into microservices
REM This script copies existing models, services, and controllers to appropriate microservices

echo Extracting existing functionality into microservices...

REM Create base directories if they don't exist
if not exist "microservices\auth-service\src\main\java\com\example\attendancesystem\auth" (
    mkdir microservices\auth-service\src\main\java\com\example\attendancesystem\auth\model
    mkdir microservices\auth-service\src\main\java\com\example\attendancesystem\auth\repository
    mkdir microservices\auth-service\src\main\java\com\example\attendancesystem\auth\service
    mkdir microservices\auth-service\src\main\java\com\example\attendancesystem\auth\controller
    mkdir microservices\auth-service\src\main\java\com\example\attendancesystem\auth\grpc
    mkdir microservices\auth-service\src\main\java\com\example\attendancesystem\auth\security
)

echo.
echo AUTH SERVICE - Copying authentication related files...

REM Copy Auth Models
copy "src\main\java\com\example\attendancesystem\model\EntityAdmin.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\SuperAdmin.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\Role.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\RefreshToken.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\SuperAdminRefreshToken.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\BlacklistedToken.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\model\" 2>nul

REM Copy Auth Repositories
copy "src\main\java\com\example\attendancesystem\repository\EntityAdminRepository.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\SuperAdminRepository.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\RoleRepository.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\RefreshTokenRepository.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\SuperAdminRefreshTokenRepository.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\BlacklistedTokenRepository.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\repository\" 2>nul

REM Copy Auth Services
copy "src\main\java\com\example\attendancesystem\service\RefreshTokenService.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\SuperAdminRefreshTokenService.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\service\" 2>nul

REM Copy Auth Controllers
copy "src\main\java\com\example\attendancesystem\controller\AuthenticationController.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\SuperAdminAuthController.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\controller\" 2>nul

REM Copy Auth gRPC Service
copy "src\main\java\com\example\attendancesystem\grpc\service\AuthServiceImpl.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\grpc\" 2>nul

REM Copy Security Classes
copy "src\main\java\com\example\attendancesystem\security\*.java" "microservices\auth-service\src\main\java\com\example\attendancesystem\auth\security\" 2>nul

echo Auth Service files copied.

echo.
echo ORGANIZATION SERVICE - Copying organization related files...

REM Create organization service directories
if not exist "microservices\organization-service\src\main\java\com\example\attendancesystem\organization" (
    mkdir microservices\organization-service\src\main\java\com\example\attendancesystem\organization\model
    mkdir microservices\organization-service\src\main\java\com\example\attendancesystem\organization\repository
    mkdir microservices\organization-service\src\main\java\com\example\attendancesystem\organization\service
    mkdir microservices\organization-service\src\main\java\com\example\attendancesystem\organization\controller
    mkdir microservices\organization-service\src\main\java\com\example\attendancesystem\organization\grpc
)

REM Copy Organization Models
copy "src\main\java\com\example\attendancesystem\model\Organization.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\OrganizationPermission.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\FeaturePermission.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\model\" 2>nul

REM Copy Organization Repositories
copy "src\main\java\com\example\attendancesystem\repository\OrganizationRepository.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\OrganizationPermissionRepository.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\repository\" 2>nul

REM Copy Organization Services
copy "src\main\java\com\example\attendancesystem\service\PermissionService.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\EntityIdService.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\service\" 2>nul

REM Copy Organization Controllers
copy "src\main\java\com\example\attendancesystem\controller\AdminController.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\SuperAdminController.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\PermissionController.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\EntityPermissionController.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\controller\" 2>nul

REM Copy Organization gRPC Service
copy "src\main\java\com\example\attendancesystem\grpc\service\SimpleOrganizationServiceImpl.java" "microservices\organization-service\src\main\java\com\example\attendancesystem\organization\grpc\" 2>nul

echo Organization Service files copied.

echo.
echo SUBSCRIBER SERVICE - Copying subscriber related files...

REM Create subscriber service directories
if not exist "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber" (
    mkdir microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\model
    mkdir microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\repository
    mkdir microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\service
    mkdir microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\controller
    mkdir microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\grpc
)

REM Copy Subscriber Models
copy "src\main\java\com\example\attendancesystem\model\Subscriber.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\SubscriberAuth.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\model\" 2>nul
copy "src\main\java\com\example\attendancesystem\model\NfcCard.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\model\" 2>nul

REM Copy Subscriber Repositories
copy "src\main\java\com\example\attendancesystem\repository\SubscriberRepository.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\SubscriberAuthRepository.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\repository\" 2>nul
copy "src\main\java\com\example\attendancesystem\repository\NfcCardRepository.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\repository\" 2>nul

REM Copy Subscriber Services
copy "src\main\java\com\example\attendancesystem\service\SubscriberAuthService.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\service\" 2>nul
copy "src\main\java\com\example\attendancesystem\service\NfcCardService.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\service\" 2>nul

REM Copy Subscriber Controllers
copy "src\main\java\com\example\attendancesystem\controller\SubscriberController.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\EntityController.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\NfcController.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\controller\" 2>nul
copy "src\main\java\com\example\attendancesystem\controller\CardManagementController.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\controller\" 2>nul

REM Copy Subscriber gRPC Service
copy "src\main\java\com\example\attendancesystem\grpc\service\SubscriberServiceImpl.java" "microservices\subscriber-service\src\main\java\com\example\attendancesystem\subscriber\grpc\" 2>nul

echo Subscriber Service files copied.

echo.
echo Microservices extraction completed!
echo.
echo Next steps:
echo 1. Update package names in copied files
echo 2. Update imports in copied files
echo 3. Create application.yml for each service
echo 4. Create Dockerfiles for each service
echo 5. Update Docker Compose configuration
