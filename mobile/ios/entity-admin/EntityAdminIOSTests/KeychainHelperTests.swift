import XCTest
@testable import EntityAdminIOS

final class KeychainHelperTests: XCTestCase {
    func testSaveAndRead() {
        let helper = KeychainHelper.standard
        helper.save("token", service: "test", account: "a")
        XCTAssertEqual(helper.read(service: "test", account: "a"), "token")
    }
}
